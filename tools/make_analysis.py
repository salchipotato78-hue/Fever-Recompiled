"""Genera los archivos de análisis de Rhythm Heaven Fever para el motor de Wii Party Recomp.

Entrada: el archivo de símbolos del juego (líneas tipo  nombre = .text:0xDIR; // type:function size:0xN).
Salida:  games/rhf/analysis/symbols.csv, dol_functions.csv y hle_functions.csv

No incluye datos del juego: tú debes tener tu propio archivo de símbolos.

Uso (desde la raíz del clon de wiiparty-recomp, con este repo copiado en games/rhf):
    python games\\rhf\\tools\\make_analysis.py ruta\\al\\symbols.txt games\\rhf\\analysis
"""
import csv
import re
import sys
from pathlib import Path

# Funciones que el motor sustituye por código propio que devuelve cero (no tienen sentido en PC).
HLE_ZERO = ["RealMode", "ConfigMEM1_24MB", "ConfigMEM2_52MB", "ConfigMEM2_56MB",
            "ConfigMEM2_64MB", "ConfigMEM2_112MB", "ConfigMEM2_128MB"]
# Renombres: el motor llama a estas funciones por su nombre estándar del SDK.
RENAME = {"SelectThread": "OSSelectThread"}
# Funciones que el motor reemplaza con implementación propia (ya existen en el motor).
HLE_NATIVE = ["OSLoadContext", "OSSwitchFiber", "OSPanic"]

PAT = re.compile(r"^(\S+) = \.\w+:0x([0-9A-Fa-f]{8}); // type:(\w+)(?: size:0x([0-9A-Fa-f]+))?")

def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    symbols_path, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    out_dir.mkdir(parents=True, exist_ok=True)

    symrows, funcs = {}, []
    for line in symbols_path.read_text(encoding="utf-8", errors="replace").splitlines():
        m = PAT.match(line)
        if not m:
            continue
        name = m.group(1).replace("\\", "_")          # nombres con \ rompen el código generado
        name = RENAME.get(name, name)
        if name in HLE_ZERO:
            name = "HleZero_" + name
        addr, typ, size = int(m.group(2), 16), m.group(3), m.group(4)
        symrows.setdefault(addr, name)
        if typ == "function" and size:
            funcs.append((addr, int(size, 16)))

    with open(out_dir / "symbols.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["address", "name"])
        for a in sorted(symrows):
            w.writerow([f"0x{a:08X}", symrows[a]])

    with open(out_dir / "dol_functions.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["address", "size"])
        for a, s in sorted(set(funcs)):
            w.writerow([f"0x{a:08X}", s])

    replacements = HLE_NATIVE + ["HleZero_" + n for n in HLE_ZERO]
    with open(out_dir / "hle_functions.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["name"])
        for n in replacements:
            w.writerow([n])

    print(f"symbols.csv: {len(symrows)} | funciones: {len(funcs)} | sustituciones: {len(replacements)}")

if __name__ == "__main__":
    main()
