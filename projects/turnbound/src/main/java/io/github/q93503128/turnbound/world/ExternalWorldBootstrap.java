package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Production world gate for the TURNBOUND overhaul.
 *
 * <p>Legacy Aster March builders are intentionally not called here. A verified/bound external world is used as-is.
 * Current Drehmal coordinates are integration seeds, not yet 26.2-playtested safe arrivals, so this bootstrap never
 * teleports a player or changes authored terrain. Safe arrival promotion belongs to the migrated-world inspection
 * gate.</p>
 */
public final class ExternalWorldBootstrap {
    private static final Set<UUID> ACTIVE = new LinkedHashSet<>();
    private static final Map<UUID, String> LAST_LOCATION = new HashMap<>();
    private static final Map<UUID, String> LAST_INTERACTION = new HashMap<>();
    private static final Map<UUID, String> LAST_NAVIGATION = new HashMap<>();

    private ExternalWorldBootstrap() {}

    public static boolean initialize(ServerPlayer player) {
        if (player == null || player.level().dimension() != Level.OVERWORLD) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            ACTIVE.remove(player.getUUID());
            FieldNetwork.close(player);
            return false;
        }

        // A bound authored world and the retired generated shell are mutually exclusive runtimes.
        // Clear any retained legacy session before the first external snapshot so no Aster writer can survive a
        // same-server migration/binding or retained-runtime reconnect.
        WorldSessionRouter.remove(player);
        ExternalWorldSavedData saved = ExternalWorldSavedData.get(server);
        DrehmalStartArrival.moveOutOfLegacySetupIfNeeded(player, saved);
        if (!player.isCreative() && !player.isSpectator()) {
            player.setGameMode(GameType.ADVENTURE);
        }

        ACTIVE.add(player.getUUID());
        DrehmalFirstRouteRuntime.recordProgress(player);
        LAST_LOCATION.put(player.getUUID(), DrehmalFirstRouteRuntime.locationId(player));
        LAST_INTERACTION.put(player.getUUID(), DrehmalFirstRouteRuntime.interactionId(player));
        LAST_NAVIGATION.put(player.getUUID(), DrehmalFirstRouteRuntime.navigationId(player));
        FieldNetwork.syncExternal(player, DrehmalFirstRouteRuntime.explorationSnapshot(player));

