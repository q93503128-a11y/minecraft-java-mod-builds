package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative resource gate for Target Assist / Predictive Combat Core. */
public final class NeuralCombatAssistService {
    private static final Map<UUID, Boolean> REQUESTED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();

    private NeuralCombatAssistService() {}

    public static void setRequested(ServerPlayer player, boolean requested) {
        REQUESTED.put(player.getUUID(), requested);
        if (!requested) ACTIVE.put(player.getUUID(), false);
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        UUID id = player.getUUID();
        if (!REQUESTED.getOrDefault(id, false)) {
            ACTIVE.put(id, false);
            return;
        }

        TitanPlayerData.AugmentInstance targetAssist = state.firstInstalledInstance("target_assist");
        TitanPlayerData.AugmentInstance predictive = state.firstInstalledInstance("predictive_combat_core");
        if (targetAssist == null && predictive == null) {
            ACTIVE.put(id, false);
            return;
        }

        AugmentationResourceService.Snapshot resources = AugmentationResourceService.snapshot(state);
        if (resources.neuralOverloaded() || state.heat() >= 98.0D) {
            ACTIVE.put(id, false);
            return;
        }

        double required = 0.0D;
        if (targetAssist != null) required += AugmentationResourceService.continuousPowerCostPerTick(state, "target_assist");
        if (predictive != null) required += AugmentationResourceService.continuousPowerCostPerTick(state, "predictive_combat_core");
        if (AugmentationResourceService.currentPower(player, state) + 1.0E-6D < required) {
            ACTIVE.put(id, false);
            return;
        }

        boolean spent = true;
        if (targetAssist != null) spent &= AugmentationResourceService.trySpendContinuousPower(player, state, "target_assist");
        if (predictive != null) spent &= AugmentationResourceService.trySpendContinuousPower(player, state, "predictive_combat_core");
        if (!spent) {
            ACTIVE.put(id, false);
            return;
        }

        ACTIVE.put(id, true);
        if (player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            double rawHeat = 0.0D;
            if (targetAssist != null) {
                AugmentationCatalog.Definition definition = AugmentationCatalog.byId("target_assist");
                if (definition != null && definition.heatLoad() > 0) {
                    rawHeat += definition.heatLoad() * 0.018D * state.heatLoadMultiplier("target_assist");
                }
            }
            if (predictive != null) {
                AugmentationCatalog.Definition definition = AugmentationCatalog.byId("predictive_combat_core");
                if (definition != null && definition.heatLoad() > 0) {
                    rawHeat += definition.heatLoad() * 0.018D * state.heatLoadMultiplier("predictive_combat_core");
                }
            }
            if (rawHeat > 0.0D) {
                data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
            }
            if (player.tickCount % 40 == 0) {
                if (targetAssist != null) data.addMasteryXp(player, "target_assist", 1);
                if (predictive != null) data.addMasteryXp(player, "predictive_combat_core", 1);
            }
        }
    }

    public static boolean active(UUID playerId) {
        return ACTIVE.getOrDefault(playerId, false);
    }

    public static void clear(UUID playerId) {
        REQUESTED.remove(playerId);
        ACTIVE.remove(playerId);
    }

    public static void clearAll() {
        REQUESTED.clear();
        ACTIVE.clear();
    }
}
