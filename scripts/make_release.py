import json, os, urllib.request, urllib.error

PAT = os.environ["PAT"]
REPO = "PSNarayananmc/plushiemod"
JAR = "/home/z/my-project/releases/plushie_party-26.2.jar"
BASE = {"Authorization": "Bearer " + PAT, "Accept": "application/vnd.github+json", "User-Agent": "plushie-release-script"}


def call(url, data=None, headers=None, method=None):
    h = dict(BASE)
    h.update(headers or {})
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r) as resp:
            b = resp.read()
    except urllib.error.HTTPError as e:
        print("HTTP", e.code, "on", url)
        print(e.read().decode()[:600])
        raise
    return json.loads(b) if b[:1] == b"{" else b


# 1) latest commit on main
c = call("https://api.github.com/repos/%s/commits/main" % REPO)
print("LATEST COMMIT:", c["sha"][:7], "|", c["commit"]["message"].splitlines()[0], "|", c["commit"]["committer"]["date"])

# 2) create release (or reuse the tag if it already exists)
rel_body = {
    "tag_name": "v1.0.1",
    "target_commitish": "main",
    "name": "Plushie Party 26.2 — Fabric Loader 0.19.5+ build",
    "body": ("**Same mod, one fix:** now accepts Fabric Loader **0.19.5 or newer** (previously required 0.20.1).\n\n"
             "Fixes the startup error: `Replace mod 'Fabric Loader' (fabricloader) 0.19.5 with version 0.20.1 or later`\n\n"
             "**Requirements:** Minecraft Java 26.2 | Fabric Loader 0.19.5+ | Fabric API 0.161.0+26.2 | Java 25\n\n"
             "**Install:** 1) remove the old plushie_party jar from mods/  2) drop this jar + Fabric API into mods/\n\n"
             "All classes, assets, sounds and animations are byte-identical to the previous build — only the loader version gate changed."),
    "draft": False,
    "prerelease": False,
}
try:
    rel = call("https://api.github.com/repos/%s/releases" % REPO,
               data=json.dumps(rel_body).encode(),
               headers={"Content-Type": "application/json"}, method="POST")
    print("CREATED RELEASE:", rel["html_url"])
except urllib.error.HTTPError:
    rel = call("https://api.github.com/repos/%s/releases/tags/v1.0.1" % REPO)
    print("REUSING EXISTING RELEASE:", rel["html_url"])

# 3) attach the jar (skip if already there)
names = [a["name"] for a in rel.get("assets", [])]
if "plushie_party-26.2.jar" in names:
    print("ASSET ALREADY ATTACHED")
else:
    up = call("https://uploads.github.com/repos/%s/releases/%d/assets?name=plushie_party-26.2.jar" % (REPO, rel["id"]),
              data=open(JAR, "rb").read(), headers={"Content-Type": "application/octet-stream"})
    print("ASSET:", up["browser_download_url"], "(%d bytes)" % up["size"])
