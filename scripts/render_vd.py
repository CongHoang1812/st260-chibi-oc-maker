"""Dựng ảnh PNG từ VectorDrawable (đủ cho glyph sinh bởi svg2vd.py) để soát bằng mắt.

python scripts/render_vd.py <thư mục site-packages có PIL> out.png <tỉ lệ> a.xml [b.xml ...]   (mỗi file một ô, ghép ngang)
Hỗ trợ: path fill (nonZero / evenOdd), stroke (xấp xỉ bằng nét tròn), clip-path, group translateX / translateY.
"""
import math
import re
import sys
import xml.etree.ElementTree as ET

sys.path.append(sys.argv[1])
from PIL import Image, ImageChops, ImageDraw  # noqa: E402

A = "{http://schemas.android.com/apk/res/android}"
TOKEN = re.compile(r"[MmLlHhVvCcSsQqTtAaZz]|-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")


def arc(p0, rx, ry, phi, fa, fs, p1, n=24):
    (x1, y1), (x2, y2) = p0, p1
    if rx == 0 or ry == 0:
        return [p1]
    c, s = math.cos(math.radians(phi)), math.sin(math.radians(phi))
    dx, dy = (x1 - x2) / 2, (y1 - y2) / 2
    x1p, y1p = c * dx + s * dy, -s * dx + c * dy
    rx, ry = abs(rx), abs(ry)
    lam = x1p ** 2 / rx ** 2 + y1p ** 2 / ry ** 2
    if lam > 1:
        rx, ry = rx * math.sqrt(lam), ry * math.sqrt(lam)
    num = rx ** 2 * ry ** 2 - rx ** 2 * y1p ** 2 - ry ** 2 * x1p ** 2
    den = rx ** 2 * y1p ** 2 + ry ** 2 * x1p ** 2
    k = math.sqrt(max(0, num / den)) * (-1 if fa == fs else 1)
    cxp, cyp = k * rx * y1p / ry, -k * ry * x1p / rx
    cx, cy = c * cxp - s * cyp + (x1 + x2) / 2, s * cxp + c * cyp + (y1 + y2) / 2
    a1 = math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
    a2 = math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx)
    da = a2 - a1
    if fs and da < 0:
        da += 2 * math.pi
    if not fs and da > 0:
        da -= 2 * math.pi
    pts = []
    for i in range(1, n + 1):
        t = a1 + da * i / n
        pts.append((cx + rx * math.cos(t) * c - ry * math.sin(t) * s, cy + rx * math.cos(t) * s + ry * math.sin(t) * c))
    return pts


