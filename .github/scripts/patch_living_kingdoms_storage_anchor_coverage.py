from pathlib import Path

builder_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
builder = builder_path.read_text(encoding="utf-8")

old = "    private static final int CI_MAX_IN_FLIGHT = 3;\n"
new = "    private static final int CI_MAX_IN_FLIGHT = 3;\n    public static final int EXPECTED_CI_ANCHORS = 104;\n"
if "EXPECTED_CI_ANCHORS" not in builder:
    if builder.count(old) != 1:
        raise SystemExit("CI_MAX_IN_FLIGHT insertion point missing")
    builder = builder.replace(old, new)

old = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
        }
"""
new = """        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                unique.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
            unique.add(storageAnchorChunk(node));
        }
        if (unique.size() != EXPECTED_CI_ANCHORS) {
            throw new IllegalStateException("Invalid Erden exterior CI anchor count " + unique.size());
        }
"""
if "Invalid Erden exterior CI anchor count" not in builder:
    if builder.count(old) != 1:
        raise SystemExit("prepareCiAnchors pattern missing")
    builder = builder.replace(old, new)

old = """            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                long packed = pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4);
                if (!data.isBuilt(packed, EXTERIOR_REVISION)) {
                    complete = false;
                    break;
                }
            }
            if (complete) data.markNode(node.id, EXTERIOR_REVISION);
"""
new = """            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                long packed = pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4);
                if (!data.isBuilt(packed, EXTERIOR_REVISION)) {
                    complete = false;
                    break;
                }
            }
            if (complete && !data.isBuilt(storageAnchorChunk(node), EXTERIOR_REVISION)) {
                complete = false;
            }
            if (complete) data.markNode(node.id, EXTERIOR_REVISION);
"""
if "complete && !data.isBuilt(storageAnchorChunk(node)" not in builder:
    if builder.count(old) != 1:
        raise SystemExit("markCompletedNodeAnchors pattern missing")
    builder = builder.replace(old, new)

needle = """    public static BlockPos storagePosition(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode node) {
"""
helper = """    public static long storageAnchorChunk(ErdenKingdomSupplyCatalog.SupplyNode node) {
        int offsetX = switch (node.facingQuarterTurns) {
            case 1 -> 18;
            case 3 -> -18;
            default -> 0;
        };
        int offsetZ = switch (node.facingQuarterTurns) {
            case 0 -> 18;
            case 2 -> -18;
            default -> 0;
        };
        return pack((node.x + offsetX) >> 4, (node.z + offsetZ) >> 4);
    }

""" + needle
if "public static long storageAnchorChunk" not in builder:
    if builder.count(needle) != 1:
        raise SystemExit("storagePosition insertion point missing")
    builder = builder.replace(needle, helper)

builder_path.write_text(builder, encoding="utf-8")

reaper_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorTicketReaper.java")
reaper = reaper_path.read_text(encoding="utf-8")
old = """    private static Set<Long> requiredAnchors() {
        Set<Long> anchors = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        return anchors;
    }

    private static Set<Long> anchorsFor(ErdenKingdomSupplyCatalog.SupplyNode node) {
        Set<Long> anchors = new LinkedHashSet<>();
        for (int[] offset : NODE_ANCHOR_OFFSETS) {
            anchors.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
        }
        return anchors;
    }
"""
new = """    private static Set<Long> requiredAnchors() {
        Set<Long> anchors = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        if (anchors.size() != ErdenKingdomExteriorBuilder.EXPECTED_CI_ANCHORS) {
            throw new IllegalStateException("Invalid Erden exterior ticket anchor count " + anchors.size());
        }
        return anchors;
    }

    private static Set<Long> anchorsFor(ErdenKingdomSupplyCatalog.SupplyNode node) {
        Set<Long> anchors = new LinkedHashSet<>();
        for (int[] offset : NODE_ANCHOR_OFFSETS) {
            anchors.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
        }
        anchors.add(ErdenKingdomExteriorBuilder.storageAnchorChunk(node));
        return anchors;
    }
"""
if "Invalid Erden exterior ticket anchor count" not in reaper:
    if reaper.count(old) != 1:
        raise SystemExit("reaper anchor pattern missing")
    reaper = reaper.replace(old, new)
reaper_path.write_text(reaper, encoding="utf-8")

workflow_path = Path(".github/workflows/audit-living-kingdoms-exterior-workforce.yml")
workflow = workflow_path.read_text(encoding="utf-8")
workflow = workflow.replace("anchors=90 released=90", "anchors=104 released=104")
if "EXPECTED_CI_ANCHORS = 104" not in workflow:
    needle = """          grep -F 'CI_MAX_IN_FLIGHT = 3' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
"""
    replacement = needle + """          grep -F 'EXPECTED_CI_ANCHORS = 104' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
          grep -F 'storageAnchorChunk(node)' \\
            \"$PROJECT_DIR/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java\"
"""
    if needle not in workflow:
        raise SystemExit("workflow anchor invariant insertion point missing")
    workflow = workflow.replace(needle, replacement, 1)
workflow_path.write_text(workflow, encoding="utf-8")
