"""Tercera parte: registra en el log las primeras 500 lecturas y escrituras de hardware
(0xCC000000..0xCDFFFFFF), con dirección, tamaño y valor. Sirve para saber qué registro
espera el juego cuando se queda colgado.

Uso (desde D:\\rhf-recomp):
    python patches\\apply_mmio_trace.py runtime
Requiere haber aplicado antes apply_mmio_registers.py y apply_mmio_window.py.
Se puede ejecutar más de una vez.
"""
import sys
from pathlib import Path

RUNTIME = Path(sys.argv[1] if len(sys.argv) > 1 else "runtime")
MEM = RUNTIME / "src" / "memory.cpp"
src = MEM.read_text(encoding="utf-8")

start_marker = "// Registros de hardware (0xCC000000..0xCDFFFFFF"
end_marker = "uint8_t MemoryInline::Read8Slow(uint32_t addr) {\n"
if start_marker not in src or end_marker not in src:
    sys.exit("[error] no encontré el bloque de registros. Aplica primero los scripts anteriores. No se cambió nada.")

HELPER = """// Registros de hardware (0xCC000000..0xCDFFFFFF, sin contar la cola GX): son la misma memoria
// que ve el codigo traducido, en orden big-endian como el bus real. Se registran los primeros
// accesos en el log para diagnosticar bloqueos.
namespace {
constexpr uint32_t kMmioBase = 0xCC000000u;
constexpr size_t kMmioSize = 0x02000000u;
constexpr int kMmioTraceLimit = 500;
int& MmioTraceCount() {
    static int count = 0;
    return count;
}
uint64_t MmioRegisterRead(uint32_t addr, size_t length) {
    const uint8_t* bytes = GuestFlat::MmioWindowHostPointer();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (bytes == nullptr || off + length > kMmioSize) return 0;
    uint64_t value = 0;
    for (size_t i = 0; i < length; ++i) value = (value << 8) | bytes[off + i];
    if (MmioTraceCount() < kMmioTraceLimit) {
        ++MmioTraceCount();
        RT_LOG(RT_TAG_MEMORY) << "[mmio trace] READ  addr=0x" << std::hex << addr
                              << " size=" << std::dec << length
                              << " value=0x" << std::hex << value << std::dec << std::endl;
    }
    return value;
}
void MmioRegisterWrite(uint32_t addr, uint64_t value, size_t length) {
    uint8_t* bytes = GuestFlat::MmioWindowHostPointer();
    const size_t off = static_cast<size_t>(addr - kMmioBase);
    if (bytes == nullptr || off + length > kMmioSize) return;
    for (size_t i = 0; i < length; ++i) {
        bytes[off + i] = static_cast<uint8_t>(value >> (8 * (length - 1 - i)));
    }
    if (MmioTraceCount() < kMmioTraceLimit) {
        ++MmioTraceCount();
        RT_LOG(RT_TAG_MEMORY) << "[mmio trace] WRITE addr=0x" << std::hex << addr
                              << " size=" << std::dec << length
                              << " value=0x" << std::hex << value << std::dec << std::endl;
    }
}
}  // namespace

"""
s = src.index(start_marker)
e = src.index(end_marker)
src = src[:s] + HELPER + src[e:]
MEM.write_text(src, encoding="utf-8")
print("Listo: trazas de hardware activadas (primeras 500 lecturas/escrituras).")
print("Recompila: cmake --build native-build --target WiiCompiled")
