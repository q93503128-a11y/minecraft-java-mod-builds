package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.Set;

/**
 * Minecraft adapter for the first production expedition loop.
 * The staging cell is deliberately presentation-neutral: final region art remains behind the design gate.
 */
public final class ExpeditionGameplayService {
    public static final ContentId REGION_ID = ContentId.rift("region/region_01");
    public static final ContentId CONTRACT_ID = ContentId.rift("contract/region_01_salvage_recovery");
    public static final ContentId RESOURCE_ID = ContentId.rift("resource/region_01_salvage");

    private static final int REGION_OFFSET_X = 320;
    private static final int REGION_OFFSET_Z = 320;

    private ExpeditionGameplayService() {}

    public static ExpeditionRun start(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        if (active(world).isPresent()) {
            throw new IllegalStateException("An expedition is already active in this vertical-slice world");
        }

        var snapshot = ContentRuntime.requireCurrent();
        var lifecycle = new ExpeditionLifecycle(snapshot);
        long expectedSequence = world.expeditionSequence() + 1L;
        ExpeditionRun validated = lifecycle.begin(expectedSequence, REGION_ID, CONTRACT_ID, snapshot.fingerprint(), level.getGameTime());
        ExpeditionRun persisted = world.createExpedition(REGION_ID, CONTRACT_ID, snapshot.fingerprint(), level.getGameTime());
        if (persisted.sequence() != validated.sequence()) {
            throw new IllegalStateException("Authoritative expedition allocation drifted from validated sequence");
        }

        ExpeditionRun deployed = lifecycle.deploy(persisted);
        world.updateExpedition(deployed);
        ServerLevel overworld = level.getServer().overworld();
        BlockPos entry = prepareTechnicalRegionCell(overworld);
        teleport(player, overworld, entry);
        player.sendSystemMessage(Component.literal("[Riftfrontier] Expedition deployed. Recover 3 amethyst-marked salvage nodes, then extract."));
        return deployed;
    }

    public static boolean tryRecover(ServerPlayer player, BlockPos pos) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(player.serverLevel());
        Optional<ExpeditionRun> active = active(world);
        if (active.isEmpty() || active.get().status() != ExpeditionRun.Status.DEPLOYED) return false;
        if (!active.get().regionId().equals(REGION_ID)) return false;

        ServerLevel overworld = player.serverLevel().getServer().overworld();
        if (player.serverLevel() != overworld || !insideTechnicalRegionCell(overworld, pos)) return false;
        if (!overworld.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) return false;

        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun recovered = lifecycle.recover(active.get(), RESOURCE_ID, 1);
        world.updateExpedition(recovered);
        overworld.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        int amount = recovered.recoveredResources().getOrDefault(RESOURCE_ID, 0);
        player.sendSystemMessage(Component.literal("[Riftfrontier] Field salvage secured: " + amount + "/3"));
        return true;
    }

    public static ExpeditionLifecycle.Resolution extract(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(player.serverLevel());
        ExpeditionRun run = active(world).orElseThrow(() -> new IllegalStateException("No active expedition"));
        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun requested = lifecycle.requestExtraction(run);
        world.updateExpedition(requested);
        ExpeditionLifecycle.Resolution resolution = lifecycle.resolveExtraction(requested, player.serverLevel().getGameTime());
        world.updateExpedition(resolution.run());
        returnToHub(player);
        int retained = resolution.retainedResources().values().stream().mapToInt(Integer::intValue).sum();
        player.sendSystemMessage(Component.literal("[Riftfrontier] Extraction complete. Secured resources: " + retained + ". " + resolution.worldConsequence()));
        return resolution;
    }

    public static Optional<ExpeditionRun> failActive(ServerPlayer player, String reason) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(player.serverLevel());
        Optional<ExpeditionRun> active = active(world);
        if (active.isEmpty()) return Optional.empty();
        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun failed = lifecycle.fail(active.get(), player.serverLevel().getGameTime());
        world.updateExpedition(failed);
        player.sendSystemMessage(Component.literal("[Riftfrontier] Expedition failed: " + reason));
        return Optional.of(failed);
    }

    public static String status(ServerPlayer player) {
        return active(RiftfrontierWorldData.get(player.serverLevel()))
            .map(run -> "sequence=" + run.sequence() + ", status=" + run.status() + ", recovered=" + run.recoveredResources())
            .orElse("no active expedition");
    }

    public static Optional<ExpeditionRun> active(RiftfrontierWorldData world) {
        return world.expeditions().stream()
            .filter(run -> !run.status().terminal())
            .max(java.util.Comparator.comparingLong(ExpeditionRun::sequence));
    }

    private static BlockPos prepareTechnicalRegionCell(ServerLevel overworld) {
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = Math.max(80, spawn.getY() + 8);
        BlockPos center = new BlockPos(spawn.getX() + REGION_OFFSET_X, y, spawn.getZ() + REGION_OFFSET_Z);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                overworld.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy <= 4; dy++) overworld.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        int[][] nodes = {{-3,-3},{3,-3},{-3,3},{3,3},{0,0}};
        for (int[] node : nodes) overworld.setBlockAndUpdate(center.offset(node[0], 0, node[1]), Blocks.AMETHYST_BLOCK.defaultBlockState());
        return center.offset(0, 0, -4);
    }

    private static boolean insideTechnicalRegionCell(ServerLevel overworld, BlockPos pos) {
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = Math.max(80, spawn.getY() + 8);
        BlockPos center = new BlockPos(spawn.getX() + REGION_OFFSET_X, y, spawn.getZ() + REGION_OFFSET_Z);
        return Math.abs(pos.getX() - center.getX()) <= 5
            && Math.abs(pos.getZ() - center.getZ()) <= 5
            && pos.getY() >= center.getY() - 1
            && pos.getY() <= center.getY() + 4;
    }

    private static void returnToHub(ServerPlayer player) {
        ServerLevel overworld = player.serverLevel().getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        teleport(player, overworld, spawn.above());
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        boolean moved = player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
        if (!moved) throw new IllegalStateException("Minecraft rejected Riftfrontier expedition teleport");
    }
}
