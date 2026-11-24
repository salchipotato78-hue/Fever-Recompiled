#!/usr/bin/env python3
"""Busca el microcódigo de DSP (AX) dentro de un main.dol y escribe su fila para dsp_ucode.csv.

El motor identifica el microcódigo por el checksum de sus bytes (recompiler/dsp/recomp_dsp.py).
Este script recorre todos los offsets del DOL y calcula ese mismo checksum, de forma vectorizada,
para encontrar la ventana cuyo checksum coincide. No necesita símbolos ni volcados del juego.

Uso:
    python3 tools/find_dsp_ucode.py data/main.dol games/rhf/analysis/dsp_ucode.csv
"""
import argparse
import csv
import sys
from pathlib import Path

import numpy as np

# Mismos valores que games/wiiparty/analysis/dsp_ucode.csv (AX, 8 KB, checksum bc128f9d).
UCODE_NAME = "ax"
UCODE_SIZE = 0x2000
UCODE_CHECKSUM = 0xBC128F9D


def checksum_of_windows(dol: np.ndarray, size: int) -> np.ndarray:
    """checksum() de recomp_dsp.py aplicado a dol[i:i+size] para todo i a la vez."""
    count = len(dol) - size + 1
    value = np.zeros(count, dtype=np.uint32)
    for k in range(size):
        value = (value * np.uint32(31)) ^ dol[k:k + count].astype(np.uint32)
    return value


def find_offsets(dol_bytes: bytes, size: int, expected: int) -> list[int]:
    if len(dol_bytes) < size:
        return []
    dol = np.frombuffer(dol_bytes, dtype=np.uint8)
    hits = np.nonzero(checksum_of_windows(dol, size) == np.uint32(expected))[0]
    return [int(h) for h in hits]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dol", type=Path)
    parser.add_argument("csv", type=Path, help="dsp_ucode.csv a crear o actualizar")
    args = parser.parse_args()

    offsets = find_offsets(args.dol.read_bytes(), UCODE_SIZE, UCODE_CHECKSUM)
    if not offsets:
        print("ERROR: no se encontró el microcódigo AX (checksum bc128f9d) en la DOL.", file=sys.stderr)
        print("Revisa que sea la DOL correcta (SOME01 NTSC-U rev. 0).", file=sys.stderr)
        return 1
    if len(offsets) > 1:
        print(f"ERROR: se encontraron {len(offsets)} coincidencias: {[hex(o) for o in offsets]}", file=sys.stderr)
        return 1

    row = {"name": UCODE_NAME, "dol_offset": f"{offsets[0]:x}", "size": f"{UCODE_SIZE:x}",
           "checksum": f"{UCODE_CHECKSUM:08x}"}
    args.csv.parent.mkdir(parents=True, exist_ok=True)
    with args.csv.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["name", "dol_offset", "size", "checksum"])
        writer.writeheader()
        writer.writerow(row)
    print(f"{UCODE_NAME}: offset {row['dol_offset']} en {args.dol.name} -> {args.csv}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
