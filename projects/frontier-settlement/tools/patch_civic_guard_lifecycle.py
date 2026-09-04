from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
BENEFIT = JAVA / "SettlementBenefitService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

benefit = BENEFIT.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

benefit = replace_once(benefit,
'''                active.addTag(GUARD_POST_GUARD_TAG);
                active.addTag(assignment);
                active.setNoAi(false);
''',
'''                active.addTag(GUARD_POST_GUARD_TAG);
                active.addTag(assignment);
                active.setNoAi(false);
                active.setInvulnerable(false);
''', "guard post survivor normalization")

benefit = replace_once(benefit,
'''                for (int i = 1; i < existing.size(); i++) {
                    IronGolem duplicate = existing.get(i);
                    duplicate.addTag(GUARD_POST_GUARD_TAG);
                    duplicate.addTag(assignment);
                    duplicate.setTarget(null);
                    duplicate.getNavigation().stop();
                    duplicate.setNoAi(true);
                }
''',
'''                for (int i = 1; i < existing.size(); i++) {
                    removeDuplicateCivicGuard(existing.get(i));
                }
''', "guard post duplicate cleanup")

benefit = replace_once(benefit,
'''        IronGolem active = guards.getFirst();
        active.setNoAi(false);
        for (int i = 1; i < guards.size(); i++) {
            IronGolem duplicate = guards.get(i);
            duplicate.setTarget(null);
            duplicate.getNavigation().stop();
            duplicate.setNoAi(true);
        }
        return active;
''',
'''        IronGolem active = guards.getFirst();
        active.setNoAi(false);
        active.setInvulnerable(false);
        for (int i = 1; i < guards.size(); i++) {
            removeDuplicateCivicGuard(guards.get(i));
        }
        return active;
''', "watch guard duplicate cleanup")

benefit = replace_once(benefit,
'''    private static AABB watchGuardArea(BlockPos home) {
''',
'''    private static void removeDuplicateCivicGuard(IronGolem duplicate) {
        duplicate.setTarget(null);
        duplicate.getNavigation().stop();
        duplicate.setNoAi(false);
        duplicate.setInvulnerable(false);
        duplicate.discard();
    }

    private static AABB watchGuardArea(BlockPos home) {
''', "civic guard cleanup helper")

old_vars = '''barracks = text(JAVA / "settlement/SettlementBarracksService.java")
waterfront = text(JAVA / "settlement/SettlementWaterfrontService.java")
'''
new_vars = '''barracks = text(JAVA / "settlement/SettlementBarracksService.java")
benefit = text(JAVA / "settlement/SettlementBenefitService.java")
waterfront = text(JAVA / "settlement/SettlementWaterfrontService.java")
'''
audit = replace_once(audit, old_vars, new_vars, "audit civic guard var")

old_anchor = '''forbid(barracks, (
    "duplicate.setNoAi(true);",
    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"
), "legacy barracks duplicate containment")
'''
new_block = old_anchor + '''must(benefit, (
    "active.setInvulnerable(false);",
    "removeDuplicateCivicGuard(existing.get(i))",
    "removeDuplicateCivicGuard(guards.get(i))",
    "private static void removeDuplicateCivicGuard(IronGolem duplicate)",
    "duplicate.setNoAi(false);",
    "duplicate.setInvulnerable(false);",
    "duplicate.discard();"
), "civic guard lifecycle cleanup")
forbid(benefit, (
    "duplicate.setNoAi(true);",
), "legacy civic guard duplicate freeze")
'''
audit = replace_once(audit, old_anchor, new_block, "audit civic guard invariants")

BENEFIT.write_text(benefit, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for token in (
    "active.setInvulnerable(false);",
    "removeDuplicateCivicGuard(existing.get(i))",
    "removeDuplicateCivicGuard(guards.get(i))",
    "private static void removeDuplicateCivicGuard(IronGolem duplicate)",
    "duplicate.discard();",
):
    if token not in benefit:
        raise SystemExit(f"civic guard invariant missing: {token}")
if "duplicate.setNoAi(true);" in benefit:
    raise SystemExit("legacy civic guard duplicate freeze remains")
if '"civic guard lifecycle cleanup"' not in audit:
    raise SystemExit("civic guard persistent audit missing")

print("CIVIC GUARD LIFECYCLE PATCH PASS")
