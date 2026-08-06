from pathlib import Path

# Triggered after the workflow is present on main.
builder_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
builder = builder_path.read_text(encoding="utf-8")

if "ciTicketsIssued" not in builder:
    builder = builder.replace(
        "    private static boolean ciRequested;\n    private static boolean ciPassed;\n",
        "    private static boolean ciRequested;\n    private static boolean ciPassed;\n    private static long ciTicketsIssued;\n    private static long ciTicketsReleased;\n",
    )

if "ciTicketsIssued = 0L;" not in builder:
    builder = builder.replace(
        "        ciRequested = false;\n        ciPassed = false;\n",
        "        ciRequested = false;\n        ciPassed = false;\n        ciTicketsIssued = 0L;\n        ciTicketsReleased = 0L;\n",
    )

old_request = "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal max_in_flight={}"
new_request = "synchronous_get_chunk=false forced_chunks=false transient_ticket=portal explicit_ticket_release=true max_in_flight={}"
if old_request in builder:
    builder = builder.replace(old_request, new_request)
elif new_request not in builder:
    raise SystemExit("exterior request marker pattern missing")

old_issue = """            if (RETAINED.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
"""
new_issue = """            if (RETAINED.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
                ciTicketsIssued++;
            }
"""
if old_issue in builder:
    builder = builder.replace(old_issue, new_issue)
elif new_issue not in builder:
    raise SystemExit("ticket issue pattern missing")

old_verify = """        if (data.completedNodeCount(EXTERIOR_REVISION) != ErdenKingdomSupplyCatalog.nodes().size()
                || data.builtChunkCount(EXTERIOR_REVISION) < 70
                || data.totalWrites(EXTERIOR_REVISION) <= 0L) return;
"""
new_verify = """        if (data.completedNodeCount(EXTERIOR_REVISION) != ErdenKingdomSupplyCatalog.nodes().size()
                || data.builtChunkCount(EXTERIOR_REVISION) < 70
                || data.totalWrites(EXTERIOR_REVISION) <= 0L
                || !CI_REQUESTS.isEmpty()
                || !CI_LOADING.isEmpty()
                || !PENDING.isEmpty()
                || active != null
                || !RETAINED.isEmpty()
                || ciTicketsIssued != ciTicketsReleased) return;
"""
if old_verify in builder:
    builder = builder.replace(old_verify, new_verify)
elif new_verify not in builder:
    raise SystemExit("CI completion guard pattern missing")

old_marker = """                \"LK_ERDEN_KINGDOM_EXTERIOR_PASS revision={} nodes={} producers={} wharves={} anchor_chunks={} writes={} metre_scale=true streamed=true external_buildings=true fields=true paddocks=true mines=true mills=true docks=true roads=true storage_yards=true debris_zero=true\",
                EXTERIOR_REVISION, ErdenKingdomSupplyCatalog.nodes().size(),
                ErdenKingdomSupplyCatalog.producerCount(), ErdenKingdomSupplyCatalog.wharfCount(),
                data.builtChunkCount(EXTERIOR_REVISION), data.totalWrites(EXTERIOR_REVISION));
"""
new_marker = """                \"LK_ERDEN_KINGDOM_EXTERIOR_PASS revision={} nodes={} producers={} wharves={} anchor_chunks={} writes={} transient_tickets_issued={} transient_tickets_released={} tickets_released=true metre_scale=true streamed=true external_buildings=true fields=true paddocks=true mines=true mills=true docks=true roads=true storage_yards=true debris_zero=true\",
                EXTERIOR_REVISION, ErdenKingdomSupplyCatalog.nodes().size(),
                ErdenKingdomSupplyCatalog.producerCount(), ErdenKingdomSupplyCatalog.wharfCount(),
                data.builtChunkCount(EXTERIOR_REVISION), data.totalWrites(EXTERIOR_REVISION),
                ciTicketsIssued, ciTicketsReleased);
"""
if old_marker in builder:
    builder = builder.replace(old_marker, new_marker)
elif new_marker not in builder:
    raise SystemExit("exterior pass marker pattern missing")

old_release = """    private static void release(ServerLevel level, long packed) {
        CI_LOADING.remove(packed);
        RETAINED.remove(packed);
    }
"""
new_release = """    private static void release(ServerLevel level, long packed) {
        CI_LOADING.remove(packed);
        if (!RETAINED.remove(packed)) return;
        level.getChunkSource().removeTicketWithRadius(
                TicketType.PORTAL,
                new ChunkPos(unpackX(packed), unpackZ(packed)),
                0);
        ciTicketsReleased++;
    }
"""
if old_release in builder:
    builder = builder.replace(old_release, new_release)
elif new_release not in builder:
    raise SystemExit("ticket release pattern missing")

builder_path.write_text(builder, encoding="utf-8")

workflow_path = Path(".github/workflows/audit-living-kingdoms-exterior-workforce.yml")
workflow = workflow_path.read_text(encoding="utf-8")
if "removeTicketWithRadius" not in workflow:
    needle = """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
"""
    replacement = needle + """          grep -F 'removeTicketWithRadius' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
"""
    if needle not in workflow:
        raise SystemExit("workforce source invariant insertion point missing")
    workflow = workflow.replace(needle, replacement, 1)
if "grep -F 'tickets_released=true'" not in workflow:
    needle = "          grep -F 'max_in_flight=3' ../../logs/exterior-workforce-server.log\n"
    if needle not in workflow:
        raise SystemExit("workforce runtime invariant insertion point missing")
    workflow = workflow.replace(
        needle,
        "          grep -F 'tickets_released=true' ../../logs/exterior-workforce-server.log\n" + needle,
        1,
    )
workflow_path.write_text(workflow, encoding="utf-8")
