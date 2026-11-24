"""Convierte el archivo de símbolos al formato que lee el traductor: "DIRECCION nombre" por línea.
Solo toma las funciones (type:function).
Uso: python tools/symbols_to_map.py symbols.txt projects/rhf/MAP.txt
"""
import re, sys
from pathlib import Path

src, dst = Path(sys.argv[1]), Path(sys.argv[2])
pat = re.compile(r'^(\S+) = \.\w+:0x([0-9A-Fa-f]{8}); // type:function')
rows = []
for line in src.read_text(encoding="utf-8", errors="replace").splitlines():
    m = pat.match(line)
    if m:
        rows.append((int(m.group(2), 16), m.group(1)))
rows.sort()
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text("".join("%08X %s\n" % (a, n) for a, n in rows), encoding="ascii", errors="replace")
print("Funciones escritas:", len(rows), "->", dst)
