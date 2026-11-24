"""Segunda parte del cambio de registros de hardware.

Hace que la ventana de hardware (0xCC000000..0xCDFFFFFF) sea memoria real y legible/escribible,
y que el runtime lea y escriba en esa misma memoria. Así el código traducido (acceso directo)
y el runtime (acceso por rutas lentas) siempre ven el mismo valor.

Uso (desde D:\\rhf-recomp):
    python patches\\apply_mmio_window.py runtime
Requiere haber aplicado antes apply_mmio_registers.py. Se puede ejecutar más de una vez.
"""
import sys
from pathlib import Path

RUNTIME = Path(sys.argv[1] if len(sys.argv) > 1 else "runtime")
HDR = RUNTIME / "include" / "guest_flat_memory.h"
CPP = RUNTIME / "src" / "guest_flat_memory.cpp"
MEM = RUNTIME / "src" / "memory.cpp"

def fail(msg):
    sys.exit("[error] " + msg + " No se cambió nada.")

def once(text, old, label):
    n = text.count(old)
    if n != 1:
        fail(f"'{label}': se esperaba 1 coincidencia y hay {n}.")
    return text

# ---------- 1) encabezado ----------
hdr = HDR.read_text(encoding="utf-8")
DECL_ANCHOR = "uint8_t* HostPointer(uint32_t guestAddress);\n"
DECL = ("\n// Host pointer al inicio de la ventana de hardware (0xCC000000, 32 MiB), memoria real\n"
        "// legible y escribible. Lo usan las rutas de registros del runtime.\n"
        "uint8_t* MmioWindowHostPointer();\n")
if "MmioWindowHostPointer" not in hdr:
    once(hdr, DECL_ANCHOR, "declaración HostPointer")
    hdr = hdr.replace(DECL_ANCHOR, DECL_ANCHOR + DECL)
    HDR.write_text(hdr, encoding="utf-8")

# ---------- 2) implementación ----------
cpp = CPP.read_text(encoding="utf-8")
OLD_COMMIT = "CommitPlaceholder(g_base + 0xCC000000u, 0x02000000u, kProtNone)"
NEW_COMMIT = "CommitPlaceholder(g_base + 0xCC000000u, 0x02000000u, kProtReadWrite)"
if OLD_COMMIT in cpp:
    once(cpp, OLD_COMMIT, "commit de la ventana MMIO")
    cpp = cpp.replace(OLD_COMMIT, NEW_COMMIT)
if "uint8_t* MmioWindowHostPointer() {" not in cpp:
    IMPL_ANCHOR = "uint8_t* HostPointer(uint32_t guestAddress) {\n"
    once(cpp, IMPL_ANCHOR, "implementación HostPointer")
    IMPL = ("uint8_t* MmioWindowHostPointer() {\n"
            "    return g_base == nullptr ? nullptr : g_base + 0xCC000000u;\n"
            "}\n\n")
    cpp = cpp.replace(IMPL_ANCHOR, IMPL + IMPL_ANCHOR)
CPP.write_text(cpp, encoding="utf-8")

# ---------- 3) runtime: usar la misma memoria ----------
mem = MEM.read_text(encoding="utf-8")
start_marker = "// Registros de hardware (0xCC000000..0xCDFFFFFF"
end_marker = "uint8_t MemoryInline::Read8Slow(uint32_t addr) {\n"
if start_marker not in mem or end_marker not in mem:
    fail("no encontré el bloque de registros de hardware: aplica primero apply_mmio_registers.py.")
s = mem.index(start_marker)
e = mem.index(end_marker)
NEW_HELPER = """// Registros de hardware (0xCC000000..0xCDFFFFFF, sin contar la cola GX): son la misma memoria
// que ve el codigo traducido, en orden big-endian como el bus real. Los dispositivos que
// necesiten comportamiento real tendran su propia emulacion (HLE) despues.
namespace {
constexpr uint32_t kMmioBase = 0xCC000000u;
constexpr size_t kMmioSize = 0x02000000u;
uint64_t MmioRegisterRead(uint32_t addr, size_t length) {
    const uint8_t* bytes = GuestFlat::MmioWindowHostPointer();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (bytes == nullptr || off + length > kMmioSize) return 0;
    uint64_t value = 0;
    for (size_t i = 0; i < length; ++i) value = (value << 8) | bytes[off + i];
    return value;
}
void MmioRegisterWrite(uint32_t addr, uint64_t value, size_t length) {
    uint8_t* bytes = GuestFlat::MmioWindowHostPointer();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (bytes == nullptr || off + length > kMmioSize) return;
    for (size_t i = 0; i < length; ++i) {
        bytes[off + i] = static_cast<uint8_t>(value >> (8 * (length - 1 - i)));
    }
}
}  // namespace

"""
mem = mem[:s] + NEW_HELPER + mem[e:]
MEM.write_text(mem, encoding="utf-8")
print("Listo: ventana MMIO compartida entre el codigo traducido y el runtime.")
print("Ahora recompila: cmake --build native-build --target WiiCompiled")
