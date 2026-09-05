from pathlib import Path

root = Path('projects/frontier-settlement')

gp = root / 'gradle.properties'
g = gp.read_text(encoding='utf-8')
if 'mod_version=0.1.0-alpha.97' not in g:
    raise SystemExit('unexpected Frontier version')
g = g.replace('mod_version=0.1.0-alpha.97', 'mod_version=0.1.0-alpha.98', 1)
g += '\n# Alpha.98 residential integrity: severely destroyed completed houses automatically retire their stale settlement record and clear only matching non-container Frontier blueprint remnants so the lot can be rebuilt without a command.\n'
gp.write_text(g, encoding='utf-8')

service = root / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementBuildingIntegrityService.java'
service.write_text('''package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Keeps the settlement ledger tied to the physical world for completed houses.
 *
 * A heavily burned/exploded house must not keep granting housing forever or permanently reserve a
 * dead lot. We only retire a house after every blueprint position is loaded and fewer than 45% of
 * its expected blocks remain. Retirement clears only blocks that still exactly match the Frontier
 * house blueprint, and never deletes block entities or arbitrary player blocks.
 */
public final class SettlementBuildingIntegrityService {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int RUIN_INTACT_PERCENT = 45;

    private SettlementBuildingIntegrityService() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;
        ServerLevel level = server.overworld();
        for (BuildingRecord building : List.copyOf(data.buildings())) {
            BuildingType type = BuildingType.fromId(building.type());
            if (type != BuildingType.HOUSE) continue;
            if (!fullyLoaded(level, type, building)) continue;
            List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(
                    type, building.origin(), building.rotation());
            int intact = 0;
            for (BuildingBlueprints.Placement placement : plan) {
                if (level.getBlockState(placement.pos()).is(placement.state().getBlock())) intact++;
            }
            if ((long) intact * 100L >= (long) plan.size() * RUIN_INTACT_PERCENT) continue;

            clearKnownHouseRemnants(level, plan);
            if (data.removeCompletedBuilding(building)) {
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
            break; // bounded: retire at most one ruined house per scan
        }
    }

    private static boolean fullyLoaded(ServerLevel level, BuildingType type, BuildingRecord building) {
        List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, building.origin(), building.rotation());
        for (BuildingBlueprints.Placement placement : plan) {
            if (!level.hasChunkAt(placement.pos())) return false;
        }
        return true;
    }

    private static void clearKnownHouseRemnants(ServerLevel level, List<BuildingBlueprints.Placement> plan) {
        for (BuildingBlueprints.Placement placement : plan) {
            BlockPos pos = placement.pos();
            if (level.getBlockEntity(pos) != null) continue;
            BlockState current = level.getBlockState(pos);
            if (!current.is(placement.state().getBlock())) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
''', encoding='utf-8')

data_file = root / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementData.java'
s = data_file.read_text(encoding='utf-8')
anchor = '''    public void clearConstruction() { if (!construction.active()) return; construction = ConstructionState.EMPTY; setDirty(); }\n'''
method = '''    public boolean removeCompletedBuilding(BuildingRecord target) {
        if (target == null) return false;
        List<BuildingRecord> next = new ArrayList<>(buildings());
        if (!next.remove(target)) return false;
        BuildingType type = BuildingType.fromId(target.type());
        if (type == BuildingType.HOUSE) houseCount = Math.max(0, houseCount - 1);
        if (type == BuildingType.LUMBER_CAMP) lumberCampCount = Math.max(0, lumberCampCount - 1);
        if (type != null) housingCapacity = Math.max(0, housingCapacity - type.housingGain());
        infrastructure = new SettlementInfrastructureState(next, roads(), roadConstruction(), outposts(), outpostConstruction());
        setDirty();
        return true;
    }

'''
if anchor not in s:
    raise SystemExit('SettlementData anchor missing')
s = s.replace(anchor, method + anchor, 1)
data_file.write_text(s, encoding='utf-8')

main = root / 'src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java'
s = main.read_text(encoding='utf-8')
import_anchor = 'import kr.moonseungjun.frontiersettlement.settlement.DroppedItemCleanupService;\n'
if import_anchor not in s:
    raise SystemExit('main import anchor missing')
s = s.replace(import_anchor, import_anchor + 'import kr.moonseungjun.frontiersettlement.settlement.SettlementBuildingIntegrityService;\n', 1)
listener_anchor = '        NeoForge.EVENT_BUS.addListener(DroppedItemCleanupService::onServerTick);\n'
if listener_anchor not in s:
    raise SystemExit('main listener anchor missing')
s = s.replace(listener_anchor, listener_anchor + '        NeoForge.EVENT_BUS.addListener(SettlementBuildingIntegrityService::onServerTick);\n', 1)
main.write_text(s, encoding='utf-8')
print('ALPHA98_RESIDENTIAL_INTEGRITY_PATCHED')
