#!/usr/bin/env python3
"""Offline visual verification: render the dumped plushie model with the real
texture sampled at the UVs. Painter's-algorithm rasterizer using PIL."""
import math, re, subprocess
from PIL import Image, ImageDraw

TEX = "/home/z/my-project/addon_extracted/plushie_party_rp/textures/re_plushie_party/creeper_plushie.png"
DUMP = "/tmp/model_dump.txt"

raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", TEX, "-f", "rawvideo", "-pix_fmt", "rgba", "-"],
                     capture_output=True).stdout
W = H = 32
def texel(u, v):
    x = min(W - 1, max(0, int(u * W)))
    y = min(H - 1, max(0, int(v * H)))
    o = (y * W + x) * 4
    return (raw[o], raw[o+1], raw[o+2], raw[o+3])

cubes = []
for line in open(DUMP):
    line = line.strip()
    if not line or line.startswith(("Note:", "Exception", "\tat", "DUMP")):
        continue
    parts = line.split("|")
    if len(parts) < 5:
        continue
    mn = tuple(float(x) for x in parts[2].split(","))
    mx = tuple(float(x) for x in parts[3].split(","))
    faces = []
    for fspec in parts[4].split(";"):
        normal, vlist = fspec.split(":")
        flat = [float(x) for x in vlist.split(",")]
        verts = [tuple(flat[i:i+5]) for i in range(0, len(flat), 5)]
        if all(len(v) == 5 for v in verts):
            faces.append((tuple(float(x) for x in normal.split(",")), verts))
    cubes.append((mn, mx, faces))

print(f"parsed {len(cubes)} cubes")

def project(p, yaw, pitch, scale=14.0, cx=110, cy=130):
    x, y, z = p
    y = 24 - y                      # flip to y-up for the view
    xr = math.radians(yaw); yr = math.radians(pitch)
    x1 = x * math.cos(xr) + z * math.sin(xr)
    z1 = -x * math.sin(xr) + z * math.cos(xr)
    y1 = y * math.cos(yr) - z1 * math.sin(yr)
    z2 = y * math.sin(yr) + z1 * math.cos(yr)
    return (x1 * scale + cx, -y1 * scale + cy, z2)

def render(yaw, pitch, path, size=(220, 260)):
    img = Image.new("RGB", size, (60, 62, 70))
    draw = ImageDraw.Draw(img)
    tris = []
    for mn, mx, faces in cubes:
        for normal, verts in faces:
            pts = [project((v[0], v[1], v[2]), yaw, pitch) for v in verts]
            u = sum(v[3] for v in verts) / 4
            v = sum(v[4] for v in verts) / 4
            r, g, b, a = texel(u, v)
            if a < 40:
                continue
            nx, ny, nz = normal
            lum = 0.6 + 0.4 * max(0.0, (0.35 * nx + 0.85 * (-ny) + 0.3 * nz))
            col = (int(r * lum), int(g * lum), int(b * lum))
            z_avg = sum(p[2] for p in pts) / 4
            area2 = 0.0
            for i in range(4):
                x0, y0, _ = pts[i]; x1, y1, _ = pts[(i + 1) % 4]
                area2 += x0 * y1 - x1 * y0
            if abs(area2) < 1e-6:
                continue
            tris.append((z_avg, [(p[0], p[1]) for p in pts], col))
    tris.sort(key=lambda t: t[0], reverse=True)
    for _, poly, col in tris:
        draw.polygon(poly, fill=col)
    img.save(path)
    print("saved", path, "faces drawn:", len(tris))

render(30, 22, "/tmp/view_iso.png")
render(180, 15, "/tmp/view_front.png")
render(90, 15, "/tmp/view_side.png")
combo = Image.new("RGB", (660, 260), (30, 30, 34))
for i, name in enumerate(["view_front", "view_iso", "view_side"]):
    combo.paste(Image.open(f"/tmp/{name}.png"), (i * 220, 0))
combo.save("/home/z/my-project/download/model_check.png")
print("combined -> /home/z/my-project/download/model_check.png")
