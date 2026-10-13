#!/usr/bin/env python3
"""Second-pass authored-looking summon FX for TURNBOUND.

Runs after generate_summon_assets.py. Portraits still come from the production
GeckoLib models; this pass only replaces reusable cinematic UI/FX textures.
"""
from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

import generate_summon_assets as base


def clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def fill_polygon(img, points, color):
    if len(points) < 3:
        return
    h = len(img)
    w = len(img[0]) if h else 0
    min_y = max(0, int(math.floor(min(p[1] for p in points))))
    max_y = min(h - 1, int(math.ceil(max(p[1] for p in points))))
    n = len(points)
    for y in range(min_y, max_y + 1):
        scan_y = y + 0.5
        xs = []
        for i in range(n):
            x1, y1 = points[i]
            x2, y2 = points[(i + 1) % n]
            if (y1 <= scan_y < y2) or (y2 <= scan_y < y1):
                if abs(y2 - y1) > 1e-6:
                    xs.append(x1 + (scan_y - y1) * (x2 - x1) / (y2 - y1))
        xs.sort()
        for i in range(0, len(xs) - 1, 2):
            xa = max(0, int(math.ceil(xs[i])))
            xb = min(w - 1, int(math.floor(xs[i + 1])))
            for x in range(xa, xb + 1):
                img[y][x] = base.blend(img[y][x], color)


def soft_line(img, x0, y0, x1, y1, rgb, alpha=180, width=1):
    for extra, mul in ((width + 3, 0.12), (width + 1, 0.28), (width, 1.0)):
        base.draw_line(img, x0, y0, x1, y1,
                       (rgb[0], rgb[1], rgb[2], int(alpha * mul)), extra)


