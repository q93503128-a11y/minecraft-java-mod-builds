package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CloneEntityTypes;
import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative activation, cooldown and snapshot boundary for Clone. */
public final class CloneActiveService {
    public static final String CLONE_ID = id("clone");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final int DURATION_TICKS = 20 * 20;
    private static final int BASE_COOLDOWN_TICKS = 60 * 20;
    private static final double HEALTH_MULTIPLIER = 0.50D;
    private static final double DAMAGE_MULTIPLIER = 0.60D;

    private CloneActiveService() {
    }

    public static void initialize() {
        CloneEntityTypes.initialize();
        for (String augmentId : new String[]{CLONE_ID, ABILITY_HASTE_ID, DOUBLE_CHARGE_ID, COMBAT_CYCLE_ID}) {
            AugmentCatalog.require(augmentId);
        }
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(CloneActiveService::afterKill);
    }

    public static boolean tryUse(ServerPlayer player) {
        if (player.isDeadOrDying()) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, CLONE_ID) <= 0) return false;
        ChargeReservation reservation = reserveCharge(player, state);
        if (reservation == null) return false;
        if (!spawnClone(player)) return false;
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(
                player,
                latest.withCooldownReadyTick(reservation.key(), reservation.readyTick())
        );
        return true;
    }

    private static boolean spawnClone(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        CombatCloneEntity clone = new CombatCloneEntity(CloneEntityTypes.COMBAT_CLONE, level);
        float maxHealth = Math.max(1.0F, owner.getMaxHealth() * (float) HEALTH_MULTIPLIER);
        double damage = Math.max(0.0D, owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER);
        clone.configure(owner, maxHealth, damage, DURATION_TICKS);

        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-6D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 origin = owner.position();
        for (Vec3 offset : List.of(side, side.scale(-1.0D), flat.scale(-1.0D))) {
            Vec3 candidate = origin.add(offset);
            clone.snapTo(candidate.x, candidate.y, candidate.z);
            clone.setYRot(owner.getYRot());
            if (level.noCollision(clone, clone.getBoundingBox())) {
                return level.addFreshEntity(clone);
            }
        }
        return false;
    }

    private static ChargeReservation reserveCharge(
            ServerPlayer player,
            AugmentationPlayerState state
    ) {
        long now = player.level().getGameTime();
        int effectiveCooldown = BASE_COOLDOWN_TICKS;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            effectiveCooldown = Math.max(
                    1,
                    (int) Math.round(BASE_COOLDOWN_TICKS * (1.0D - ExpansionTuning.current().abilityHasteReduction()))
            );
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            if (state.cooldownReadyTick(CLONE_ID) > now) return null;
            return new ChargeReservation(CLONE_ID, now + effectiveCooldown);
        }
        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + effectiveCooldown);
    }

    private static void afterKill(ServerLevel level, Entity killer, LivingEntity killed, DamageSource source) {
        if (!(killer instanceof ServerPlayer player) || !CombatProvenanceContext.resolve(source).isRootDirect()) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int cycleLevel = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycleLevel <= 0 || boundedLevel(state, CLONE_ID) <= 0) return;
        int seconds = CombatExpansionService.isConfiguredBoss(killed) ? cycleLevel * 5 : cycleLevel;
        reduceCooldown(player, seconds * 20);
    }

    private static void reduceCooldown(ServerPlayer player, int reductionTicks) {
        if (reductionTicks <= 0) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(CLONE_ID);
            if (ready > now) {
                next = state.withCooldownReadyTick(CLONE_ID, Math.max(now, ready - reductionTicks));
            }
        } else {
            List<String> keys = new ArrayList<>(List.of(chargeKey(0), chargeKey(1)));
            keys.sort(Comparator.comparingLong(next::cooldownReadyTick));
            int remaining = reductionTicks;
            for (String key : keys) {
                if (remaining <= 0) break;
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) AugmentationPlayerStateService.replace(player, next);
    }

    public static boolean areCloneAllies(Entity first, Entity second) {
        CombatCloneEntity clone = first instanceof CombatCloneEntity value
                ? value
                : second instanceof CombatCloneEntity value ? value : null;
        if (clone == null || clone.ownerId() == null) return false;
        Entity other = clone == first ? second : first;
        return clone.isAlliedTo(other);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) {
        return CLONE_ID + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record ChargeReservation(String key, long readyTick) {
    }
}
