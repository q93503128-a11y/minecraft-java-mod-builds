from pathlib import Path

root = Path('projects/survival-ascension')
java = root / 'src/main/java/kr/moonseungjun/survivalascension'

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

audit = root / 'tools/test_gameplay_qol_061.py'
s = audit.read_text(encoding='utf-8')
anchor = 'mining_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java")\n'
insert = anchor + 'expedition_action = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionAction.java")\nexpedition_progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")\n'
if s.count(anchor) != 1:
    raise SystemExit('QoL audit expedition read anchor drift')
s = s.replace(anchor, insert, 1)
anchor = 'need(ui, ["case FISHING", "낚싯대 마모 방지"], "fishing skill UI")\n'
checks = anchor + 'need(expedition_action, ["case FISHING -> null;"], "fishing expedition isolation")\nneed(expedition_progression, ["ExpeditionAction action = ExpeditionAction.fromSkill(skill);", "if (action != null) recordAction(player, action, amount);"], "nullable skill action guard")\n'
if s.count(anchor) != 1:
    raise SystemExit('QoL audit expedition check anchor drift')
audit.write_text(s.replace(anchor, checks, 1), encoding='utf-8')

print('FISHING EXPEDITION ISOLATION HOTFIX APPLIED')
