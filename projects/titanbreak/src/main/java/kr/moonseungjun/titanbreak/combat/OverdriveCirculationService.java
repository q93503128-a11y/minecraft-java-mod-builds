package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.augmentation.AugmentIntegrityService;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime burst-output state for the Overdrive Circulatory Organ. */
public final class OverdriveCirculationService {
    private static final Identifier ATTACK_ID = id("overdrive_attack");
    private static final Identifier MOVE_ID = id("overdrive_move");
    private static final Identifier ATTACK_SPEED_ID = id("overdrive_attack_speed");
    private static final Identifier KNOCKBACK_ID = id("overdrive_knockback");

    private static final long BASE_DURATION = 60L;
    private static final long PLUS_FIVE_DURATION = 80L;
    private static final long REUSE_DELAY_AFTER_END = 180L;
    private static final double ACTIVATION_POWER = 12.0D;

    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        long activeUntil = Long.MIN_VALUE / 4L;
        long readyTick;
        int enhancement;
    }

    private OverdriveCirculationService() {}

    public static void activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        TitanPlayerData.AugmentInstance overdrive = state.firstInstalledInstance("overdrive_circulation");
        if (overdrive == null) return;

        long now = level.getGameTime();
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        if (now < runtime.readyTick || now < runtime.activeUntil) return;

        AugmentationResourceService.Snapshot resources = AugmentationResourceService.snapshot(state);
        if (resources.neuralOverloaded() || state.heat() >= 85.0D) return;
        if (!AugmentationResourceService.trySpendBurstPower(player, state, ACTIVATION_POWER)) return;

        int enhancement = overdrive.enhancement();
        runtime.enhancement = enhancement;
        runtime.activeUntil = now + (enhancement >= 5 ? PLUS_FIVE_DURATION : BASE_DURATION);
        runtime.readyTick = runtime.activeUntil + REUSE_DELAY_AFTER_END;

        double preActivationHeat = state.heat();
        double activationHeat = enhancement >= 10 ? 10.0D : 7.0D;
        data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, activationHeat));
        if (enhancement >= 10 && preActivationHeat >= 60.0D) {
            AugmentIntegrityService.stress(player, state, overdrive, preActivationHeat >= 80.0D ? 2 : 1);
        }
        data.addMasteryXp(player, "overdrive_circulation", 4);
        TitanbreakNetwork.sync(player);
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        TitanPlayerData.AugmentInstance overdrive = state.firstInstalledInstance("overdrive_circulation");
        if (runtime == null || overdrive == null) {
            clearModifiers(player);
            if (overdrive == null) RUNTIME.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        if (now >= runtime.activeUntil) {
            runtime.activeUntil = Long.MIN_VALUE / 4L;
            clearModifiers(player);
            return;
        }

        double powerPerTick = runtime.enhancement >= 10 ? 0.30D : 0.22D;
        if (!AugmentationResourceService.trySpendBurstPower(player, state, powerPerTick) || state.heat() >= 98.0D) {
            runtime.activeUntil = Long.MIN_VALUE / 4L;
            clearModifiers(player);
            return;
        }

        applyOutput(player, runtime.enhancement);
        if (player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            double rawHeat = runtime.enhancement >= 10 ? 0.62D : 0.42D;
            if (runtime.enhancement >= 10 && state.heat() >= 90.0D && player.tickCount % 40 == 0) {
                AugmentIntegrityService.stress(player, state, overdrive, 1);
            }
            data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
            if (player.tickCount % 20 == 0) data.addMasteryXp(player, "overdrive_circulation", 2);
        }
    }

    public static boolean active(UUID playerId) {
        RuntimeState runtime = RUNTIME.get(playerId);
        return runtime != null && runtime.activeUntil > 0L;
    }

    public static int remainingTicks(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) return 0;
        return (int) Math.max(0L, runtime.activeUntil - player.level().getGameTime());
    }

    private static void applyOutput(ServerPlayer player, int enhancement) {
        double attack = enhancement >= 10 ? 0.45D : enhancement >= 7 ? 0.30D : 0.20D;
        double move = enhancement >= 10 ? 0.28D : enhancement >= 7 ? 0.20D : 0.12D;
        double attackSpeed = enhancement >= 10 ? 0.35D : enhancement >= 7 ? 0.25D : 0.15D;
        set(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, attack, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MOVEMENT_SPEED, MOVE_ID, move, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (enhancement >= 10) {
            set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.25D, AttributeModifier.Operation.ADD_VALUE);
        } else {
            remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        }
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.operation() == operation && Math.abs(current.amount() - amount) < 1.0E-8D) return;
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) instance.removeModifier(id);
    }

    private static void clearModifiers(ServerPlayer player) {
        remove(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
        remove(player, Attributes.MOVEMENT_SPEED, MOVE_ID);
        remove(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
    }

    public static void clear(ServerPlayer player) {
        clearModifiers(player);
        RUNTIME.remove(player.getUUID());
    }

    public static void clearAll() {
        RUNTIME.clear();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path);
    }
}
