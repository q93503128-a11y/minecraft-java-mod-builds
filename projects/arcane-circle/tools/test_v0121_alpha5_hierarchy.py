#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def need(source: str, tokens: tuple[str, ...], label: str) -> None:
    missing = [token for token in tokens if token not in source]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")

properties = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
damage = read("src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneDamage.java")
tradition = read("src/main/java/kr/moonseungjun/arcanecircle/world/MagicTradition.java")
society = read("src/main/java/kr/moonseungjun/arcanecircle/world/MageSociety.java")
mages = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java")
screen = read("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
economy = read("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java")
canon = read("docs/MAGIC_WORLD_CANON.md")
index = json.loads(read("src/main/resources/data/arcanecircle/spell_catalog/index.json"))

need(properties, ("mod_version=0.12.1-alpha.5",), "version")
need(main, ('VERSION = "0.12.1-alpha.5"',), "lifecycle version")
need(casting, (
    "QUEUE_TIMEOUT_TICKS = 2400L", "CHARGE_TIMEOUT_TICKS = 1600L",
    "sameCircleTicks", "620", "minimumTicks", "360", "Math.pow(0.78",
    "requiredFusionCastTicks", "case 8 -> 620", "default -> 960"
), "nonlinear cast hierarchy")
need(damage, ("playerAttack(caster)", "mobAttack(caster)", "mob.setTarget(caster)"), "attributed spell damage")
need(tradition, ("왕국 마도연맹", "백은 성약", "녹월 결사", "재의 밀약", "social factions"), "affiliations")
need(society, ("ALLIED", "FRIENDLY", "NEUTRAL", "HOSTILE", "avoidsAutoTarget"), "diplomacy")
need(mages, (
    "CIRCLE_WEIGHTS", "59_000", "25_000", "10_000", "4_000", "1_500", "400", "80", "20",
    "weightedCircle", "MageProfile", "MageSociety.Role", "residentAffiliation", "naturalAffiliation",
    "castResidentSpell", "castHostileSpell", "ArcaneDamage.hurt", "setCustomNameVisible(true)"
), "mage population and roles")
if "weightedCircle(mob.getUUID(), 9)" in mages or "Math.min(9" in mages:
    raise SystemExit("random natural 9-circle mage generation remains")
need(screen, (
    'new Tab("academy", "마도회")', "fusionCircle", "clickRecipes", "융합 써클",
    "fusionFormulasInCircle", '"소속 "'
), "circle-filtered fusion and affiliation UI")
need(economy, ("Social affiliation does not discount", "[소속 등록]"), "neutral affiliation economy")
need(canon, ("6 | 대마법사", "9 | 세계급", "자연 생성 금지", "재의 밀약", "연속 선 메시"), "world canon")

for path in (JAVA / "magic").glob("*.java"):
    if path.name == "ArcaneDamage.java":
        continue
    if "damageSources().magic()" in path.read_text(encoding="utf-8"):
        raise SystemExit(f"unattributed player spell damage remains: {path.name}")

if index.get("version") != "0.12.1-alpha.5":
    raise SystemExit("alpha.5 index version missing")
if index.get("spell_damage_attribution") != "caster_bound":
    raise SystemExit("caster-bound damage metadata missing")
if index.get("fusion_ui") != "circle_hierarchy":
    raise SystemExit("fusion circle hierarchy metadata missing")
if index.get("natural_circle_weights", {}).get("9") != 0:
    raise SystemExit("natural ninth-circle weight must be zero")

print("Arcane Circle v0.12.1-alpha.5 hierarchy, affiliation and aggro contract: PASS")
