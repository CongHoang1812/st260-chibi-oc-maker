"""Chuyển SVG glyph xuất từ Figma sang VectorDrawable.

Hỗ trợ đúng những gì Figma xuất cho glyph của app này:
- <path d fill>, <rect x y width height rx fill stroke>, <use href=#id fill>
- viền qua <mask>: mask chỉ chứa hình → viền trong (clip-path); mask = rect trắng trừ hình → viền ngoài (vẽ trước fill)
- filter bóng đổ feOffset dy (alpha 0.2) → bản sao hình lệch dy, màu đen 20%
Bỏ qua clip-path khung 0..W (trùng viewport).

python svg2vd.py in.svg out.xml
"""
import re
import sys
import xml.etree.ElementTree as ET

NS = {"svg": "http://www.w3.org/2000/svg", "xlink": "http://www.w3.org/1999/xlink"}
XLINK = "{http://www.w3.org/1999/xlink}href"
SHADOW = "#33000000"


def tag(e):
    return e.tag.split("}")[1]


def num(v, default=0.0):
    return float(v) if v is not None else default


def fmt(v):
    return ("%.3f" % v).rstrip("0").rstrip(".")


def rect_path(e):
    x, y = num(e.get("x")), num(e.get("y"))
    w, h = num(e.get("width")), num(e.get("height"))
    r = min(num(e.get("rx", e.get("ry"))), w / 2, h / 2)
    if r <= 0:
        return f"M{fmt(x)},{fmt(y)}H{fmt(x + w)}V{fmt(y + h)}H{fmt(x)}Z"
    return (
        f"M{fmt(x + r)},{fmt(y)}H{fmt(x + w - r)}A{fmt(r)},{fmt(r)} 0 0 1 {fmt(x + w)},{fmt(y + r)}"
        f"V{fmt(y + h - r)}A{fmt(r)},{fmt(r)} 0 0 1 {fmt(x + w - r)},{fmt(y + h)}"
        f"H{fmt(x + r)}A{fmt(r)},{fmt(r)} 0 0 1 {fmt(x)},{fmt(y + h - r)}"
        f"V{fmt(y + r)}A{fmt(r)},{fmt(r)} 0 0 1 {fmt(x + r)},{fmt(y)}Z"
    )


def color(c, opacity=None):
    c = c.strip()
    if not re.fullmatch(r"#[0-9a-fA-F]{6}", c):
        if c.lower() == "white":
            c = "#FFFFFF"
        elif c.lower() == "black":
            c = "#000000"
        else:
            raise ValueError("màu không hỗ trợ: " + c)
    a = 255 if opacity is None else round(float(opacity) * 255)
    return "#%02X%s" % (a, c[1:].upper())


def convert(src):
    root = ET.parse(src).getroot()
    w, h = root.get("width"), root.get("height")
    vb = root.get("viewBox").split()
    defs, rules = {}, {}
    for e in root.iter():
        if e.get("id") and tag(e) == "path" and e.get("d"):
            defs[e.get("id")] = e.get("d")
            rules[e.get("id")] = e.get("fill-rule")
    masks = {e.get("id"): e for e in root.iter() if tag(e) == "mask"}
    filters = {}
    for f in root.iter():
        if tag(f) == "filter":
            off = [c for c in f if tag(c) == "feOffset"]
            filters[f.get("id")] = num(off[0].get("dy")) if off else 0.0

    def shape_of_mask(m):
        """(d của hình, viền trong?)"""
        kids = list(m)
        has_rect = any(tag(k) == "rect" for k in kids)
        parts = [defs[k.get(XLINK).lstrip("#")] if tag(k) == "use" else k.get("d")
                 for k in kids if tag(k) in ("use", "path")]
        if not parts:
            raise ValueError("mask lạ")
        return " ".join(parts), not has_rect

    out = []

    def emit_group(elems, dy):
        fills, inner, outer = [], [], []
        for e in elems:
            t = tag(e)
            if t == "use":
                ref = e.get(XLINK).lstrip("#")
                fills.append((defs[ref], e.get("fill"), None, rules.get(ref)))
            elif t in ("path", "rect"):
                d = e.get("d") if t == "path" else rect_path(e)
                fill = e.get("fill")
                if fill and fill != "none":
                    fills.append((d, fill, e.get("fill-opacity"), e.get("fill-rule")))
                stroke = e.get("stroke")
                if stroke and stroke != "none":
                    sw = num(e.get("stroke-width"), 1.0)
                    mref = e.get("mask")
                    if mref:
                        shape, inside = shape_of_mask(masks[mref[5:-1]])
                        (inner if inside else outer).append((d, stroke, sw, shape))
                    else:
                        outer.append((d, stroke, sw, None))
        if dy:
            out.append(f'    <group android:translateY="{fmt(dy)}">')
            for d, s, sw, _ in outer:
                out.append(f'        <path android:pathData="{d}" android:strokeColor="{SHADOW}" android:strokeWidth="{fmt(sw)}" />')
            for d, f, op, rule in fills:
                ft = ' android:fillType="evenOdd"' if rule == "evenodd" else ""
                out.append(f'        <path android:pathData="{d}" android:fillColor="{SHADOW}"{ft} />')
            out.append("    </group>")
        for d, s, sw, _ in outer:
            out.append(f'    <path android:pathData="{d}" android:strokeColor="{color(s)}" android:strokeWidth="{fmt(sw)}" />')
        for d, f, op, rule in fills:
            ft = ' android:fillType="evenOdd"' if rule == "evenodd" else ""
            out.append(f'    <path android:pathData="{d}" android:fillColor="{color(f, op)}"{ft} />')
        for d, s, sw, shape in inner:
            out.append("    <group>")
            out.append(f'        <clip-path android:pathData="{shape}" />')
            out.append(f'        <path android:pathData="{d}" android:strokeColor="{color(s)}" android:strokeWidth="{fmt(sw)}" />')
            out.append("    </group>")

    def walk(node, dy):
        leaf = []
        for c in node:
            t = tag(c)
            if t in ("defs", "mask", "clipPath", "filter"):
                continue
            if t == "g":
                if leaf:
                    emit_group(leaf, dy)
                    leaf = []
                f = c.get("filter")
                walk(c, filters[f[5:-1]] if f else dy)
            else:
                leaf.append(c)
        if leaf:
            emit_group(leaf, dy)

    walk(root, 0.0)
    head = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f'<!-- Sinh từ SVG Figma bằng svg2vd.py ({src.split("/")[-1].split(chr(92))[-1]}) -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{fmt(float(w))}dp"\n    android:height="{fmt(float(h))}dp"\n'
        f'    android:viewportWidth="{vb[2]}"\n    android:viewportHeight="{vb[3]}">\n'
    )
    return head + "\n".join(out) + "\n</vector>\n"


if __name__ == "__main__":
    open(sys.argv[2], "w", encoding="utf-8", newline="\n").write(convert(sys.argv[1]))
