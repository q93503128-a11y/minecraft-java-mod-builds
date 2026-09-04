from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "tools/patch_civil_builder_storage_runtime.py"
source = BASE.read_text(encoding="utf-8")

bad = "builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, 0.86D);"
good = "builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.86D);"

# The first bad anchor is the fill service and is correct there. The historical retaining service
# uses integer Y for the direct move, so correct only the final occurrence before executing the
# otherwise identical validated patch logic.
index = source.rfind(bad)
if index < 0:
    raise SystemExit("civil v2 retaining anchor not found")
source = source[:index] + good + source[index + len(bad):]
if source.rfind(bad) >= index:
    raise SystemExit("civil v2 retaining anchor replacement failed")

exec(compile(source, str(BASE), "exec"), {"__name__": "__main__", "__file__": str(BASE)})
