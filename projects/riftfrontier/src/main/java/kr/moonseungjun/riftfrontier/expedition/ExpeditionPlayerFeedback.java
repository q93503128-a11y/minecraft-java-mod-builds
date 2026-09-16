package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lightweight Minecraft-native player feedback for the first expedition vertical slice.
 *
 * <p>This is deliberately not a custom HUD or final Riftfrontier UI language. It reuses Minecraft's actionbar for
 * short-lived objective/status feedback while authoritative expedition, storage, pressure and encounter state remain
 * owned by the existing server services.</p>
 */
public final class ExpeditionPlayerFeedback {
    private ExpeditionPlayerFeedback() {}

    public static void hubReady(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("riftfrontier.expedition.feedback.hub_ready"), true);
    }

    public static void deployed(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get((ServerLevel) player.level());
        ExpeditionRun run = ExpeditionGameplayService.activeFor(player, world).orElse(null);
        if (run == null) return;
        int recovered = run.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        player.sendSystemMessage(Component.translatable(
            "riftfrontier.expedition.feedback.deployed",
            recovered,
            3,
            run.startContext().map(ExpeditionStartContext::regionPressure).orElse(world.region01Pressure())
        ), true);
    }

    /**
     * Refreshes the field objective from existing authoritative run + encounter state.
     * This owns no progress, combat or extraction state; it is only a temporary readable projection.
     */
    public static void fieldStatus(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        ExpeditionRun run = ExpeditionGameplayService.activeFor(player, world).orElse(null);
        if (run == null
            || run.status() != ExpeditionRun.Status.DEPLOYED
            || !run.regionId().equals(ExpeditionGameplayService.REGION_ID)) {
            return;
        }
        int recovered = run.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        int threats = Region01EncounterRuntime.liveThreatCount(
            level.getServer().overworld(),
            ExpeditionGameplayService.technicalRegionCenter(),
            run.sequence()
        );
        if (threats == 0) {
            player.sendSystemMessage(Component.translatable(
                "riftfrontier.expedition.feedback.patrol_cleared",
                recovered,
                3
            ), true);
            return;
        }
        if (recovered >= 3) {
            player.sendSystemMessage(Component.translatable(
                "riftfrontier.expedition.feedback.extraction_choice",
                threats
            ), true);
            return;
        }
        player.sendSystemMessage(Component.translatable(
            "riftfrontier.expedition.feedback.salvage",
            recovered,
            3,
            threats
        ), true);
    }

    public static void salvageUpdated(ServerPlayer player) {
        fieldStatus(player);
    }

    public static void extractionRelayOnline(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("riftfrontier.expedition.feedback.relay_online"), true);
    }

    public static void extractionComplete(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get((ServerLevel) player.level());
        player.sendSystemMessage(Component.translatable(
            "riftfrontier.expedition.feedback.extracted",
            world.securedRegion01Salvage(),
            world.region01Pressure(),
            world.region01PreparationSupplyCost()
        ), true);
    }

    public static void provisioned(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get((ServerLevel) player.level());
        player.sendSystemMessage(Component.translatable(
            "riftfrontier.expedition.feedback.provisioned",
            world.expeditionSupply(),
            world.securedRegion01Salvage(),
            world.region01PreparationSupplyCost()
        ), true);
    }
}
