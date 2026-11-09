"""Ghép nền nút icon (Figma "icon" 44×44, cùng kiểu WoodButtonDrawable) với glyph VectorDrawable.

python compose_icon.py glyph.xml out.xml "<ghi chú nguồn>"
Màu nền dùng @color token để đổi theo theme; glyph giữ màu Figma.
"""
import re
import sys
import xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"
SIZE = 44

BG = """    <!-- Nền nút (Figma: khung trắng bo 16 viền #662A00, lõi cam bo 12 viền #803600, bóng đáy 3, đốm sáng) -->
    <path android:fillColor="@color/white" android:pathData="M16,0H28A16,16 0 0 1 44,16V28A16,16 0 0 1 28,44H16A16,16 0 0 1 0,28V16A16,16 0 0 1 16,0Z" />
    <group>
        <clip-path android:pathData="M16,0H28A16,16 0 0 1 44,16V28A16,16 0 0 1 28,44H16A16,16 0 0 1 0,28V16A16,16 0 0 1 16,0Z" />
        <path android:fillColor="@color/button_drop" android:pathData="M16,6H28A12,12 0 0 1 40,18V30A12,12 0 0 1 28,42H16A12,12 0 0 1 4,30V18A12,12 0 0 1 16,6Z" />
    </group>
    <path android:fillColor="@color/tab_on_shadow" android:pathData="M16,4H28A12,12 0 0 1 40,16V28A12,12 0 0 1 28,40H16A12,12 0 0 1 4,28V16A12,12 0 0 1 16,4Z" />
    <group>
        <clip-path android:pathData="M16,4H28A12,12 0 0 1 40,16V28A12,12 0 0 1 28,40H16A12,12 0 0 1 4,28V16A12,12 0 0 1 16,4Z" />
        <path android:fillColor="@color/orange_button" android:pathData="M16,1H28A12,12 0 0 1 40,13V25A12,12 0 0 1 28,37H16A12,12 0 0 1 4,25V13A12,12 0 0 1 16,1Z" />
    </group>
    <path android:strokeColor="@color/button_inner_stroke" android:strokeWidth="1" android:pathData="M16,4.5H28A11.5,11.5 0 0 1 39.5,16V28A11.5,11.5 0 0 1 28,39.5H16A11.5,11.5 0 0 1 4.5,28V16A11.5,11.5 0 0 1 16,4.5Z" />
    <path android:strokeColor="@color/brown_dark" android:strokeWidth="1" android:pathData="M16,0.5H28A15.5,15.5 0 0 1 43.5,16V28A15.5,15.5 0 0 1 28,43.5H16A15.5,15.5 0 0 1 0.5,28V16A15.5,15.5 0 0 1 16,0.5Z" />
    <path android:fillColor="@color/button_highlight_dot" android:pathData="M7,8.5A3,2.5 0 1 0 13,8.5A3,2.5 0 1 0 7,8.5Z" />
"""


def main(glyph, out, note):
    src = open(glyph, encoding="utf-8").read()
    root = ET.fromstring(src.split("\n", 1)[1] if src.startswith("<?xml") else src)
    gw, gh = float(root.get(A + "viewportWidth")), float(root.get(A + "viewportHeight"))
    tx, ty = (SIZE - gw) / 2, (SIZE - gh) / 2
    body = re.search(r"<vector[^>]*>\n(.*)</vector>", src, re.S).group(1)
    body = "\n".join("    " + line if line.strip() else line for line in body.rstrip("\n").split("\n"))
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- Nút icon 44dp: {note}. Sinh bằng svg2vd.py + compose_icon.py, không sửa tay phần glyph. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{SIZE}dp"\n    android:height="{SIZE}dp"\n'
        f'    android:viewportWidth="{SIZE}"\n    android:viewportHeight="{SIZE}">\n'
        + BG
        + f'    <group android:translateX="{tx:g}" android:translateY="{ty:g}">\n'
        + body + "\n    </group>\n</vector>\n"
    )
    open(out, "w", encoding="utf-8", newline="\n").write(xml)


if __name__ == "__main__":
    main(*sys.argv[1:4])