        if (!saved.initialized(player.getUUID())) {
            saved.markInitialized(player.getUUID());
            Turnbound.LOGGER.info(
                    "TURNBOUND external-world runtime opened for {}. Safe hub arrival remains pending terrain validation; hub seed={}",
                    player.getUUID(),
                    DrehmalWorldBinding.hubSeed());
        }
        return true;
    }

    public static boolean tick(ServerPlayer player) {
        if (player == null) return false;
        if (player.level().dimension() != Level.OVERWORLD) {
            if (ACTIVE.remove(player.getUUID())) FieldNetwork.close(player);
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            if (ACTIVE.remove(player.getUUID())) FieldNetwork.close(player);
            return false;
        }
        if (!ACTIVE.contains(player.getUUID())) return initialize(player);

        // Drehmal may move the host back into its setup terminal even after an earlier TURNBOUND arrival.
        // Re-check the physical zone itself so a stale one-shot flag can never trap an existing save there.
        if (player.tickCount % 10 == 0) {
            DrehmalStartArrival.moveOutOfLegacySetupIfNeeded(player, ExternalWorldSavedData.get(server));
        }

        if (player.tickCount % 20 == 0) DrehmalHubEntityPolicy.sweep(player);
        if (DrehmalFastTravelService.tick(player)) return true;
        DrehmalMountService.tick(player);
        DrehmalVisibleEncounterService.tick(player);
        DrabyelLocalArcRuntime.tick(player);
        DrehmalFieldNpcRuntime.tick(player);
        DrabyelHubServiceRuntime.tick(player);
        DrehmalWaystationRuntime.tick(player);
        DrehmalFirstRouteRuntime.recordProgress(player);
        String location = DrehmalFirstRouteRuntime.locationId(player);
        String previousLocation = LAST_LOCATION.put(player.getUUID(), location);
        boolean locationChanged = previousLocation == null || !previousLocation.equals(location);
        String interaction = DrehmalFirstRouteRuntime.interactionId(player);
        String previousInteraction = LAST_INTERACTION.put(player.getUUID(), interaction);
        boolean interactionChanged = previousInteraction == null || !previousInteraction.equals(interaction);
        String navigation = DrehmalFirstRouteRuntime.navigationId(player);
        String previousNavigation = LAST_NAVIGATION.put(player.getUUID(), navigation);
        boolean navigationChanged = previousNavigation == null || !previousNavigation.equals(navigation);
        if (locationChanged || interactionChanged || navigationChanged || player.tickCount % 40 == 0) {
            FieldNetwork.syncExternal(player, DrehmalFirstRouteRuntime.explorationSnapshot(player));
        }
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return player != null
                && ACTIVE.contains(player.getUUID())
                && player.level().dimension() == Level.OVERWORLD
                && DrehmalWorldBinding.isBound(player.level().getServer());
    }

    public static void refreshFieldContext(ServerPlayer player) {
        if (!active(player)) return;
        DrehmalFirstRouteRuntime.recordProgress(player);
        LAST_LOCATION.put(player.getUUID(), DrehmalFirstRouteRuntime.locationId(player));
        LAST_INTERACTION.put(player.getUUID(), DrehmalFirstRouteRuntime.interactionId(player));
        LAST_NAVIGATION.put(player.getUUID(), DrehmalFirstRouteRuntime.navigationId(player));
        FieldNetwork.syncExternal(player, DrehmalFirstRouteRuntime.explorationSnapshot(player));
    }

    public static void refreshAfterBattle(ServerPlayer player) {
        refreshFieldContext(player);
    }

    /**
     * External authored-world field commands are fail-closed. Current Drehmal travel/services are physical-world or
     * meta-surface interactions, so no legacy relay command is valid here.
     */
    public static void command(ServerPlayer player, String command) {
        if (!active(player) || command == null || command.isBlank()) return;
        String[] parts = command.split("\\|", -1);
        if (parts.length == 2 && "TRAVEL".equals(parts[0])
                && DrehmalFastTravelService.handle(player, parts[1])) {
            return;
        }
        if (parts.length == 2 && "MOUNT".equals(parts[0])
                && DrehmalMountService.handle(player, parts[1])) {
            return;
        }
        if (parts.length == 2 && "QUEST_ACCEPT".equals(parts[0]) && "CAPITAL_VALLEY".equals(parts[1])
                && DrabyelHubServiceRuntime.nearRole(player, "GREETER")) {
            if (DrabyelLocalArcProgress.acceptRegional(player)) {
                FieldNetwork.showDialogue(player, "문지기 아렌",
                        "캐피털 밸리 정찰 의뢰를 맡겼습니다. J 전체 지도에 표시한 세 지역 목표 중 하나를 해결하고 돌아오세요.");
            }
            refreshFieldContext(player);
            return;
        }
        if (parts.length == 2 && "CONTRACT_ACCEPT".equals(parts[0])
                && RegionalContractService.accept(player, parts[1])) {
            refreshFieldContext(player);
            return;
        }
        refreshFieldContext(player);
    }

    public static boolean interactEntity(ServerPlayer player, net.minecraft.world.entity.Entity target) {
        if (!active(player)) return false;
        boolean handled = DrehmalFieldNpcRuntime.interact(player, target)
                || DrabyelHubServiceRuntime.interact(player, target)
                || DrehmalWaystationRuntime.interact(player, target);
        if (handled) refreshFieldContext(player);
        return handled;
    }

    public static boolean serviceActor(net.minecraft.world.entity.Entity target) {
        return DrehmalFieldNpcRuntime.isNpc(target)
                || DrabyelHubServiceRuntime.serviceLocator(target) != null
                || DrehmalWaystationRuntime.isKeeper(target);
    }

    public static boolean onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        if (!active(player)) return false;
        return DrehmalVisibleEncounterService.onBattleEnded(player, encounterId, outcome);
    }

    /**
     * Shared-battle completion does not require the original claimant to still be online.
     */
    public static boolean onSharedBattleEnded(ServerLevel level, UUID claimantId, String encounterId, BattleOutcome outcome) {
        if (level == null || claimantId == null) return false;
        MinecraftServer server = level.getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) return false;
        return DrehmalVisibleEncounterService.onBattleEnded(level, claimantId, encounterId, outcome);
    }

    public static void remove(ServerPlayer player) {
        if (player == null) return;
        DrehmalVisibleEncounterService.onPlayerRemoved(player);
        DrehmalFastTravelService.remove(player);
        DrehmalMountService.release(player);
        ACTIVE.remove(player.getUUID());
        LAST_LOCATION.remove(player.getUUID());
        LAST_INTERACTION.remove(player.getUUID());
        LAST_NAVIGATION.remove(player.getUUID());
    }

    public static void clear() {
        DrehmalVisibleEncounterService.clear();
        DrehmalFieldNpcRuntime.clear();
        DrehmalAdaptiveRoutePlacement.clear();
        DrabyelLocalArcRuntime.clear();
        QuestTargetGlowService.clear();
        DrehmalFastTravelService.clear();
        DrehmalWaystationRuntime.clear();
        DrehmalMountService.clearAll();
        AvsalExpansionRuntime.clear();
        DrabyelHubServiceRuntime.clear();
        ACTIVE.clear();
        LAST_LOCATION.clear();
        LAST_INTERACTION.clear();
        LAST_NAVIGATION.clear();
    }
}
