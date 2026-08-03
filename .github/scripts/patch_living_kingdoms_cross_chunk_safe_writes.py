from pathlib import Path


def replace_once_or_verify(text: str, old: str, new: str, label: str) -> str:
    old_count = text.count(old)
    new_count = text.count(new)
    if old_count == 1:
        return text.replace(old, new)
    if old_count == 0 and new_count == 1:
        return text
    raise SystemExit(f"{label}: old={old_count} new={new_count}")


plan_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java")
plan = plan_path.read_text(encoding="utf-8")
old_flags = "    private static final int CONSTRUCTION_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;"
new_flags = "Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS"
if old_flags in plan:
    plan = plan.replace(
        old_flags,
        "    private static final int CONSTRUCTION_UPDATE_FLAGS = " + new_flags + ";",
    )
elif new_flags not in plan:
    raise SystemExit("cross-chunk-safe construction flags are missing")
plan_path.write_text(plan, encoding="utf-8")

builder_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
builder = builder_path.read_text(encoding="utf-8")
builder = replace_once_or_verify(
    builder,
    "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal max_in_flight={}",
    "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal cross_chunk_neighbor_updates=false max_in_flight={}",
    "exterior marker",
)
builder_path.write_text(builder, encoding="utf-8")

workflow_path = Path(".github/workflows/audit-living-kingdoms-exterior-workforce.yml")
workflow = workflow_path.read_text(encoding="utf-8")
workflow = replace_once_or_verify(
    workflow,
    """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
          ! grep -R 'importWarehouseStock' \"$PROJECT_DIR/src/main/java\"
""",
    """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
          grep -F 'Block.UPDATE_KNOWN_SHAPE' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java\"
          ! grep -R 'importWarehouseStock' \"$PROJECT_DIR/src/main/java\"
""",
    "workforce source check",
)
workflow = replace_once_or_verify(
    workflow,
    "          grep -F 'max_in_flight=3' ../../logs/exterior-workforce-server.log\n",
    """          grep -F 'cross_chunk_neighbor_updates=false' ../../logs/exterior-workforce-server.log
          grep -F 'max_in_flight=3' ../../logs/exterior-workforce-server.log
""",
    "workforce runtime check",
)
workflow_path.write_text(workflow, encoding="utf-8")
