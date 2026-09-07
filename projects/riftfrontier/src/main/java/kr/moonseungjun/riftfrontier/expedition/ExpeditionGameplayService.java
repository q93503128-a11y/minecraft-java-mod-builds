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
 * The fixed hub/region cells are bounded technical validation spaces, not final visual design.
 */
public final class ExpeditionGameplayService {
    public static final ContentId REGION_ID = ContentId.rift("region/region_01");
    public static final ContentId CONTRACT_ID = ContentId.rift("contract/region_01_salvage_recovery");
    public static final ContentId RESOURCE_ID = ContentId.rift("resource/region_01_salvage");

    private static final BlockPos TECHNICAL_HUB = new BlockPos(0, 100, 0);
    private static final BlockPos TECHNICAL_REGION = new BlockPos(320, 100, 320);

    private ExpeditionGameplayService() {}

    public static ExpeditionRun start(ServerPlayer player) {
        ServerLevel level = serverLevel(player);
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        if (active(world).isPresent()) throw new IllegalStateException("An expedition is already active in this vertical-slice world");

        var snapshot = ContentRuntime.requireCurrent();
        var lifecycle = new ExpeditionLifecycle(snapshot);
        long expectedSequence = world.expeditionSequence() + 1L;
        ExpeditionRun validated = lifecycle.begin(expectedSequence, REGION_ID, CONTRACT_ID, snapshot.fingerprint(), level.getGameTime());

        int supplyCost = world.region01PreparationSupplyCost();
        if (!world.consumeRegion01PreparationSupply()) {
            throw new IllegalStateException(
                "Insufficient expedition supply: need " + supplyCost + ", have " + world.expeditionSupply()
                    + ". Extract salvage and use /riftfrontier expedition provision."
            );
        }

        ExpeditionRun persisted = world.createExpedition(REGION_ID, CONTRACT_ID, snapshot.fingerprint(), level.getGameTime());
        if (persisted.sequence() != validated.sequence()) throw new IllegalStateException("Authoritative expedition allocation drifted from validated sequence");

        ExpeditionRun deployed = lifecycle.deploy(persisted);
        world.updateExpedition(deployed);
        ServerLevel overworld = level.getServer().overworld();
        prepareTechnicalCell(overworld, TECHNICAL_HUB, false);
        prepareTechnicalCell(overworld, TECHNICAL_REGION, true);
        var encounter = Region01EncounterRuntime.begin(overworld, TECHNICAL_REGION, deployed.sequence(), world.region01Pressure());
        teleport(player, overworld, TECHNICAL_REGION.offset(0, 0, -4));
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Expedition deployed. Supply spent: " + supplyCost
                + ". Region pressure: " + world.region01Pressure()
                + ". Threats: " + encounter.totalThreats() + " (hunter=" + encounter.hunters()
                + ", scout=" + encounter.scouts() + ", elite=" + encounter.elites() + "). Recover 3 salvage nodes."
        ));
        return deployed;
    }

    public static boolean tryRecover(ServerPlayer player, BlockPos pos) {
        ServerLevel playerLevel = serverLevel(player);
        RiftfrontierWorldData world = RiftfrontierWorldData.get(playerLevel);
        Optional<ExpeditionRun> active = active(world);
        if (active.isEmpty() || active.get().status() != ExpeditionRun.Status.DEPLOYED) return false;
        if (!active.get().regionId().equals(REGION_ID)) return false;

        ServerLevel overworld = playerLevel.getServer().overworld();
        if (playerLevel != overworld || !insideTechnicalRegionCell(pos)) return false;
        if (!overworld.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) return false;

        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun recovered = lifecycle.recover(active.get(), RESOURCE_ID, 1);
        world.updateExpedition(recovered);
        overworld.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        Region01EncounterRuntime.applySalvageHazard(player, world.region01Pressure());
        int amount = recovered.recoveredResources().getOrDefault(RESOURCE_ID, 0);
        int liveThreats = Region01EncounterRuntime.liveThreatCount(overworld, TECHNICAL_REGION, recovered.sequence());
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Field salvage secured: " + amount + "/3. Active patrol threats=" + liveThreats
                + ". Clear the patrol for a bonus salvage unit, or risk a fast extraction."
        ));
        return true;
    }

    public static ExpeditionLifecycle.Resolution extract(ServerPlayer player) {
        ServerLevel level = serverLevel(player);
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        ExpeditionRun run = active(world).orElseThrow(() -> new IllegalStateException("No active expedition"));
        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun requested = lifecycle.requestExtraction(run);
        world.updateExpedition(requested);
        ExpeditionLifecycle.Resolution resolution = lifecycle.resolveExtraction(requested, level.getGameTime());
        world.updateExpedition(resolution.run());

        ServerLevel overworld = level.getServer().overworld();
        boolean patrolCleared = Region01EncounterRuntime.patrolCleared(overworld, TECHNICAL_REGION, run.sequence());
        int baseRetainedSalvage = resolution.retainedResources().getOrDefault(RESOURCE_ID, 0);
        int patrolBonus = patrolCleared ? 1 : 0;
        int retainedSalvage = Math.addExact(baseRetainedSalvage, patrolBonus);
        world.settleRegion01Extraction(retainedSalvage);
        Region01EncounterRuntime.clearRun(overworld, TECHNICAL_REGION, run.sequence());
        returnToHub(player);
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Extraction complete. Hub salvage +" + retainedSalvage
                + " (base=" + baseRetainedSalvage + ", patrol bonus=" + patrolBonus
                + ", stored=" + world.securedRegion01Salvage() + "). Region pressure is now " + world.region01Pressure()
                + "; next expedition supply cost=" + world.region01PreparationSupplyCost()
                + ". " + resolution.worldConsequence()
        ));
        return resolution;
    }

    public static void provision(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(serverLevel(player));
        world.provisionRegion01Supply();
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Provisioned 2 expedition supply from 1 secured salvage. Storage="
                + world.securedRegion01Salvage() + ", supply=" + world.expeditionSupply()
                + ", next cost=" + world.region01PreparationSupplyCost()
        ));
    }

    public static Optional<ExpeditionRun> failActive(ServerPlayer player, String reason) {
        ServerLevel level = serverLevel(player);
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        Optional<ExpeditionRun> active = active(world);
        if (active.isEmpty()) return Optional.empty();
        var lifecycle = new ExpeditionLifecycle(ContentRuntime.requireCurrent());
        ExpeditionRun failed = lifecycle.fail(active.get(), level.getGameTime());
        world.updateExpedition(failed);
        Region01EncounterRuntime.clearRun(level.getServer().overworld(), TECHNICAL_REGION, failed.sequence());
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Expedition failed: " + reason + ". Preparation supply is not refunded."
        ));
        return Optional.of(failed);
    }

    public static String status(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get(serverLevel(player));
        String run = active(world)
            .map(value -> "sequence=" + value.sequence() + ", status=" + value.status() + ", recovered=" + value.recoveredResources())
            .orElse("no active expedition");
        return run
            + ", hubSalvage=" + world.securedRegion01Salvage()
            + ", supply=" + world.expeditionSupply()
            + ", regionPressure=" + world.region01Pressure()
            + ", nextSupplyCost=" + world.region01PreparationSupplyCost();
    }

    public static Optional<ExpeditionRun> active(RiftfrontierWorldData world) {
        return world.expeditions().stream()
            .filter(run -> !run.status().terminal())
            .max(java.util.Comparator.comparingLong(ExpeditionRun::sequence));
    }

    public static BlockPos technicalRegionCenter() {
        return TECHNICAL_REGION;
    }

    private static void prepareTechnicalCell(ServerLevel level, BlockPos center, boolean resourceNodes) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                level.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy <= 4; dy++) level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        }
        if (!resourceNodes) return;
        int[][] nodes = {{-3,-3},{3,-3},{-3,3},{3,3},{0,0}};
        for (int[] node : nodes) level.setBlockAndUpdate(center.offset(node[0], 0, node[1]), Blocks.AMETHYST_BLOCK.defaultBlockState());
    }

    private static boolean insideTechnicalRegionCell(BlockPos pos) {
        return Math.abs(pos.getX() - TECHNICAL_REGION.getX()) <= 5
            && Math.abs(pos.getZ() - TECHNICAL_REGION.getZ()) <= 5
            && pos.getY() >= TECHNICAL_REGION.getY() - 1
            && pos.getY() <= TECHNICAL_REGION.getY() + 4;
    }

    private static void returnToHub(ServerPlayer player) {
        ServerLevel overworld = serverLevel(player).getServer().overworld();
        prepareTechnicalCell(overworld, TECHNICAL_HUB, false);
        teleport(player, overworld, TECHNICAL_HUB);
    }

    private static ServerLevel serverLevel(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        boolean moved = player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
        if (!moved) throw new IllegalStateException("Minecraft rejected Riftfrontier expedition teleport");
    }
}
