#!/usr/bin/env python3
"""生成 tabBar 图标（48x48 RGBA PNG，线性风格）。纯标准库，无需 PIL。
未选中 #C8C8C8，选中 #1A1A1A。运行一次生成 static/tabbar 下 8 张图。"""
import os, struct, zlib, math

SIZE = 81  # 27px @3x，微信推荐
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "static", "tabbar")

UNSEL = (0xC8, 0xC8, 0xC8)
SEL = (0x1A, 0x1A, 0x1A)


def canvas():
    return [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]


def put(buf, x, y, color, a=255):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < SIZE and 0 <= yi < SIZE:
        buf[yi][xi] = (color[0], color[1], color[2], a)


def stroke_line(buf, x0, y0, x1, y1, color, w=5):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
    r = w / 2
    for i in range(steps + 1):
        t = i / steps
        cx = x0 + (x1 - x0) * t
        cy = y0 + (y1 - y0) * t
        disc(buf, cx, cy, r, color)


def disc(buf, cx, cy, r, color, fill=True):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x - cx, y - cy)
            if fill and d <= r:
                put(buf, x, y, color)
            elif not fill and abs(d - r) <= 1.0:
                put(buf, x, y, color)


def ring(buf, cx, cy, r, color, w=5):
    for rr in [r - i * 0.4 for i in range(int(w * 2.5))]:
        for a in range(0, 3600, 3):
            rad = a / 10 * math.pi / 180
            put(buf, cx + rr * math.cos(rad), cy + rr * math.sin(rad), color)


def rect_outline(buf, x0, y0, x1, y1, color, w=5):
    stroke_line(buf, x0, y0, x1, y0, color, w)
    stroke_line(buf, x1, y0, x1, y1, color, w)
    stroke_line(buf, x1, y1, x0, y1, color, w)
    stroke_line(buf, x0, y1, x0, y0, color, w)


def draw_home(buf, c):
    stroke_line(buf, 14, 40, 40, 16, c)  # left roof
    stroke_line(buf, 40, 16, 67, 40, c)  # right roof
    stroke_line(buf, 20, 38, 20, 64, c)  # left wall
    stroke_line(buf, 61, 38, 61, 64, c)  # right wall
    stroke_line(buf, 20, 64, 61, 64, c)  # base
    rect_outline(buf, 34, 48, 47, 64, c, 4)  # door


def draw_category(buf, c):
    for gx in (18, 45):
        for gy in (18, 45):
            rect_outline(buf, gx, gy, gx + 18, gy + 18, c, 5)


def draw_cart(buf, c):
    stroke_line(buf, 12, 16, 22, 16, c)  # handle
    stroke_line(buf, 22, 16, 28, 46, c)  # to basket
    stroke_line(buf, 24, 46, 66, 46, c)  # basket top
    stroke_line(buf, 24, 46, 30, 62, c)
    stroke_line(buf, 66, 46, 60, 62, c)
    stroke_line(buf, 30, 62, 60, 62, c)
    disc(buf, 34, 70, 4, c)
    disc(buf, 56, 70, 4, c)


def draw_mine(buf, c):
    ring(buf, 40, 28, 13, c)  # head
    stroke_line(buf, 18, 66, 26, 50, c)  # shoulders
    stroke_line(buf, 26, 50, 54, 50, c)
    stroke_line(buf, 54, 50, 62, 66, c)
    stroke_line(buf, 18, 66, 62, 66, c)


DRAW = {
    "home": draw_home,
    "category": draw_category,
    "cart": draw_cart,
    "mine": draw_mine,
}


def encode_png(buf):
    raw = bytearray()
    for row in buf:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    return sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in DRAW.items():
        for suffix, color in (("", UNSEL), ("-active", SEL)):
            b = canvas()
            fn(b, color)
            path = os.path.join(OUT, f"{name}{suffix}.png")
            with open(path, "wb") as f:
                f.write(encode_png(b))
            print("wrote", path)


if __name__ == "__main__":
    main()
