from pathlib import Path

root = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')
external_path = root / 'ExternalDistrictBuildingBuilder.java'
planner_path = root / 'RealmSitePlanner.java'

external = external_path.read_text(encoding='utf-8')
old = '            int surfaceY = RealmSitePlanner.surfaceY(level, span.x, span.z);\n'
new = '            int surfaceY = plan.plannedSurfaceY(level, span.x, span.z);\n'
if old in external:
    external = external.replace(old, new, 1)
elif new not in external:
    raise SystemExit('external building surface lookup pattern missing')
external_path.write_text(external, encoding='utf-8')

planner = planner_path.read_text(encoding='utf-8')
if 'import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;\n' not in planner:
    planner = planner.replace(
        'import kr.moonseungjun.livingkingdoms.worldgen.AuthoredBiomeVerifier;\n',
        'import kr.moonseungjun.livingkingdoms.worldgen.AuthoredBiomeVerifier;\n'
        'import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;\n',
        1)
planner = planner.replace('import net.minecraft.world.level.levelgen.Heightmap;\n', '')
old_method = '''    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }
'''
new_method = '''    public static int surfaceY(ServerLevel level, int x, int z) {
        // World-edit planning must never synchronously promote or load a chunk from the server tick.
        // Erden terrain is authored deterministically, so unloaded planning samples use that source of truth.
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }
'''
if old_method in planner:
    planner = planner.replace(old_method, new_method, 1)
elif new_method not in planner:
    raise SystemExit('RealmSitePlanner.surfaceY sync-load pattern missing')
planner_path.write_text(planner, encoding='utf-8')
