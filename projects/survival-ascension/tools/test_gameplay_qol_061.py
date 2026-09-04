#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def need(text, needles, label):
    for n in needles:
        if n not in text:
            errors.append(f"{label} missing: {n}")

skill = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillType.java")
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
helper = read("src/main/java/kr/moonseungjun/survivalascension/progress/AutomatedToolBreak.java")
mining = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
fishing = read("src/main/java/kr/moonseungjun/survivalascension/fishing/FishingProgression.java")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
ui = read("src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java")
mining_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java")
expedition_action = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java")
expedition_progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")

need(skill, ['FISHING("fishing", "낚시"'], "fishing skill enum")
need(tuning, ["if (level < 30) return 430L + 15L * (level - 20);", "fishingRodPreservationChance", "case FISHING"], "post20 XP/fishing tuning")
need(helper, ["AUTOMATIC_BLOCKS_PER_WEAR = 4", "destroyWithReducedWear", "player.gameMode.destroyBlock(target)"], "reduced bulk wear")
if mining.count("AutomatedToolBreak.destroyWithReducedWear") != 4:
    errors.append("mining automatic break paths != 4")
if bore.count("AutomatedToolBreak.destroyWithReducedWear") != 1:
    errors.append("bore automatic break path != 1")
if wood.count("AutomatedToolBreak.destroyWithReducedWear") != 1:
    errors.append("wood automatic break path != 1")
need(mining, ["case PLANE -> {", "centerState.is(VALUABLE_ORES) && veinLimit > 1", "breakConnectedOre(player, level, center, centerState, veinLimit)"], "ore-safe plane mining")
need(fishing, ["ItemFishedEvent", "SkillType.FISHING", "xpForCatch", "damageRodBy", "fishingRodPreservationChance"], "fishing runtime")
need(main, ["FishingProgression::onItemFished"], "fishing event wiring")
need(ui, ["case FISHING", "낚싯대 마모 방지"], "fishing skill UI")
need(expedition_action, ["case FISHING -> null;"], "fishing expedition isolation")
need(expedition_progression, ["ExpeditionAction action = ExpeditionAction.fromSkill(skill);", "if (action != null) recordAction(player, action, amount);"], "nullable skill action guard")
need(mining, ["if (centerState.is(VALUABLE_ORES)) {", "if (veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);"], "unconditional ore-origin protection")
need(mining_ui, ["광석=동종만 / 일반=굴착", "광석=동종만"], "ore-safe mining UI")
need(ui, ["ROW_HEIGHT = 24", "LIST_TOP = 38", "barTop = top + 20"], "seven-skill compact layout")
need(guide, ["case FISHING", "fishingRodPreservationChance"], "fishing guide stats")
need(commands, ["case FISHING", "낚싯대 마모 방지"], "fishing command status")
if "destroyWithoutAdditionalWear" in helper + mining + bore + wood:
    errors.append("obsolete zero-wear bulk helper remains")

if errors:
    print("GAMEPLAY QOL AUDIT FAIL")
    for e in errors:
        print("-", e)
    raise SystemExit(1)
print("GAMEPLAY QOL AUDIT PASS")
print("post20_xp_curve=RETUNED")
print("ore_family_protection=AUTO_AND_PLANE")
print("bulk_tool_wear=ONE_VANILLA_ROLL_PER_4_AUTOMATIC_BLOCKS")
print("fishing_skill=WIRED")