def stage_background(w=1000, h=625):
    img = base.blank(w, h)
    cx, cy = w * 0.54, h * 0.46
    for y in range(h):
        vy = y / max(1, h - 1)
        for x in range(w):
            vx = x / max(1, w - 1)
            radial = clamp(1.0 - math.hypot((x - cx) / (w * 0.56), (y - cy) / (h * 0.72)))
            cyan = radial ** 2.2
            gold = clamp(1.0 - math.hypot((x - w * 0.76) / (w * 0.44), (y - h * 0.45) / (h * 0.70))) ** 3.2
            horizon = math.exp(-((vy - 0.66) / 0.20) ** 2)
            r = int(4 + 4 * vy + 7 * cyan + 9 * gold)
            g = int(8 + 9 * vy + 20 * cyan + 9 * gold + 4 * horizon)
            b = int(20 + 18 * vy + 42 * cyan + 8 * gold + 8 * horizon)
            edge = max(abs(vx - 0.5) * 2.0, abs(vy - 0.52) * 1.45)
            dim = int(12 * clamp(edge - 0.45) / 0.55)
            img[y][x] = (max(0, r - dim), max(0, g - dim), max(0, b - dim), 255)

    rng = random.Random(0x71B0A2)
    for _ in range(330):
        x = rng.randrange(w)
        y = rng.randrange(int(h * 0.82))
        a = rng.randrange(40, 160)
        c = (110 + rng.randrange(70), 180 + rng.randrange(60), 220 + rng.randrange(35), a)
        img[y][x] = base.blend(img[y][x], c)
        if a > 120 and x + 1 < w:
            img[y][x + 1] = base.blend(img[y][x + 1], (c[0], c[1], c[2], a // 3))

    for anchor in (58, 154, w - 154, w - 58):
        soft_line(img, anchor, 54, anchor, h - 18, (12, 25, 43), 210, 9)
        soft_line(img, anchor + (18 if anchor < w / 2 else -18), 88,
                  anchor + (44 if anchor < w / 2 else -44), h - 36, (30, 63, 84), 80, 2)

    soft_line(img, w * 0.12, h * 0.73, w * 0.88, h * 0.73, (68, 151, 188), 72, 1)
    for i in range(-4, 5):
        tx = cx + i * w * 0.11
        soft_line(img, cx, h * 0.69, tx, h, (29, 72, 102), 54, 1)
    return img


def flare_asset(w, h, rgb):
    img = base.blank(w, h)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        dy = abs(y - cy) / max(1.0, h * 0.5)
        for x in range(w):
            dx = abs(x - cx) / max(1.0, w * 0.5)
            line = math.exp(-dy * 10.0) * (1.0 - dx) ** 1.7
            core = math.exp(-((x - cx) ** 2 + (y - cy) ** 2) / max(1.0, (h * 0.20) ** 2))
            ghost = math.exp(-((x - w * 0.68) ** 2 + (y - cy) ** 2) / max(1.0, (h * 0.16) ** 2)) * 0.22
            a = int(235 * clamp(line * 0.62 + core + ghost))
            if a:
                img[y][x] = (rgb[0], rgb[1], rgb[2], a)
    return img


def sigil_asset(size=640):
    img = base.blank(size, size)
    cx = cy = size / 2
    cyan = (94, 208, 246)
    gold = (226, 186, 92)
    for radius, color, alpha, width in (
            (0.41, cyan, 110, 1), (0.34, cyan, 74, 1), (0.275, gold, 62, 1), (0.18, cyan, 48, 1)):
        prev = None
        for i in range(361):
            a = math.radians(i)
            p = (cx + math.cos(a) * size * radius, cy + math.sin(a) * size * radius)
            if prev is not None:
                soft_line(img, prev[0], prev[1], p[0], p[1], color, alpha, width)
            prev = p
    for i in range(16):
        a = math.radians(i * 22.5)
        r0 = size * (0.285 if i % 2 else 0.30)
        r1 = size * (0.395 if i % 2 else 0.41)
        color = gold if i % 4 == 0 else cyan
        soft_line(img, cx + math.cos(a) * r0, cy + math.sin(a) * r0,
                  cx + math.cos(a) * r1, cy + math.sin(a) * r1, color, 68, 1)
    star = [(cx, cy - size * 0.13), (cx + size * 0.045, cy - size * 0.035),
            (cx + size * 0.13, cy), (cx + size * 0.045, cy + size * 0.035),
            (cx, cy + size * 0.13), (cx - size * 0.045, cy + size * 0.035),
            (cx - size * 0.13, cy), (cx - size * 0.045, cy - size * 0.035)]
    fill_polygon(img, star, (71, 179, 224, 23))
    for i in range(len(star)):
        p0, p1 = star[i], star[(i + 1) % len(star)]
        soft_line(img, p0[0], p0[1], p1[0], p1[1], cyan, 94, 1)
    return img


def shards_asset(w, h, rgb, seed):
    img = base.blank(w, h)
    rng = random.Random(seed)
    for _ in range(36):
        cx = rng.uniform(w * 0.08, w * 0.94)
        cy = rng.uniform(h * 0.08, h * 0.92)
        length = rng.uniform(10, 42)
        width = rng.uniform(2.0, 8.0)
        a = rng.uniform(-1.2, 1.2)
        ux, uy = math.cos(a), math.sin(a)
        vx, vy = -uy, ux
        points = [
            (cx - ux * length * 0.55, cy - uy * length * 0.55),
            (cx + vx * width, cy + vy * width),
            (cx + ux * length * 0.55, cy + uy * length * 0.55),
            (cx - vx * width * 0.45, cy - vy * width * 0.45),
        ]
        alpha = rng.randrange(36, 112)
        fill_polygon(img, points, (rgb[0], rgb[1], rgb[2], alpha // 2))
        for j in range(4):
            p0, p1 = points[j], points[(j + 1) % 4]
            base.draw_line(img, p0[0], p0[1], p1[0], p1[1], (rgb[0], rgb[1], rgb[2], alpha), 0)
    return img


def crystal_asset(w=360, h=500):
    img = base.blank(w, h)
    cx = w / 2
    silhouette = [(cx, h * 0.035), (w * 0.80, h * 0.27), (w * 0.72, h * 0.77),
                  (cx, h * 0.965), (w * 0.28, h * 0.77), (w * 0.20, h * 0.27)]
    for y in range(h):
        for x in range(w):
            d = math.hypot((x - cx) / (w * 0.55), (y - h * 0.50) / (h * 0.56))
            if d < 1.0:
                a = int(52 * (1.0 - d) ** 2)
                if a:
                    img[y][x] = (78, 194, 255, a)
    fill_polygon(img, silhouette, (54, 161, 229, 230))
    facets = [
        ([(cx, h * .035), (w * .50, h * .50), (w * .20, h * .27)], (159, 232, 255, 128)),
        ([(cx, h * .035), (w * .80, h * .27), (w * .50, h * .50)], (84, 205, 249, 112)),
        ([(w * .20, h * .27), (w * .50, h * .50), (w * .28, h * .77)], (61, 175, 234, 150)),
        ([(w * .80, h * .27), (w * .72, h * .77), (w * .50, h * .50)], (41, 130, 211, 145)),
        ([(w * .28, h * .77), (w * .50, h * .50), (cx, h * .965)], (83, 201, 242, 115)),
        ([(w * .72, h * .77), (cx, h * .965), (w * .50, h * .50)], (35, 117, 198, 122)),
    ]
    for pts, color in facets:
        fill_polygon(img, pts, color)
    for i in range(len(silhouette)):
        p0, p1 = silhouette[i], silhouette[(i + 1) % len(silhouette)]
        soft_line(img, p0[0], p0[1], p1[0], p1[1], (199, 244, 255), 195, 1)
    for p in silhouette:
        soft_line(img, cx, h * 0.50, p[0], p[1], (193, 240, 255), 92, 1)
    return img


def card_asset(w, h, rgb):
    img = base.blank(w, h)
    for y in range(h):
        t = y / max(1, h - 1)
        inner = 0.20 + 0.80 * (1.0 - abs(t - 0.42))
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            glow = clamp(edge / 18.0)
            img[y][x] = (
                int(6 + rgb[0] * 0.025 * inner),
                int(10 + rgb[1] * 0.035 * inner),
                int(20 + rgb[2] * 0.055 * inner),
                int(236 * glow + 18),
            )
    soft_line(img, 8, 7, w - 9, 7, rgb, 210, 1)
    soft_line(img, 7, 8, 7, h - 9, rgb, 115, 1)
    soft_line(img, w - 8, 8, w - 8, h - 9, rgb, 115, 1)
    soft_line(img, 8, h - 8, w - 9, h - 8, rgb, 155, 1)
    for d in (0, 1):
        base.draw_line(img, 7 + d, 16, 16, 7 + d, (rgb[0], rgb[1], rgb[2], 180), 0)
        base.draw_line(img, w - 17, 7 + d, w - 8 - d, 16, (rgb[0], rgb[1], rgb[2], 180), 0)
    return img


def veil_asset(w=1000, h=625):
    img = base.blank(w, h)
    for y in range(h):
        vy = y / max(1, h - 1)
        top = clamp((0.18 - vy) / 0.18)
        bottom = clamp((vy - 0.78) / 0.22)
        for x in range(w):
            vx = x / max(1, w - 1)
            side = max(clamp((0.08 - vx) / 0.08), clamp((vx - 0.92) / 0.08))
            a = int(165 * max(top, bottom, side))
            img[y][x] = (0, 0, 0, a)
    return img


def sweep_asset(w=640, h=180):
    img = base.blank(w, h)
    for y in range(h):
        for x in range(w):
            u = x - (y - h / 2) * 0.65
            d = abs(u - w * 0.5) / (w * 0.5)
            vy = abs(y - h / 2) / (h * 0.5)
            a = int(220 * clamp(1.0 - d * 4.2) * clamp(1.0 - vy) ** 1.8)
            if a:
                img[y][x] = (255, 242, 205, a)
    return img


def generate(out: Path):
    out.mkdir(parents=True, exist_ok=True)
    base.write_png(out / "bg_archive.png", 1000, 625, stage_background())
    base.write_png(out / "sigil_back.png", 640, 640, sigil_asset())
    base.write_png(out / "flare_cyan.png", 768, 192, flare_asset(768, 192, (108, 222, 255)))
    base.write_png(out / "flare_violet.png", 768, 192, flare_asset(768, 192, (199, 126, 255)))
    base.write_png(out / "flare_gold.png", 768, 192, flare_asset(768, 192, (255, 218, 122)))
    base.write_png(out / "shards_cyan.png", 1000, 625, shards_asset(1000, 625, (105, 214, 255), 0xC1A0))
    base.write_png(out / "shards_violet.png", 1000, 625, shards_asset(1000, 625, (202, 130, 255), 0xB105))
    base.write_png(out / "shards_gold.png", 1000, 625, shards_asset(1000, 625, (255, 215, 116), 0xA017))
    base.write_png(out / "veil_soft.png", 1000, 625, veil_asset())
    base.write_png(out / "light_sweep.png", 640, 180, sweep_asset())
    base.write_png(out / "crystal.png", 360, 500, crystal_asset())
    base.write_png(out / "card_common.png", 180, 250, card_asset(180, 250, (87, 169, 219)))
    base.write_png(out / "card_rare.png", 180, 250, card_asset(180, 250, (198, 126, 255)))
    base.write_png(out / "card_legendary.png", 180, 250, card_asset(180, 250, (255, 204, 92)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", required=True)
    ap.add_argument("--output", required=True)
    args = ap.parse_args()
    output = Path(args.output).resolve()
    generate(output / "assets/turnbound/textures/gui/summon")


if __name__ == "__main__":
    main()
