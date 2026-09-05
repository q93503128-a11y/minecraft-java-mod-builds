from pathlib import Path

repo = Path(__file__).resolve().parents[2]

gp = repo / "projects/frontier-settlement/gradle.properties"
g = gp.read_text(encoding="utf-8")
if "mod_version=0.1.0-alpha.93" not in g:
    raise SystemExit("unexpected Frontier version")
g = g.replace("mod_version=0.1.0-alpha.93", "mod_version=0.1.0-alpha.94", 1)
g += "\n# Alpha.94 builder site-envelope recovery: once the physical builder reaches the local construction zone it may place every blueprint height from ground level; /frontier status now reports an unreachable site instead of silently stalling.\n"
gp.write_text(g, encoding="utf-8")

p = repo / "projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java"
s = p.read_text(encoding="utf-8")

old = "    private static final double WORK_POSITION_REACHED_SQR = 9.0D;\n"
new = old + "    private static final int SITE_WORK_MARGIN = 12;\n"
if old not in s or "SITE_WORK_MARGIN" in s:
    raise SystemExit("unexpected work-range declaration")
s = s.replace(old, new, 1)

start = s.index("    private static boolean moveBuilderToWorkPosition(")
end = s.index("    private static List<BlockPos> workPositionsFor(", start)
replacement = '''    private static boolean moveBuilderToWorkPosition(ServerLevel level, ConstructionState construction, BuildingType type,
                                                     BuildingBlueprints.Placement placement, FrontierWorkerEntity builder, BlockPos supply) {
        // The builder must physically reach the construction zone, but no individual blueprint block
        // requires a fragile exact perimeter cell or vertical scaffold. Once the worker is locally on site,
        // every height is authoritative from ground level. This keeps construction visible without letting
        // hedges, doorways or already-built walls turn one later blueprint step into a permanent stall.
        if (builderWithinSiteWorkEnvelope(construction, type, builder)) {
            builder.getNavigation().stop();
            return true;
        }
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
            double workDistance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
            if (workDistance <= WORK_POSITION_REACHED_SQR) {
                builder.getNavigation().stop();
                return true;
            }
            if (moveToReachable(builder, work, 1.05D)) return false;
        }
        builder.getNavigation().stop();
        return false;
    }

    private static boolean builderWithinSiteWorkEnvelope(ConstructionState construction, BuildingType type,
                                                          FrontierWorkerEntity builder) {
        BuildingRotation rotation = construction.buildingRotation();
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        double minX = construction.originX() - SITE_WORK_MARGIN;
        double maxX = construction.originX() + width - 1 + SITE_WORK_MARGIN + 1.0D;
        double minZ = construction.originZ() - SITE_WORK_MARGIN;
        double maxZ = construction.originZ() + depth - 1 + SITE_WORK_MARGIN + 1.0D;
        return builder.getX() >= minX && builder.getX() <= maxX
                && builder.getZ() >= minZ && builder.getZ() <= maxZ;
    }

    private static boolean hasReachableGroundWorkPosition(ServerLevel level, ConstructionState construction,
                                                          BuildingType type, BuildingBlueprints.Placement placement,
                                                          FrontierWorkerEntity builder, BlockPos supply) {
        if (builderWithinSiteWorkEnvelope(construction, type, builder)) return true;
        for (BlockPos work : workPositionsFor(level, construction, type, placement, builder, supply)) {
            if (createReachablePath(builder, work) != null) return true;
        }
        return false;
    }

'''
s = s[:start] + replacement + s[end:]

issue_start = s.index("    public static String constructionIssue(")
issue_end = s.index("    /**\n     * Explicit safe repair for /frontier normalize.", issue_start)
segment = s[issue_start:issue_end]
needle = '''            }
            return "";
        }

        for (BuildingBlueprints.Placement placement : plan) {
'''
replacement_issue = '''            }
            if (!hasReachableGroundWorkPosition(level, construction, type, placement, builder, supply)) {
                return "건설 현장 접근 불가 · 건물 주변 지상 통로를 확인하세요";
            }
            return "";
        }

        for (BuildingBlueprints.Placement placement : plan) {
'''
if needle not in segment:
    raise SystemExit("missing constructionIssue insertion point")
segment = segment.replace(needle, replacement_issue, 1)
s = s[:issue_start] + segment + s[issue_end:]

p.write_text(s, encoding="utf-8")
print("ALPHA94_SOURCE_PATCHED")
