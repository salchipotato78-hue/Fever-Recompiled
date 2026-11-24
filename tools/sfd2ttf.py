#!/usr/bin/env python3
"""Convierte un proyecto FontForge (.sfd en texto) a TTF con fontTools.

Usa la capa Fore de cada glifo. Las curvas cúbicas se convierten a cuadráticas (TrueType) y los
glifos compuestos (Refer:) se escriben como componentes. Uso: python3 sfd2ttf.py entrada.sfd salida.ttf
"""
import sys
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.cu2quPen import Cu2QuPen


def _is_number(token):
    try:
        float(token)
        return True
    except ValueError:
        return False


def parse_point_line(line):
    """'x1 y1 x2 y2 x3 y3 c 258' -> ('c', [(x1,y1),(x2,y2),(x3,y3)])."""
    tokens = line.split()
    type_at = next((i for i, t in enumerate(tokens) if not _is_number(t)), None)
    if type_at is None:
        return None
    kind = tokens[type_at]
    nums = [float(t) for t in tokens[:type_at]]
    pts = [(nums[k], nums[k + 1]) for k in range(0, len(nums) - 1, 2)]
    return kind, pts


def parse(path):
    header, glyphs, font_section = {}, [], True
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    cur, in_fore_spline, layer = None, False, None
    for line in lines:
        if line.startswith("StartChar:"):
            font_section = False
            cur = {"name": line.split(":", 1)[1].strip(), "uni": -1, "width": 0,
                   "contours": [], "refs": []}
            glyphs.append(cur)
            in_fore_spline, layer = False, None
            continue
        if font_section:
            if ":" in line:
                k, v = line.split(":", 1)
                header.setdefault(k.strip(), v.strip())
            continue
        if cur is None:
            continue
        if line.startswith("EndChar"):
            cur = None
            continue
        if line in ("Fore", "Back") or line.startswith("Layer:"):
            layer = line
            continue
        if line.startswith("Encoding:") and cur["uni"] == -1:
            cur["uni"] = int(line.split()[1])
        elif line.startswith("Width:"):
            cur["width"] = int(line.split()[1])
        elif line.startswith("SplineSet"):
            in_fore_spline = layer in (None, "Fore")
            if in_fore_spline:
                cur["contours"].append([])
        elif line.startswith("EndSplineSet"):
            in_fore_spline = False
        elif line.startswith("Refer:"):
            p = line.split()
            if layer in (None, "Fore"):
                cur["refs"].append((int(p[1]), [float(x) for x in p[4:10]]))
        elif in_fore_spline and cur["contours"]:
            parsed = parse_point_line(line)
            if parsed:
                kind, pts = parsed
                if kind == "m" and pts:
                    # Cada "m" abre un contorno nuevo dentro del mismo SplineSet.
                    cur["contours"].append([("m", pts[0])])
                elif kind == "l" and pts:
                    cur["contours"][-1].append(("l", pts[0]))
                elif kind == "c" and len(pts) == 3:
                    cur["contours"][-1].append(("c", pts))
    return header, glyphs


def draw_contours(pen, contours):
    for contour in contours:
        if not contour or contour[0][0] != "m":
            continue
        pen.moveTo(contour[0][1])
        for kind, data in contour[1:]:
            if kind == "l":
                pen.lineTo(data)
            else:
                pen.curveTo(*data)
        pen.closePath()


def build(sfd, out):
    header, glyphs = parse(sfd)
    upm = int(header.get("Ascent", 880)) + int(header.get("Descent", 120))
    names = [g["name"] for g in glyphs]
    if names[0] != ".notdef":
        names.insert(0, ".notdef")
        glyphs.insert(0, {"name": ".notdef", "uni": -1, "width": upm // 2, "contours": [], "refs": []})
    # Índice SFD: el orden de StartChar es el orden de glifos (Refer usa ese índice).
    class Ref:
        """Envoltorio con draw(); TTGlyphPen lo usa para resolver componentes."""
        def __init__(self, glyph):
            self.glyph = glyph

        def draw(self, pen, glyfTable=None):
            self.glyph.draw(pen, glyfTable)

    glyf, metrics, cmap = {}, {}, {}
    glyphset = {}
    pending = []
    for g in glyphs:
        if g["refs"]:
            pending.append(g)
            continue
        pen = TTGlyphPen(glyphset)
        draw_contours(Cu2QuPen(pen, max_err=1.0), g["contours"])
        glyf[g["name"]] = pen.glyph()
        glyphset[g["name"]] = Ref(glyf[g["name"]])
    # Compuestos: se resuelven después de sus componentes simples (y hasta estabilizarse).
    while pending:
        progressed = False
        for g in list(pending):
            if all(names[r] in glyphset for r, _ in g["refs"] if r < len(names)):
                pen = TTGlyphPen(glyphset)
                draw_contours(Cu2QuPen(pen, max_err=1.0), g["contours"])
                for r, m in g["refs"]:
                    if r < len(names):
                        pen.addComponent(names[r], tuple(m))
                glyf[g["name"]] = pen.glyph()
                glyphset[g["name"]] = Ref(glyf[g["name"]])
                pending.remove(g)
                progressed = True
        if not progressed:
            raise SystemExit("componentes sin resolver: " + ", ".join(g["name"] for g in pending[:5]))
    for g in glyphs:
        metrics[g["name"]] = (g["width"] if g["width"] > 0 else upm // 2, 0)
        if g["uni"] >= 0 and g["name"] != ".notdef":
            cmap[g["uni"]] = g["name"]
    fb = FontBuilder(upm, isTTF=True)
    fb.setupGlyphOrder(names)
    fb.setupGlyf(glyf)
    fb.setupCharacterMap(cmap)
    fb.setupHorizontalMetrics(metrics)
    fb.setupHorizontalHeader(ascent=int(header.get("Ascent", 880)), descent=-int(header.get("Descent", 120)))
    fb.setupNameTable({"familyName": header.get("FamilyName", "FeverHD"), "styleName": "Regular"})
    fb.setupOS2(sTypoAscender=int(header.get("Ascent", 880)), sTypoDescender=-int(header.get("Descent", 120)),
                usWinAscent=int(header.get("OS2WinAscent", 1058)), usWinDescent=int(header.get("OS2WinDescent", 179)))
    fb.setupPost()
    fb.save(out)
    return len(glyphs), len(cmap)


if __name__ == "__main__":
    n, m = build(sys.argv[1], sys.argv[2])
    print(f"glifos: {n}, codepoints: {m}")
