"""Hace que los registros de hardware (0xCC000000..0xCDFFFFFF, excepto la cola GX) se
comporten como memoria normal: lo que se escribe se guarda y se devuelve al leer.

Uso (desde la carpeta rhf-recomp):
    python patches\\apply_mmio_registers.py
Lo ejecutas una sola vez. Si lo corres dos veces, no hace nada.
"""
import sys
from pathlib import Path

RUNTIME = Path(sys.argv[1] if len(sys.argv) > 1 else "runtime")
MEM = RUNTIME / "src" / "memory.cpp"

HELPER = """// Registros de hardware (0xCC000000..0xCDFFFFFF, sin contar la cola GX): se guardan como
// memoria normal, en orden big-endian como el bus real. Los dispositivos que necesiten
// comportamiento real tendrán su propia emulacion (HLE) despues.
namespace {
constexpr uint32_t kMmioBase = 0xCC000000u;
std::vector<uint8_t>& MmioRegisterBytes() {
    static std::vector<uint8_t> bytes(0x02000000u, 0);
    return bytes;
}
uint64_t MmioRegisterRead(uint32_t addr, size_t length) {
    const auto& bytes = MmioRegisterBytes();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (off + length > bytes.size()) return 0;
    uint64_t value = 0;
    for (size_t i = 0; i < length; ++i) value = (value << 8) | bytes[off + i];
    return value;
}
void MmioRegisterWrite(uint32_t addr, uint64_t value, size_t length) {
    auto& bytes = MmioRegisterBytes();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (off + length > bytes.size()) return;
    for (size_t i = 0; i < length; ++i) {
        bytes[off + i] = static_cast<uint8_t>(value >> (8 * (length - 1 - i)));
    }
}
}  // namespace

"""

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        sys.exit(f"[error] '{label}': se esperaba 1 coincidencia y hay {count}. No se cambio nada.")
    return text.replace(old, new)

def replace_all_exact(text, old, new, label, expected):
    count = text.count(old)
    if count != expected:
        sys.exit(f"[error] '{label}': se esperaban {expected} coincidencias y hay {count}. No se cambio nada.")
    return text.replace(old, new)

src = MEM.read_text(encoding="utf-8")
if "MmioRegisterRead" in src:
    print("Ya estaba aplicado. No hay nada que hacer.")
    sys.exit(0)

# 1) include <vector>
src = replace_once(src, "#include <cstring>\n", "#include <cstring>\n#include <vector>\n", "include vector")

# 2) helper antes de Read8Slow
src = replace_once(src, "uint8_t MemoryInline::Read8Slow(uint32_t addr) {\n",
                   HELPER + "uint8_t MemoryInline::Read8Slow(uint32_t addr) {\n", "anchor Read8Slow")

# 3) lecturas: devolver el valor del registro en vez de fallar
reads = {
    "sizeof(uint8_t)": ("        return static_cast<uint8_t>(MmioRegisterRead(addr, sizeof(uint8_t)));\n", "Read8"),
    "sizeof(uint16_t)": ("        return static_cast<uint16_t>(MmioRegisterRead(addr, sizeof(uint16_t)));\n", "Read16"),
    "sizeof(uint32_t)": ("        return static_cast<uint32_t>(MmioRegisterRead(addr, sizeof(uint32_t)));\n", "Read32"),
    "sizeof(uint64_t)": ("        return MmioRegisterRead(addr, sizeof(uint64_t));\n", "Read64"),
    "sizeof(float)": ("        const auto bits = static_cast<uint32_t>(MmioRegisterRead(addr, sizeof(float)));\n"
                      "        float value;\n        std::memcpy(&value, &bits, sizeof(value));\n        return value;\n", "ReadFloat32"),
    "sizeof(double)": ("        const auto bits = MmioRegisterRead(addr, sizeof(double));\n"
                       "        double value;\n        std::memcpy(&value, &bits, sizeof(value));\n        return value;\n", "ReadFloat64"),
}
for size_expr, (body, label) in reads.items():
    old = f"    if (IsMmioAddress(addr)) {{\n        ThrowMmioReadBlocked(addr, {size_expr});\n    }}\n"
    new = f"    if (IsMmioAddress(addr)) {{\n{body}    }}\n"
    src = replace_once(src, old, new, "lectura " + label)

# 4) escrituras de enteros (4 funciones con el mismo texto)
old_int = 'throw Memory::AccessViolation(addr, sizeof(val), "MMIO write blocked (non-GPU)");'
src = replace_all_exact(src, old_int, "MmioRegisterWrite(addr, val, sizeof(val)); return;", "escrituras enteras", 4)

# 5) escrituras de coma flotante
src = replace_once(src, 'throw Memory::AccessViolation(addr, sizeof(float), "MMIO write blocked (non-GPU)");',
                   "MmioRegisterWrite(addr, bits, sizeof(float)); return;", "escritura float32")
src = replace_once(src, 'throw Memory::AccessViolation(addr, sizeof(double), "MMIO write blocked (non-GPU)");',
                   "MmioRegisterWrite(addr, bits, sizeof(double)); return;", "escritura float64")

MEM.write_text(src, encoding="utf-8")
print("Listo: memory.cpp actualizado. Ahora recompila con cmake --build native-build --target WiiCompiled")