def parse(d):
    toks = TOKEN.findall(d)
    i, cmd = 0, None
    subs, cur = [], []
    x = y = sx = sy = 0.0
    lc = None

    def nums(k):
        nonlocal i
        v = [float(t) for t in toks[i:i + k]]
        i += k
        return v

    def bez(p0, p1, p2, p3, n=16):
        return [(
            (1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t ** 2 * p2[0] + t ** 3 * p3[0],
            (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t ** 2 * p2[1] + t ** 3 * p3[1],
        ) for t in (j / n for j in range(1, n + 1))]

    while i < len(toks):
        if toks[i].isalpha():
            cmd = toks[i]
            i += 1
        rel = cmd.islower()
        C = cmd.upper()
        ox, oy = (x, y) if rel else (0.0, 0.0)
        if C == "M":
            a, b = nums(2)
            if cur:
                subs.append(cur)
            x, y = ox + a, oy + b
            sx, sy = x, y
            cur = [(x, y)]
            cmd = "l" if rel else "L"
            lc = None
        elif C == "L":
            a, b = nums(2)
            x, y = ox + a, oy + b
            cur.append((x, y))
            lc = None
        elif C == "H":
            (a,) = nums(1)
            x = ox + a
            cur.append((x, y))
            lc = None
        elif C == "V":
            (b,) = nums(1)
            y = oy + b
            cur.append((x, y))
            lc = None
        elif C == "C":
            v = nums(6)
            p1, p2, p3 = (ox + v[0], oy + v[1]), (ox + v[2], oy + v[3]), (ox + v[4], oy + v[5])
            cur += bez((x, y), p1, p2, p3)
            lc = p2
            x, y = p3
        elif C == "S":
            v = nums(4)
            p1 = (2 * x - lc[0], 2 * y - lc[1]) if lc else (x, y)
            p2, p3 = (ox + v[0], oy + v[1]), (ox + v[2], oy + v[3])
            cur += bez((x, y), p1, p2, p3)
            lc = p2
            x, y = p3
        elif C == "Q":
            v = nums(4)
            q, p3 = (ox + v[0], oy + v[1]), (ox + v[2], oy + v[3])
            p1 = (x + 2 / 3 * (q[0] - x), y + 2 / 3 * (q[1] - y))
            p2 = (p3[0] + 2 / 3 * (q[0] - p3[0]), p3[1] + 2 / 3 * (q[1] - p3[1]))
            cur += bez((x, y), p1, p2, p3)
            lc = None
            x, y = p3
        elif C == "A":
            v = nums(7)
            p1 = (ox + v[5], oy + v[6])
            cur += arc((x, y), v[0], v[1], v[2], int(v[3]), int(v[4]), p1)
            x, y = p1
            lc = None
        elif C == "Z":
            cur.append((sx, sy))
            subs.append(cur)
            cur = []
            x, y = sx, sy
            lc = None
        else:
            raise ValueError(cmd)
    if cur:
        subs.append(cur)
    return subs


def fill_mask(subs, size, s, dy, evenodd, dx=0.0):
    W, H = size
    m = Image.new("L", size, 0)
    px = m.load()
    edges = []
    for sp in subs:
        for (x0, y0), (x1, y1) in zip(sp, sp[1:] + sp[:1]):
            if y0 != y1:
                edges.append(((x0 + dx) * s, (y0 + dy) * s, (x1 + dx) * s, (y1 + dy) * s))
    for row in range(H):
        yc = row + 0.5
        xs = []
        for x0, y0, x1, y1 in edges:
            if (y0 <= yc < y1) or (y1 <= yc < y0):
                xs.append((x0 + (yc - y0) * (x1 - x0) / (y1 - y0), 1 if y1 > y0 else -1))
        xs.sort()
        wind = 0
        for (xa, wa), (xb, _) in zip(xs, xs[1:]):
            wind += wa
            inside = (wind % 2 == 1) if evenodd else (wind != 0)
            if inside:
                for col in range(max(0, int(xa + 0.5)), min(W, int(xb + 0.5))):
                    px[col, row] = 255
    return m


def stroke_mask(subs, size, s, dy, width, dx=0.0):
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    r = width * s / 2
    for sp in subs:
        pts = [((px + dx) * s, (py + dy) * s) for px, py in sp]
        d.line(pts, fill=255, width=max(1, int(round(width * s))))
        for p in pts:
            d.ellipse((p[0] - r, p[1] - r, p[0] + r, p[1] + r), fill=255)
    return m


COLORS = {}
COLORS_XML = str(__import__("pathlib").Path(__file__).resolve().parent.parent / "app/src/main/res/values/colors.xml")
for _c in ET.parse(COLORS_XML).getroot():
    COLORS[_c.get("name")] = _c.text.strip()


def argb(c):
    if c.startswith("@color/"):
        c = COLORS[c[7:]]
    c = c.lstrip("#")
    if len(c) == 6:
        c = "FF" + c
    return int(c[2:4], 16), int(c[4:6], 16), int(c[6:8], 16), int(c[0:2], 16)


def render(path, s):
    root = ET.parse(path).getroot()
    vw, vh = float(root.get(A + "viewportWidth")), float(root.get(A + "viewportHeight"))
    size = (int(math.ceil(vw * s)), int(math.ceil(vh * s)))
    img = Image.new("RGBA", size, (0, 0, 0, 0))

    def draw(node, dy, clip, dx=0.0):
        for c in node:
            t = c.tag
            if t == "group":
                gdy = dy + float(c.get(A + "translateY", 0))
                gdx = dx + float(c.get(A + "translateX", 0))
                gclip = clip
                for cp in c.findall("clip-path"):
                    cm = fill_mask(parse(cp.get(A + "pathData")), size, s, gdy, False, gdx)
                    gclip = cm if gclip is None else ImageChops.multiply(gclip, cm)
                draw(c, gdy, gclip, gdx)
            elif t == "path":
                subs = parse(c.get(A + "pathData"))
                for attr, mk in (("fillColor", "f"), ("strokeColor", "s")):
                    col = c.get(A + attr)
                    if not col:
                        continue
                    if mk == "f":
                        m = fill_mask(subs, size, s, dy, c.get(A + "fillType") == "evenOdd", dx)
                    else:
                        m = stroke_mask(subs, size, s, dy, float(c.get(A + "strokeWidth", 1)), dx)
                    if clip is not None:
                        m = ImageChops.multiply(m, clip)
                    r, g, b, a = argb(col)
                    layer = Image.new("RGBA", size, (r, g, b, 255))
                    layer.putalpha(m.point(lambda v: v * a // 255))
                    img.alpha_composite(layer)

    draw(root, 0.0, None)
    return img


if __name__ == "__main__":
    out, s = sys.argv[2], float(sys.argv[3])
    tiles = [render(p, s) for p in sys.argv[4:]]
    pad = int(8 * s)
    W = sum(t.width for t in tiles) + pad * (len(tiles) + 1)
    H = max(t.height for t in tiles) + 2 * pad
    sheet = Image.new("RGBA", (W, H), (255, 129, 39, 255))
    x = pad
    for t in tiles:
        sheet.alpha_composite(t, (x, pad))
        x += t.width + pad
    sheet.save(out)
