from pathlib import Path

capital_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenCapitalStreamingBuilder.java")
capital = capital_path.read_text(encoding="utf-8")

old = "import kr.moonseungjun.livingkingdoms.LivingKingdoms;\n"
new = old + "import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;\n"
assert capital.count(old) == 1, "capital import anchor changed"
assert "worldgen.AuthoredContinentDensity" not in capital
capital = capital.replace(old, new)

old = "public static final int CAPITAL_REVISION = 4;"
new = "public static final int CAPITAL_REVISION = 5;"
assert capital.count(old) == 1, "capital revision anchor changed"
capital = capital.replace(old, new)

old = """                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);\n                BlockPos surface = new BlockPos(x, surfaceY, z);\n                boolean fluid = !level.getFluidState(surface).isEmpty();\n                if (fluid && roadClass != RoadClass.ROYAL) continue;\n\n                if (fluid && roadClass == RoadClass.ROYAL) {\n                    int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;\n                    if (Math.floorMod(x + z, 6) == 0) {\n                        plan.addFill(x, floor + 1, z, x, surfaceY - 1, z, Blocks.STONE_BRICKS);\n                    }\n                    plan.addSet(x, surfaceY, z, Blocks.STONE_BRICKS);\n                } else {\n                    plan.addSet(x, surfaceY, z,\n                            roadClass == RoadClass.ROYAL ? Blocks.POLISHED_ANDESITE : Blocks.PACKED_MUD);\n                }\n                plan.addFill(x, surfaceY + 1, z, x, surfaceY + 3, z, Blocks.AIR);\n"""
new = """                int originalSurfaceY = plan.originalSurfaceY(level, x, z);\n                BlockPos originalSurface = new BlockPos(x, originalSurfaceY, z);\n                boolean fluid = !level.getFluidState(originalSurface).isEmpty();\n                if (fluid && roadClass != RoadClass.ROYAL) continue;\n\n                int surfaceY = fluid\n                        ? originalSurfaceY\n                        : (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));\n                if (!fluid) {\n                    if (originalSurfaceY < surfaceY) {\n                        plan.addFill(x, originalSurfaceY + 1, z, x, surfaceY - 1, z, Blocks.DIRT);\n                    } else if (originalSurfaceY > surfaceY) {\n                        plan.addFill(x, surfaceY + 1, z, x, originalSurfaceY + 3, z, Blocks.AIR);\n                    }\n                }\n\n                if (fluid && roadClass == RoadClass.ROYAL) {\n                    int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;\n                    if (Math.floorMod(x + z, 6) == 0) {\n                        plan.addFill(x, floor + 1, z, x, surfaceY - 1, z, Blocks.STONE_BRICKS);\n                    }\n                    plan.addSet(x, surfaceY, z, Blocks.STONE_BRICKS);\n                } else {\n                    plan.addSet(x, surfaceY, z,\n                            roadClass == RoadClass.ROYAL ? Blocks.POLISHED_ANDESITE : Blocks.PACKED_MUD);\n                }\n                plan.addFill(x, surfaceY + 1, z, x, Math.max(surfaceY + 3, originalSurfaceY + 3), z, Blocks.AIR);\n                plan.setPlannedSurfaceY(x, z, surfaceY);\n"""
assert capital.count(old) == 1, "capital road body anchor changed"
capital = capital.replace(old, new)
capital_path.write_text(capital, encoding="utf-8")

infra_path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanInfrastructureBuilder.java")
infra = infra_path.read_text(encoding="utf-8")

old = """    public static void finalizeChunk(ServerLevel level, ChunkPos chunk) {\n        for (ServiceNode node : SERVICE_NODES) {\n"""
new = """    public static void finalizeChunk(ServerLevel level, ChunkPos chunk) {\n        finalizeRoyalCulvert(level, chunk);\n        for (ServiceNode node : SERVICE_NODES) {\n"""
assert infra.count(old) == 1, "infrastructure finalize anchor changed"
infra = infra.replace(old, new)

old = """                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);\n\n                if (isRoadEdge(x, z)) {\n"""
new = """                int surfaceY = plan.plannedSurfaceY(level, x, z);\n\n                if (isRoadEdge(x, z)) {\n"""
assert infra.count(old) == 1, "road drainage height anchor changed"
infra = infra.replace(old, new)

