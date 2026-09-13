from pathlib import Path
import re

ROOT = Path('projects/survival-ascension')
VERSION_OLD = '0.61.21-alpha.1'
VERSION_NEW = '0.61.22-alpha.1'


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')


def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {text.count(old)}')
    return text.replace(old, new, 1)

# Fishing is fundamentally one validated action per successful reel-in. Unlike mining/woodcutting/
# harvesting, it never gains area/chain action counts as mastery rises, so the old generic early->late
# taper made the exploding shared XP curve dominate late play. Give fishing its own action-rate curve.
path = 'src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java'
text = read(path)
pattern = re.compile(r'    public static double skillXpMultiplier\(SkillType skill, int currentLevel\) \{.*?\n    public static long scaleSkillXp', re.S)
match = pattern.search(text)
if not match:
    raise SystemExit('SkillTuning.skillXpMultiplier block not found')
replacement = '''    public static double skillXpMultiplier(SkillType skill, int currentLevel) {
        int level = clamp(currentLevel);
        if (skill == SkillType.FISHING) return fishingXpMultiplier(level);

        double early;
        double late;
        switch (skill) {
            // Mining already earns XP from very high real block counts and keeps its proven pacing.
            case MINING -> { early = 1.25D; late = 1.10D; }
            // Action-scarce skills are normalized against actual survival play time. Lv90 is a major
            // infrastructure/mastery threshold, so these must not require thousands of repetitive actions.
            case WOODCUTTING -> { early = 2.50D; late = 2.00D; }
            case HARVESTING -> { early = 3.00D; late = 2.50D; }
            case COMBAT -> { early = 4.00D; late = 3.50D; }
            case CONSTRUCTION -> { early = 5.00D; late = 3.50D; }
            case MOBILITY -> { early = 4.00D; late = 3.00D; }
            default -> { early = 1.0D; late = 1.0D; }
        }
        double progress = Math.min(1.0D, level / 60.0D);
        return early + (late - early) * progress;
    }

    /**
     * Fishing remains one reel-in per action even at high mastery, so it cannot rely on the area/chain
     * action scaling that naturally accelerates Mining, Woodcutting and Harvesting. Keep the opening
     * readable, then raise XP density with the shared XP curve so late Fishing stays measured in tens
     * of catches per level rather than hundreds. Anchors: Lv0 8x, Lv10 10x, Lv30 16x, Lv60 32x,
     * Lv90 50x, Lv100 60x. Angler Harbor's existing 1.25x multiplier still stacks afterward.
     */
    private static double fishingXpMultiplier(int level) {
        int clamped = clamp(level);
        if (clamped < 10) return 8.0D + 2.0D * clamped / 10.0D;
        if (clamped < 30) return 10.0D + 6.0D * (clamped - 10) / 20.0D;
        if (clamped < 60) return 16.0D + 16.0D * (clamped - 30) / 30.0D;
        if (clamped < 90) return 32.0D + 18.0D * (clamped - 60) / 30.0D;
        return 50.0D + 10.0D * (clamped - 90) / 10.0D;
    }

    public static long scaleSkillXp'''
text = text[:match.start()] + replacement + text[match.end():]
write(path, text)

# Runtime/version identity.
path = 'gradle.properties'
text = read(path)
text = replace_once(text, f'mod_version={VERSION_OLD}', f'mod_version={VERSION_NEW}', 'gradle version')
text = text.replace('# 0.61.21 final release validation trigger (2026-09-07).', '# 0.61.22 fishing mastery pacing rebalance (2026-09-13).')
write(path, text)

path = 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
text = read(path)
text = replace_once(text, f'public static final String VERSION = "{VERSION_OLD}";', f'public static final String VERSION = "{VERSION_NEW}";', 'source version')
write(path, text)

# Current-source regression contract follows the new dedicated Fishing curve.
path = 'tools/test_current_source.py'
text = read(path)
text = text.replace(f'mod_version={VERSION_OLD}', f'mod_version={VERSION_NEW}')
text = text.replace(f'VERSION = "{VERSION_OLD}"', f'VERSION = "{VERSION_NEW}"')
text = replace_once(text,
    '    "case FISHING -> { early = 6.00D; late = 5.00D; }",\n',
    '    "if (skill == SkillType.FISHING) return fishingXpMultiplier(level);",\n',
    'fishing pacing verifier')
needle = 'for threshold in (\n'
insert = '''require("if (clamped < 10) return 8.0D + 2.0D * clamped / 10.0D;" in tuning\n        and "if (clamped < 30) return 10.0D + 6.0D * (clamped - 10) / 20.0D;" in tuning\n        and "if (clamped < 60) return 16.0D + 16.0D * (clamped - 30) / 30.0D;" in tuning\n        and "if (clamped < 90) return 32.0D + 18.0D * (clamped - 60) / 30.0D;" in tuning\n        and "return 50.0D + 10.0D * (clamped - 90) / 10.0D;" in tuning,\n        "Fishing action-rate normalization curve drift")\n\n'''
text = replace_once(text, needle, insert + needle, 'fishing curve regression insertion')
write(path, text)

