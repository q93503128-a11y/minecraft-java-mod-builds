from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/entity"
FILES = [
    BASE / "SilverHartEntity.java",
    BASE / "AshHoundEntity.java",
    BASE / "RiverWispEntity.java",
]


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


for path in FILES:
    text = path.read_text(encoding="utf-8")
    if "removeWhenFarAway(double distanceToClosestPlayer)" not in text:
        anchor = "\n}\n"
        require(text.endswith(anchor), f"entity class closing anchor missing: {path.name}")
        method = '''
    /** These are player-local ecology instances, not permanent settlement actors. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }
'''
        text = text[:-len(anchor)] + method + anchor
        path.write_text(text, encoding="utf-8")

for path in FILES:
    current = path.read_text(encoding="utf-8")
    require("removeWhenFarAway(double distanceToClosestPlayer)" in current,
            f"missing bounded ecology lifecycle override in {path.name}")
    require("return true;" in current, f"ecology despawn policy not enabled in {path.name}")

print("Prepared bounded player-local lifecycle for all three fantasy ecology species.")
