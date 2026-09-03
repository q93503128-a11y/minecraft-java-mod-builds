from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"
ADVANCED = JAVA / "settlement/SettlementAdvancedWorkshopService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

advanced = ADVANCED.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

# Advanced workshop has two independent datapack-facing cost domains: metal and expedition relics.
# An external item may legally be tagged into both unless we fail closed here. If an overlapping
# stack satisfies both prechecks, sequential mutation can consume metal first and then fail the relic
# consume. Make the cost predicates disjoint before any count/extract/consume operation.
advanced = advanced.replace(
    "SettlementStorageService::isMetalStack",
    "SettlementAdvancedWorkshopService::isForgeMetal",
)
advanced = advanced.replace(
    "SettlementStorageService.isMetalStack(carried)",
    "isForgeMetal(carried)",
)

helper_anchor = '''    private static int findRelicSlot(Container crate) {\n'''
helper = '''    /**\n     * Forge metal and expedition relics are intentionally disjoint cost domains. A companion\n     * datapack may accidentally tag the same item as both; treating that stack as a relic only\n     * prevents one sequential consume from invalidating the next after resources already changed.\n     */\n    private static boolean isForgeMetal(ItemStack stack) {\n        return !stack.isEmpty()\n                && SettlementStorageService.isMetalStack(stack)\n                && !stack.is(ExternalContentTags.EXPEDITION_RELICS);\n    }\n\n'''
if "private static boolean isForgeMetal(ItemStack stack)" not in advanced:
    if advanced.count(helper_anchor) != 1:
        raise SystemExit(f"advanced helper anchor count={advanced.count(helper_anchor)}")
    advanced = advanced.replace(helper_anchor, helper + helper_anchor, 1)

# Persistent source audit. Insert variables/audit blocks in positions that remain stable even if
# another hardening workflow has already extended Alpha.91's source audit.
advanced_var = 'advanced_workshop = text(JAVA / "settlement/SettlementAdvancedWorkshopService.java")\n'
if advanced_var not in audit:
    anchor = 'entity = text(JAVA / "content/FrontierWorkerEntity.java")\n'
    if audit.count(anchor) != 1:
        raise SystemExit(f"advanced audit variable anchor count={audit.count(anchor)}")
    audit = audit.replace(anchor, advanced_var + anchor, 1)

advanced_audit = '''must(advanced_workshop, (\n    "private static boolean isForgeMetal(ItemStack stack)",\n    "SettlementStorageService.isMetalStack(stack)",\n    "!stack.is(ExternalContentTags.EXPEDITION_RELICS)",\n    "SettlementAdvancedWorkshopService::isForgeMetal"\n), "advanced workshop disjoint costs")\nforbid(advanced_workshop, (\n    "SettlementStorageService::isMetalStack",\n    "SettlementStorageService.isMetalStack(carried)"\n), "advanced workshop overlapping metal predicate")\n'''
if '"advanced workshop disjoint costs"' not in audit:
    anchor = 'must(service, (\n'
    if audit.count(anchor) != 1:
        raise SystemExit(f"advanced audit block anchor count={audit.count(anchor)}")
    audit = audit.replace(anchor, advanced_audit + anchor, 1)

ADVANCED.write_text(advanced, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

final_advanced = ADVANCED.read_text(encoding="utf-8")
final_audit = AUDIT.read_text(encoding="utf-8")
required = (
    "private static boolean isForgeMetal(ItemStack stack)",
    "SettlementStorageService.isMetalStack(stack)",
    "!stack.is(ExternalContentTags.EXPEDITION_RELICS)",
    "SettlementAdvancedWorkshopService::isForgeMetal",
)
for token in required:
    if token not in final_advanced:
        raise SystemExit(f"advanced cost invariant missing: {token}")
for forbidden in (
    "SettlementStorageService::isMetalStack",
    "SettlementStorageService.isMetalStack(carried)",
):
    if forbidden in final_advanced:
        raise SystemExit(f"advanced overlapping cost predicate remains: {forbidden}")
if "advanced workshop disjoint costs" not in final_audit:
    raise SystemExit("persistent advanced workshop cost audit missing")

print("ADVANCED WORKSHOP DISJOINT COST PATCH PASS")
