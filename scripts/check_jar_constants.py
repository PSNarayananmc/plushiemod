"""Check which float constants exist in classes inside the deployed jar.

Determines whether the deployed jar matches the current source state:
- 1.501 in the renderers = the vanilla-style translate chain (correct)
- 18.8/17.8 in PlushieGeometry = the mouth decal cube (source line 42) present
- 3.01 = face decal plane, 22.8 = leg pivot, 70.0 = leg rotation
"""
import struct
import zipfile

JAR = "/home/z/my-project/releases/plushie_party-26.2.jar"
VALUES = {
    "1.501": 1.501, "-1.501": -1.501, "1.5": 1.5, "-1.5": -1.5,
    "18.8": 18.8, "17.8": 17.8, "3.01": 3.01, "3.02": 3.02,
    "22.8": 22.8, "70.0": 70.0, "24.0": 24.0, "0.625": 0.625,
    "1.93511": 1.93511, "14.0": 14.0, "18.3": 18.3,
}
CLASSES = [
    "com/redeagle/plushieparty/client/render/CreeperPlushieRenderer.class",
    "com/redeagle/plushieparty/client/render/PlushieItemRenderers.class",
    "com/redeagle/plushieparty/client/model/PlushieGeometry.class",
]

encodings = {name: struct.pack(">f", val) for name, val in VALUES.items()}

with zipfile.ZipFile(JAR) as zf:
    for cls in CLASSES:
        data = zf.read(cls)
        found = [name for name, enc in encodings.items() if enc in data]
        print(f"{cls.split('/')[-1]} ({len(data)} bytes): {sorted(found)}")
