"""Muestra datos básicos de boot.bin y main.dol.
Uso: python tools/rhf_info.py inputs/
"""
import hashlib, struct, sys
from pathlib import Path

folder = Path(sys.argv[1] if len(sys.argv) > 1 else "inputs")
boot = folder / "boot.bin"
dol = folder / "main.dol"
for f in (boot, dol):
    if not f.exists():
        sys.exit(f"Falta {f}")

b = boot.read_bytes()
print("ID del juego:", b[:6].decode("ascii", errors="replace"))
print("Título:", b[0x20:0x60].split(b"\0")[0].decode("ascii", errors="replace"))

d = dol.read_bytes()
print("SHA-256 main.dol:", hashlib.sha256(d).hexdigest())
print("Entrada: 0x%08X" % struct.unpack(">I", d[0xE0:0xE4])[0])
for i in range(18):
    off = struct.unpack(">I", d[i*4:i*4+4])[0]
    addr = struct.unpack(">I", d[0x48+i*4:0x4C+i*4])[0]
    size = struct.unpack(">I", d[0x90+i*4:0x94+i*4])[0]
    if off and size:
        print("  sección %2d: addr=0x%08X size=0x%X" % (i, addr, size))
