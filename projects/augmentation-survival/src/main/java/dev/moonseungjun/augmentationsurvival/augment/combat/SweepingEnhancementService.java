package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Canonical existing-sweep/multi-hit enhancement with an optional Better Combat adapter boundary. */
public final class SweepingEnhancementService {
    public static final String ID = AugmentationSurvival.MOD_ID + ":sweeping_enhancement";
    private static final double[] RANGE_BONUS = {0.0D, 0.5D, 1.0D, 1.5D};
    private static final double[] ANGLE_BONUS = {0.0D, 20.0D, 40.0D, 60.0D};
    private static final float[] SECONDARY_DAMAGE = {1.0F, 1.25F, 1.50F, 1.75F};

    private static final ThreadLocal<BetterCombatTarget> CURRENT_TARGET = new ThreadLocal<>();
    private static final ThreadLocal<BetterCombatGeometry> CURRENT_GEOMETRY = new ThreadLocal<>();
    private static final ThreadLocal<BetterCombatHit> PENDING_HIT = new ThreadLocal<>();

    private static volatile boolean reflectionChecked;
    private static volatile Method getComboCount;
    private static volatile Method getCurrentAttack;
    private static volatile Method handAttack;
    private static volatile Method attackAngle;
    private static volatile Method attackRangeMultiplier;

    private SweepingEnhancementService() {
    }

    public static void initialize() {
        AugmentCatalog.require(ID);
    }

    public static AABB expandVanillaSweepBox(Player player, AABB original) {
        if (!(player instanceof ServerPlayer serverPlayer)) return original;
        AABB scaled = scaleHorizontally(original, bodyScale(serverPlayer));
        double bonus = rangeBonus(level(serverPlayer));
        return bonus > 0.0D ? scaled.inflate(bonus, 0.0D, bonus) : scaled;
    }

    public static float modifyVanillaSecondaryDamage(Player player, float damage) {
        if (!(player instanceof ServerPlayer serverPlayer)) return damage;
        return damage * secondaryDamageMultiplier(level(serverPlayer));
    }

    public static double clientRangeBonus(int level) {
        return rangeBonus(level);
    }

    public static double clientAngleBonus(int level) {
        return angleBonus(level);
    }

    /** Better Combat server validation calls this after resolving its current attack hand. */
    public static double modifyBetterCombatServerRange(Player player, ItemStack stack, double baseRange) {
        if (!(player instanceof ServerPlayer serverPlayer)) return baseRange;
        int level = level(serverPlayer);
        double bodyScale = bodyScale(serverPlayer);
        if (level <= 0 && bodyScale <= 1.000001D) {
            CURRENT_GEOMETRY.remove();
            return baseRange;
        }

        double multiplier = 1.0D;
        double angle = 360.0D;
        Object attack = currentBetterCombatAttack(player);
        if (attack != null) {
            multiplier = invokeNumber(attackRangeMultiplier, attack, 1.0D);
            angle = invokeNumber(attackAngle, attack, 360.0D);
        }
        double finalRange = Math.max(0.0D, baseRange * multiplier * bodyScale + rangeBonus(level));
        double finalAngle = Math.min(360.0D, Math.max(0.0D, angle + angleBonus(level)));
        CURRENT_GEOMETRY.set(new BetterCombatGeometry(
                serverPlayer.getUUID(),
                serverPlayer.tickCount,
                finalRange,
                finalAngle
        ));
        return finalRange;
    }

    /** Called from Better Combat's relation lookup before isHitAllowed. */
    public static void noteBetterCombatTarget(Player attacker, Entity target) {
        if (!(attacker instanceof ServerPlayer serverPlayer) || target == null) return;
        CURRENT_TARGET.set(new BetterCombatTarget(
                serverPlayer,
                target,
                serverPlayer.tickCount,
                false
        ));
    }

    /** Captures Better Combat's own direct-vs-secondary classification without changing it. */
    public static boolean noteBetterCombatDirect(boolean direct) {
        BetterCombatTarget current = CURRENT_TARGET.get();
        if (current != null) CURRENT_TARGET.set(current.withDirect(direct));
        return direct;
    }

    /**
     * Keeps Better Combat's normal relation decision, while independently validating the extra
     * secondary target against the enhanced server-side range/angle before arming its damage bonus.
     */
    public static boolean validateBetterCombatHit(boolean allowed) {
        BetterCombatTarget current = CURRENT_TARGET.get();
        if (!allowed || current == null) {
            PENDING_HIT.remove();
            return allowed;
        }
        if (current.direct()) {
            PENDING_HIT.remove();
            return true;
        }

        ServerPlayer player = current.player();
        int level = level(player);
        BetterCombatGeometry geometry = CURRENT_GEOMETRY.get();
        if (geometry != null
                && geometry.owner().equals(player.getUUID())
                && geometry.tick() == player.tickCount
                && !insideGeometry(player, current.target(), geometry)) {
            PENDING_HIT.remove();
            return false;
        }
        if (level <= 0) {
            PENDING_HIT.remove();
            return true;
        }

        PENDING_HIT.set(new BetterCombatHit(
                player.getUUID(),
                current.target().getId(),
                player.tickCount,
                secondaryDamageMultiplier(level)
        ));
        return true;
    }

