from pathlib import Path

plan_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java")
plan = plan_path.read_text(encoding="utf-8")
old_flags = "    private static final int CONSTRUCTION_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;"
new_flags = "    private static final int CONSTRUCTION_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;"
if plan.count(old_flags) != 1:
    raise SystemExit(f"construction flag pattern count={plan.count(old_flags)}")
plan = plan.replace(old_flags, new_flags)
plan_path.write_text(plan, encoding="utf-8")

builder_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
builder = builder_path.read_text(encoding="utf-8")
old_marker = "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal max_in_flight={}"
new_marker = "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal cross_chunk_neighbor_updates=false max_in_flight={}"
if builder.count(old_marker) != 1:
    raise SystemExit(f"exterior marker pattern count={builder.count(old_marker)}")
builder = builder.replace(old_marker, new_marker)
builder_path.write_text(builder, encoding="utf-8")

workflow_path = Path(".github/workflows/audit-living-kingdoms-exterior-workforce.yml")
workflow = workflow_path.read_text(encoding="utf-8")
old_source_check = """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
          ! grep -R 'importWarehouseStock' \"$PROJECT_DIR/src/main/java\"
"""
new_source_check = """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
          grep -F 'Block.UPDATE_KNOWN_SHAPE' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/IncrementalWorldEditPlan.java\"
          ! grep -R 'importWarehouseStock' \"$PROJECT_DIR/src/main/java\"
"""
if workflow.count(old_source_check) != 1:
    raise SystemExit("workforce source-check pattern missing")
workflow = workflow.replace(old_source_check, new_source_check)
old_runtime_check = "          grep -F 'max_in_flight=3' ../../logs/exterior-workforce-server.log\n"
new_runtime_check = """          grep -F 'cross_chunk_neighbor_updates=false' ../../logs/exterior-workforce-server.log
          grep -F 'max_in_flight=3' ../../logs/exterior-workforce-server.log
"""
if workflow.count(old_runtime_check) != 1:
    raise SystemExit("workforce runtime-check pattern missing")
workflow = workflow.replace(old_runtime_check, new_runtime_check)
workflow_path.write_text(workflow, encoding="utf-8")