path = 'tools/test_release_source.py'
text = read(path)
text = replace_once(text, f'CURRENT_VERSION = "{VERSION_OLD}"', f'CURRENT_VERSION = "{VERSION_NEW}"', 'release current version')
text = replace_once(text, f'PREVIOUS_DOC_VERSION = "{VERSION_OLD}"', f'PREVIOUS_DOC_VERSION = "{VERSION_NEW}"', 'release doc version')
write(path, text)

project_section = '''## 0.61.22 Fishing Mastery Pacing / 낚시 숙련 속도 재조정
- Fishing remains one validated action per successful reel-in, so it no longer shares the generic early-to-late XP taper used by skills that gain area/chain action counts.
- Fishing mastery XP scaling now grows with level: Lv0 8x, Lv10 10x, Lv30 16x, Lv60 32x, Lv90 50x, Lv100 60x. Interpolation is continuous between milestones.
- A normal one-fish catch still uses the existing raw catch value; only the Fishing normalization factor changes. The existing Angler Harbor +25% Fishing XP remains multiplicative on the raw catch before mastery scaling.
- Approximate plain single-fish pacing falls from roughly 3,400 successful catches for Lv100 to about 480 before Harbor/treasure variation, with Lv30 around 50 catches and Lv60 around 180. This is intentionally a feel correction for a time-gated one-action skill, not a global mastery-curve reduction.
- No SavedData field, event authority, fishing-hook timing, bonus-yield rule, rod-preservation rule or network protocol changes. Existing Fishing XP carries forward unchanged.

'''

path = 'PROJECT.md'
text = read(path)
text = replace_once(text, f'- Mod version: `{VERSION_OLD}`', f'- Mod version: `{VERSION_NEW}`', 'PROJECT version')
old_compat = '- Existing-world compatibility: 0.61.21 adds no SavedData ID or codec field and does not bump the network protocol. Existing skill XP, infrastructure/logistics/outpost/production data and affix CustomData remain compatible.'
new_compat = '- Existing-world compatibility: 0.61.22 changes only Fishing mastery XP pacing and does not add a SavedData ID/codec field or bump the network protocol. Existing skill XP, deterministic fishing meters, infrastructure/logistics/outpost/production data and equipment CustomData remain compatible.'
text = replace_once(text, old_compat, new_compat, 'PROJECT compatibility')
text = replace_once(text, '## 0.61.21 Bounded Bulk Mining / 대량 채굴 틱 분산\n', project_section + '## 0.61.21 Bounded Bulk Mining / 대량 채굴 틱 분산\n', 'PROJECT section')
write(path, text)

path = 'README.md'
text = read(path)
readme_section = '''## 0.61.22-alpha.1 — Fishing Mastery Pacing / 낚시 숙련 속도 재조정
낚시는 숙련도가 올라도 한 번의 성공적인 릴인에서 얻는 유효 행동 수가 1회인 반면, 기존 공용 숙련 곡선은 후반 요구 XP가 크게 오르고 낚시 보정은 오히려 6x에서 5x로 낮아졌다. 채굴·벌목·수확처럼 숙련 상승으로 면적/연쇄 행동 수가 늘어나는 기술과 같은 후반 보정 구조를 쓰던 것이 실제 플레이 감각과 맞지 않았다.

낚시만 행동 빈도에 맞는 전용 XP 보정을 사용한다: Lv0 8x → Lv10 10x → Lv30 16x → Lv60 32x → Lv90 50x → Lv100 60x. 단계 사이는 연속 보간된다. 일반 물고기 1회 어획 기준 Lv100까지 필요한 성공 어획은 대략 3,400회에서 약 480회 수준으로 줄며, 기존 어업 부두의 +25% 낚시 XP는 그대로 추가 적용된다. 저장 데이터·낚시찌 타이밍·추가 어획·낚싯대 보존·네트워크 프로토콜은 바뀌지 않는다.

'''
text = replace_once(text, '## 0.61.21-alpha.1 — Bounded Bulk Mining / 대량 채굴 틱 분산\n', readme_section + '## 0.61.21-alpha.1 — Bounded Bulk Mining / 대량 채굴 틱 분산\n', 'README section')
write(path, text)

path = 'CHANGELOG.md'
text = read(path)
change = '''## 0.61.22-alpha.1
- Rebalanced Fishing mastery around its real action rate instead of the generic late-game taper. Fishing XP normalization now rises continuously through Lv0/10/30/60/90/100 anchors of 8x/10x/16x/32x/50x/60x.
- A normal one-fish catch now reaches Lv30 in roughly 50 successful catches, Lv60 around 180 and Lv100 around 480 before Angler Harbor or treasure variation, instead of roughly 3,400 catches to Lv100 under the previous 6x -> 5x taper.
- Existing Fishing XP and deterministic bonus/preservation meters are untouched; Angler Harbor keeps its +25% XP multiplier. No fishing-hook timing, SavedData schema, packet or network protocol changes. Protocol remains 15.

'''
text = replace_once(text, '# Changelog\n\n', '# Changelog\n\n' + change, 'CHANGELOG section')
write(path, text)

print('Survival Ascension 0.61.22 Fishing pacing patch applied.')