    /** Applies only to the exact Better Combat secondary target armed above. */
    public static float modifyBetterCombatOutgoingDamage(
            LivingEntity victim,
            DamageSource source,
            float damage
    ) {
        BetterCombatHit hit = PENDING_HIT.get();
        if (hit == null || !(source.getEntity() instanceof ServerPlayer attacker)) return damage;
        if (!hit.owner().equals(attacker.getUUID())
                || hit.targetId() != victim.getId()
                || hit.tick() != attacker.tickCount) {
            return damage;
        }
        PENDING_HIT.remove();
        return damage * hit.multiplier();
    }

    public static void clearBetterCombatHit(Player player) {
        BetterCombatHit hit = PENDING_HIT.get();
        if (hit != null && hit.owner().equals(player.getUUID())) PENDING_HIT.remove();
    }

    private static boolean insideGeometry(ServerPlayer player, Entity target, BetterCombatGeometry geometry) {
        Vec3 origin = player.getEyePosition().subtract(0.0D, player.getBbHeight() * 0.15D * player.getAgeScale(), 0.0D);
        AABB box = target.getBoundingBox().inflate(target.getPickRadius());
        Vec3 closest = new Vec3(
                clamp(origin.x, box.minX, box.maxX),
                clamp(origin.y, box.minY, box.maxY),
                clamp(origin.z, box.minZ, box.maxZ)
        );
        Vec3 closestVector = closest.subtract(origin);
        Vec3 centerVector = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(origin);
        if (closestVector.lengthSqr() > geometry.range() * geometry.range()) return false;
        if (geometry.angle() >= 360.0D) return true;
        Vec3 look = player.getLookAngle();
        double maxDifference = geometry.angle() * 0.5D;
        return angleBetween(closestVector, look) <= maxDifference
                || angleBetween(centerVector, look) <= maxDifference;
    }

    private static AABB scaleHorizontally(AABB box, double scale) {
        if (!(scale > 1.000001D) || !Double.isFinite(scale)) return box;
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double halfX = box.getXsize() * 0.5D * scale;
        double halfZ = box.getZsize() * 0.5D * scale;
        return new AABB(centerX - halfX, box.minY, centerZ - halfZ,
                centerX + halfX, box.maxY, centerZ + halfZ);
    }

    private static double bodyScale(ServerPlayer player) {
        double scale = player.getAttributeValue(Attributes.SCALE);
        return Double.isFinite(scale) ? Math.max(1.0D, scale) : 1.0D;
    }

    private static double angleBetween(Vec3 a, Vec3 b) {
        double product = a.length() * b.length();
        if (product < 1.0E-7D) return 0.0D;
        double cosine = Math.max(-1.0D, Math.min(1.0D, a.dot(b) / product));
        return Math.toDegrees(Math.acos(cosine));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int level(ServerPlayer player) {
        int value = AugmentationPlayerStateService.state(player).level(ID);
        return Math.max(0, Math.min(3, value));
    }

    private static double rangeBonus(int level) {
        return RANGE_BONUS[Math.max(0, Math.min(3, level))];
    }

    private static double angleBonus(int level) {
        return ANGLE_BONUS[Math.max(0, Math.min(3, level))];
    }

    private static float secondaryDamageMultiplier(int level) {
        return SECONDARY_DAMAGE[Math.max(0, Math.min(3, level))];
    }

    private static Object currentBetterCombatAttack(Player player) {
        ensureBetterCombatReflection();
        if (getComboCount == null || getCurrentAttack == null || handAttack == null) return null;
        try {
            int combo = ((Number) getComboCount.invoke(player)).intValue();
            Object hand = getCurrentAttack.invoke(null, player, combo);
            return hand == null ? null : handAttack.invoke(hand);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void ensureBetterCombatReflection() {
        if (reflectionChecked) return;
        synchronized (SweepingEnhancementService.class) {
            if (reflectionChecked) return;
            reflectionChecked = true;
            try {
                Class<?> properties = Class.forName("net.bettercombat.logic.PlayerAttackProperties");
                Class<?> helper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
                Class<?> hand = Class.forName("net.bettercombat.api.AttackHand");
                Class<?> attack = Class.forName("net.bettercombat.api.WeaponAttributes$Attack");
                getComboCount = properties.getMethod("getComboCount");
                getCurrentAttack = helper.getMethod("getCurrentAttack", Player.class, int.class);
                handAttack = hand.getMethod("attack");
                attackAngle = attack.getMethod("angle");
                attackRangeMultiplier = attack.getMethod("rangeMultiplier");
            } catch (ReflectiveOperationException | LinkageError exception) {
                getComboCount = null;
                getCurrentAttack = null;
                handAttack = null;
                attackAngle = null;
                attackRangeMultiplier = null;
            }
        }
    }

    private static double invokeNumber(Method method, Object owner, double fallback) {
        if (method == null || owner == null) return fallback;
        try {
            Object result = method.invoke(owner);
            return result instanceof Number number ? number.doubleValue() : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return fallback;
        }
    }

    private record BetterCombatTarget(ServerPlayer player, Entity target, int tick, boolean direct) {
        BetterCombatTarget withDirect(boolean value) {
            return new BetterCombatTarget(player, target, tick, value);
        }
    }

    private record BetterCombatGeometry(UUID owner, int tick, double range, double angle) {
    }

    private record BetterCombatHit(UUID owner, int targetId, int tick, float multiplier) {
    }
}
