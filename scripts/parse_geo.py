#!/usr/bin/env python3
"""Parse Bedrock plushie geometry and dump compact cube/bone data."""
import json, sys

GEO = "/home/z/my-project/addon_extracted/plushie_party_rp/models/entity/creeper_plushie.geo.json"
data = json.load(open(GEO))
for geo in data["minecraft:geometry"]:
    desc = geo["description"]
    print(f"=== {desc['identifier']}  tex {desc['texture_width']}x{desc['texture_height']} ===")
    for bone in geo["bones"]:
        piv = bone.get("pivot")
        rot = bone.get("rotation")
        parent = bone.get("parent", "-")
        print(f"BONE {bone['name']} parent={parent} pivot={piv} rot={rot} cubes={len(bone.get('cubes', []))}")
        for i, c in enumerate(bone.get("cubes", [])):
            uv = c.get("uv")
            if isinstance(uv, dict):
                uv_repr = "; ".join(f"{k}:{v['uv']}+{v.get('uv_size')}" for k, v in uv.items())
            else:
                uv_repr = str(uv)
            print(f"  CUBE{i} origin={c['origin']} size={c['size']} pivot={c.get('pivot')} rot={c.get('rotation')} inflate={c.get('inflate')} uv[{uv_repr}]")
