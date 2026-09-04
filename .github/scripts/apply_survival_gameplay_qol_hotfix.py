from pathlib import Path

root = Path('projects/survival-ascension')
java = root / 'src/main/java/kr/moonseungjun/survivalascension'

# Fishing is a normal skill but is intentionally not reinterpreted as an existing expedition objective.
action = java / 'expedition/ExpeditionAction.java'
s = action.read_text(encoding='utf-8')
old = '''            case MINING -> BLOCKS_MINED;
            case COMBAT -> HOSTILES_KILLED;
'''
new = '''            case MINING -> BLOCKS_MINED;
            case COMBAT -> HOSTILES_KILLED;
            case FISHING -> null;
'''
if s.count(old) != 1:
    raise SystemExit('ExpeditionAction fishing switch anchor drift')
action.write_text(s.replace(old, new, 1), encoding='utf-8')

progression = java / 'expedition/ExpeditionProgression.java'
s = progression.read_text(encoding='utf-8')
old = '''    public static void recordSkillAction(ServerPlayer player, SkillType skill, int amount) {
        recordAction(player, ExpeditionAction.fromSkill(skill), amount);
    }
'''
new = '''    public static void recordSkillAction(ServerPlayer player, SkillType skill, int amount) {
        ExpeditionAction action = ExpeditionAction.fromSkill(skill);
        if (action != null) recordAction(player, action, amount);
    }
'''
if s.count(old) != 1:
    raise SystemExit('ExpeditionProgression nullable skill-action anchor drift')
progression.write_text(s.replace(old, new, 1), encoding='utf-8')

# Ore safety is unconditional: before vein mining unlock, an ore click stays one block;
# after unlock it expands only through the same ore family. Never plane-mine surrounding rock from an ore origin.
mining = java / 'mining/MiningProgression.java'
s = mining.read_text(encoding='utf-8')
old = '''                case AUTO -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    else if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }
                case PLANE -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    else if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }
'''
new = '''                case AUTO -> {
                    if (centerState.is(VALUABLE_ORES)) {
                        if (veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    } else if (areaSize > 1) {
                        breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                    }
                }
                case PLANE -> {
                    if (centerState.is(VALUABLE_ORES)) {
                        if (veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    } else if (areaSize > 1) {
                        breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                    }
                }
'''
if s.count(old) != 1:
    raise SystemExit('Mining ore-safe AUTO/PLANE anchor drift')
mining.write_text(s.replace(old, new, 1), encoding='utf-8')

mining_ui = java / 'client/MiningRadialMenuScreen.java'
s = mining_ui.read_text(encoding='utf-8')
s = s.replace('"광석=같은 종류 광맥 / 일반=굴착"', '"광석=동종만 / 일반=굴착"')
s = s.replace('"Lv.10 · 일반=평면 / 광석=같은 종류 광맥 보호"', '"Lv.10 · 일반=평면 / 광석=동종만"')
mining_ui.write_text(s, encoding='utf-8')

# Seven skill rows must still fit common GUI-scale heights without overlapping the Done button.
skills_ui = java / 'client/SkillsScreen.java'
s = skills_ui.read_text(encoding='utf-8')
s = s.replace('private static final int ROW_HEIGHT = 27;', 'private static final int ROW_HEIGHT = 24;', 1)
s = s.replace('private static final int LIST_TOP = 44;', 'private static final int LIST_TOP = 38;', 1)
s = s.replace('int barLeft = left + 9, barRight = left + ROW_WIDTH - 9, barTop = top + 23;', 'int barLeft = left + 9, barRight = left + ROW_WIDTH - 9, barTop = top + 20;', 1)
skills_ui.write_text(s, encoding='utf-8')

# Guide's exhaustive skill switch also exposes the fishing perk in the normal stats page.
guide = java / 'client/GuideScreen.java'
s = guide.read_text(encoding='utf-8')
old = '''            case HARVESTING -> SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " 수확";
            case COMBAT -> String.format(Locale.ROOT, "피해 %.2f× / 파급 %d체", SkillTuning.combatDamageMultiplier(level), SkillTuning.combatCleaveTargetLimit(level));
'''
new = '''            case HARVESTING -> SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " 수확";
            case FISHING -> "낚싯대 마모 방지 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D) + "%";
            case COMBAT -> String.format(Locale.ROOT, "피해 %.2f× / 파급 %d체", SkillTuning.combatDamageMultiplier(level), SkillTuning.combatCleaveTargetLimit(level));
'''
if s.count(old) != 1:
    raise SystemExit('GuideScreen fishing effect anchor drift')
guide.write_text(s.replace(old, new, 1), encoding='utf-8')

# Command/status output must remain exhaustive after adding the skill.
commands = java / 'command/AscensionCommands.java'
s = commands.read_text(encoding='utf-8')
old = '''            case HARVESTING -> "범위 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level)
                    + " | 속도 " + fmt(SkillTuning.harvestingSpeedMultiplier(level));
            case COMBAT -> "피해 " + fmt(SkillTuning.combatDamageMultiplier(level))
'''
new = '''            case HARVESTING -> "범위 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level)
                    + " | 속도 " + fmt(SkillTuning.harvestingSpeedMultiplier(level));
            case FISHING -> "낚싯대 마모 방지 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D) + "%";
            case COMBAT -> "피해 " + fmt(SkillTuning.combatDamageMultiplier(level))
'''
if s.count(old) != 1:
    raise SystemExit('AscensionCommands fishing status anchor drift')
commands.write_text(s.replace(old, new, 1), encoding='utf-8')

audit = root / 'tools/test_gameplay_qol_061.py'
s = audit.read_text(encoding='utf-8')
anchor = 'mining_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java")\n'
insert = anchor + 'expedition_action = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java")\nexpedition_progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")\nguide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")\ncommands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")\n'
if s.count(anchor) != 1:
    raise SystemExit('QoL audit expedition read anchor drift')
s = s.replace(anchor, insert, 1)
anchor = 'need(ui, ["case FISHING", "낚싯대 마모 방지"], "fishing skill UI")\n'
checks = anchor + '''need(expedition_action, ["case FISHING -> null;"], "fishing expedition isolation")
need(expedition_progression, ["ExpeditionAction action = ExpeditionAction.fromSkill(skill);", "if (action != null) recordAction(player, action, amount);"], "nullable skill action guard")
need(mining, ["if (centerState.is(VALUABLE_ORES)) {", "if (veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);"], "unconditional ore-origin protection")
need(mining_ui, ["광석=동종만 / 일반=굴착", "광석=동종만"], "ore-safe mining UI")
need(ui, ["ROW_HEIGHT = 24", "LIST_TOP = 38", "barTop = top + 20"], "seven-skill compact layout")
need(guide, ["case FISHING", "fishingRodPreservationChance"], "fishing guide stats")
need(commands, ["case FISHING", "낚싯대 마모 방지"], "fishing command status")
'''
if s.count(anchor) != 1:
    raise SystemExit('QoL audit expedition check anchor drift')
s = s.replace('need(mining_ui, ["광석=같은 종류 광맥", "광석=같은 종류 광맥 보호"], "ore-safe mining UI")\n', '')
audit.write_text(s.replace(anchor, checks, 1), encoding='utf-8')

print('FISHING COMMAND/UI + EXPEDITION + STRICT ORE-SAFETY HOTFIX APPLIED')