old = """        int surfaceY = RealmSitePlanner.surfaceY(level, x, z);\n        setClipped(plan, chunk, x, surfaceY - 4, z, Blocks.STONE_BRICKS);\n"""
new = """        int surfaceY = plan.plannedSurfaceY(level, x, z);\n        setClipped(plan, chunk, x, surfaceY - 4, z, Blocks.STONE_BRICKS);\n"""
assert infra.count(old) == 1, "culvert planning height anchor changed"
infra = infra.replace(old, new)

old = """                int surfaceY = RealmSitePlanner.surfaceY(level, x, z);\n                plan.addSet(x, surfaceY, z, material);\n"""
new = """                int surfaceY = authoredSurfaceY(x, z);\n                int originalSurfaceY = plan.originalSurfaceY(level, x, z);\n                if (originalSurfaceY < surfaceY) {\n                    plan.addFill(x, originalSurfaceY + 1, z, x, surfaceY - 1, z, Blocks.DIRT);\n                } else if (originalSurfaceY > surfaceY) {\n                    plan.addFill(x, surfaceY + 1, z, x, originalSurfaceY + 3, z, Blocks.AIR);\n                }\n                plan.addSet(x, surfaceY, z, material);\n                plan.setPlannedSurfaceY(x, z, surfaceY);\n"""
assert infra.count(old) == 1, "access path height anchor changed"
infra = infra.replace(old, new)

anchor = """    private static int serviceBaseY(ServiceNode node) {\n        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(node.x, node.z));\n    }\n\n"""
addition = """    private static void finalizeRoyalCulvert(ServerLevel level, ChunkPos chunk) {\n        int minX = chunk.getMinBlockX();\n        int minZ = chunk.getMinBlockZ();\n        for (int x = minX; x <= minX + 15; x++) {\n            for (int z = minZ; z <= minZ + 15; z++) {\n                if (x != 0 && z != 0) continue;\n                if (ErdenCapitalStreamingBuilder.roadClassAt(x, z)\n                        != ErdenCapitalStreamingBuilder.RoadClass.ROYAL) continue;\n                int surfaceY = authoredSurfaceY(x, z);\n                setNow(level, x, surfaceY - 4, z, Blocks.STONE_BRICKS);\n                setNow(level, x, surfaceY - 3, z, Blocks.WATER);\n                setNow(level, x, surfaceY - 2, z, Blocks.AIR);\n                setNow(level, x, surfaceY - 1, z, Blocks.STONE_BRICKS);\n                if (z == 0) {\n                    setNow(level, x, surfaceY - 3, z - 1, Blocks.STONE_BRICKS);\n                    setNow(level, x, surfaceY - 2, z - 1, Blocks.STONE_BRICKS);\n                    setNow(level, x, surfaceY - 3, z + 1, Blocks.STONE_BRICKS);\n                    setNow(level, x, surfaceY - 2, z + 1, Blocks.STONE_BRICKS);\n                } else {\n                    setNow(level, x - 1, surfaceY - 3, z, Blocks.STONE_BRICKS);\n                    setNow(level, x - 1, surfaceY - 2, z, Blocks.STONE_BRICKS);\n                    setNow(level, x + 1, surfaceY - 3, z, Blocks.STONE_BRICKS);\n                    setNow(level, x + 1, surfaceY - 2, z, Blocks.STONE_BRICKS);\n                }\n            }\n        }\n    }\n\n    private static int authoredSurfaceY(int x, int z) {\n        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));\n    }\n\n""" + anchor
assert infra.count(anchor) == 1, "infrastructure helper insertion anchor changed"
infra = infra.replace(anchor, addition)
infra_path.write_text(infra, encoding="utf-8")

# Keep all permanent CI expectations and documentation on the authored revision.
changed = []
for root in [Path(".github/workflows"), Path("projects/living-kingdoms/docs")]:
    if not root.exists():
        continue
    for candidate in root.rglob("*"):
        if not candidate.is_file() or candidate.suffix not in {".yml", ".yaml", ".md", ".txt"}:
            continue
        value = candidate.read_text(encoding="utf-8")
        replacement = value.replace("capital_revision=4", "capital_revision=5")
        replacement = replacement.replace("capital revision 4", "capital revision 5")
        if replacement != value:
            candidate.write_text(replacement, encoding="utf-8")
            changed.append(str(candidate))
print("updated revision references:", changed)
