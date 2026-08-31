package kr.moonseungjun.titanbreak.augmentation;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AugmentationResourceService {
    public static final double BASE_POWER_CAPACITY = 100.0D;
    public static final double BASE_HEAT_CAPACITY = 100.0D;
    public static final double BASE_NEURAL_CAPACITY = 100.0D;

    private static final double BASE_POWER_REGEN_PER_TICK = 0.20D;
    private static final double BASE_COOLING_PER_TICK = 0.45D;
    private static final int POWER_REGEN_DELAY_TICKS = 10;

    private static final Set<String> AUTO_CONTROL_AUGMENTS = Set.of(
            "target_assist",
            "predictive_combat_core",
            "reflex_accelerator",
            "threat_detection",
            "motor_sync_core",
            "combat_autopilot"
    );
    private static final Set<String> TEMPORAL_AUGMENTS = Set.of(
            "reflex_drive_i",
            "phase_step_spine"
    );

    private static final ConcurrentMap<UUID, Double> POWER = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Long> LAST_POWER_USE_TICK = new ConcurrentHashMap<>();

    public record Snapshot(double powerCapacity, double heatCapacity, double neuralCapacity,
                           double powerLoad, double heatLoad, double neuralLoad,
                           double powerRegenPerTick, double coolingPerTick,
                           double powerCostMultiplier, double heatGenerationMultiplier) {
        public boolean neuralOverloaded() {
            return neuralLoad > neuralCapacity + 1.0E-6D;
        }

        public double neuralFraction() {
            return neuralCapacity <= 0.0D ? 1.0D : neuralLoad / neuralCapacity;
        }
    }

    private AugmentationResourceService() {}

    public static Snapshot snapshot(TitanPlayerData.State state) {
        double powerCapacity = BASE_POWER_CAPACITY;
        double heatCapacity = BASE_HEAT_CAPACITY;
        double neuralCapacity = BASE_NEURAL_CAPACITY;
        double powerRegen = BASE_POWER_REGEN_PER_TICK;
        double cooling = BASE_COOLING_PER_TICK;
        double powerCostMultiplier = 1.0D;
        double heatGenerationMultiplier = 1.0D;

        TitanPlayerData.AugmentInstance bus = state.firstInstalledInstance("high_speed_neural_bus");
        int busEnhancement = bus == null ? 0 : bus.enhancement();
        if (bus != null && busEnhancement >= 5) neuralCapacity += 25.0D;

        TitanPlayerData.AugmentInstance auxiliaryPower = state.firstInstalledInstance("auxiliary_power_organ");
        if (auxiliaryPower != null) {
            powerCapacity += 35.0D;
            powerRegen += 0.12D;
            if (auxiliaryPower.enhancement() >= 5) powerCapacity += 25.0D;
            if (auxiliaryPower.enhancement() >= 7) powerRegen += 0.18D;
            if (auxiliaryPower.enhancement() >= 10) powerCostMultiplier *= 0.85D;
        }

        TitanPlayerData.AugmentInstance heatShunt = state.firstInstalledInstance("heat_shunt_mesh");
        if (heatShunt != null) {
            heatCapacity += 25.0D;
            cooling += 0.10D;
            if (heatShunt.enhancement() >= 5) heatCapacity += 25.0D;
            if (heatShunt.enhancement() >= 7) cooling += 0.20D;
        }

        TitanPlayerData.AugmentInstance adrenaline = state.firstInstalledInstance("adrenaline_pump");
        if (adrenaline != null && adrenaline.enhancement() >= 7) {
            heatGenerationMultiplier *= 0.90D;
        }

        double powerLoad = 0.0D;
        double heatLoad = 0.0D;
        double neuralLoad = 0.0D;
        for (TitanPlayerData.AugmentInstance instance : state.installedInstanceView().values().stream().distinct().toList()) {
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId(instance.id());
            if (definition == null) continue;

            powerLoad += scaledSignedLoad(definition.powerLoad(), state.powerLoadMultiplier(instance.id()));
            heatLoad += scaledSignedLoad(definition.heatLoad(), state.heatLoadMultiplier(instance.id()));

            double neural = scaledSignedLoad(definition.neuralLoad(), state.neuralLoadMultiplier(instance.id()));
            if (neural > 0.0D && bus != null) {
                if (busEnhancement >= 7 && AUTO_CONTROL_AUGMENTS.contains(instance.id())) neural *= 0.85D;
                if (busEnhancement >= 10 && TEMPORAL_AUGMENTS.contains(instance.id())) neural *= 0.80D;
            }
            neuralLoad += neural;
        }

        return new Snapshot(
                powerCapacity,
                heatCapacity,
                neuralCapacity,
                Math.max(0.0D, powerLoad),
                heatLoad,
                Math.max(0.0D, neuralLoad),
                powerRegen,
                cooling,
                powerCostMultiplier,
                heatGenerationMultiplier
        );
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        Snapshot resources = snapshot(state);
        UUID id = player.getUUID();
        double current = POWER.computeIfAbsent(id, ignored -> resources.powerCapacity());
        current = Math.min(current, resources.powerCapacity());

        long now = player.level().getGameTime();
        long lastUse = LAST_POWER_USE_TICK.getOrDefault(id, Long.MIN_VALUE / 4L);
        if (now - lastUse >= POWER_REGEN_DELAY_TICKS) {
            current = Math.min(resources.powerCapacity(), current + resources.powerRegenPerTick());
        }
        POWER.put(id, current);
    }

    public static double currentPower(ServerPlayer player, TitanPlayerData.State state) {
        Snapshot resources = snapshot(state);
        UUID id = player.getUUID();
        double current = POWER.computeIfAbsent(id, ignored -> resources.powerCapacity());
        if (current > resources.powerCapacity()) {
            current = resources.powerCapacity();
            POWER.put(id, current);
        }
        return Math.max(0.0D, current);
    }

    public static double continuousPowerCostPerTick(TitanPlayerData.State state, String augmentId) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (definition == null || definition.powerLoad() <= 0) return 0.0D;
        Snapshot resources = snapshot(state);
        return definition.powerLoad()
                * state.powerLoadMultiplier(augmentId)
                * 0.05D
                * resources.powerCostMultiplier();
    }

    public static boolean trySpendContinuousPower(ServerPlayer player, TitanPlayerData.State state, String augmentId) {
        Snapshot resources = snapshot(state);
        if (resources.neuralOverloaded()) return false;
        double cost = continuousPowerCostPerTick(state, augmentId);
        return trySpend(player, state, cost);
    }

    public static boolean trySpendBurstPower(ServerPlayer player, TitanPlayerData.State state, double baseCost) {
        Snapshot resources = snapshot(state);
        if (resources.neuralOverloaded()) return false;
        return trySpend(player, state, Math.max(0.0D, baseCost) * resources.powerCostMultiplier());
    }

    private static boolean trySpend(ServerPlayer player, TitanPlayerData.State state, double cost) {
        if (cost <= 0.0D) return true;
        UUID id = player.getUUID();
        double current = currentPower(player, state);
        if (current + 1.0E-6D < cost) return false;
        POWER.put(id, Math.max(0.0D, current - cost));
        LAST_POWER_USE_TICK.put(id, player.level().getGameTime());
        return true;
    }

    public static double normalizedHeatGain(TitanPlayerData.State state, double rawHeat) {
        Snapshot resources = snapshot(state);
        if (rawHeat <= 0.0D) return rawHeat;
        return rawHeat * (BASE_HEAT_CAPACITY / resources.heatCapacity()) * resources.heatGenerationMultiplier();
    }

    public static void clearAll() {
        POWER.clear();
        LAST_POWER_USE_TICK.clear();
    }

    private static double scaledSignedLoad(int load, double positiveMultiplier) {
        if (load <= 0) return load;
        return load * positiveMultiplier;
    }
}
