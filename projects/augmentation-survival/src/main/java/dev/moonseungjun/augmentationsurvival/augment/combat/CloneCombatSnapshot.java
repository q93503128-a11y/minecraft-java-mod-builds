package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Activation-time offensive snapshot used by Clone without creating player kill provenance. */
public final class CloneCombatSnapshot {
    private static final String KEEN_EDGE_ID = id("keen_edge");
    private static final String LAST_STRENGTH_ID = id("last_strength");
    private static final String SPEED_CONVERSION_ID = id("speed_conversion");
    private static final String BRAWL_ID = id("brawl");
    private static final String GLASS_CANNON_ID = id("glass_cannon");

    private CloneCombatSnapshot() { }

    public static double meleeDamage(ServerPlayer player) {
        double damage = Math.max(0.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (!(damage > 0.0D) || !Double.isFinite(damage)) return 0.0D;

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        CombatTuning.Snapshot combat = CombatTuning.current();
        double bonus = combat.keenEdgePerLevel() * boundedLevel(state, KEEN_EDGE_ID);
        if (player.getMaxHealth() > 0.0F
                && player.getHealth() / player.getMaxHealth() <= combat.lastStrengthHealthThreshold()) {
            bonus += combat.lastStrengthPerLevel() * boundedLevel(state, LAST_STRENGTH_ID);
        }

        ExpansionTuning.Snapshot expansion = ExpansionTuning.current();
        int speedConversion = boundedLevel(state, SPEED_CONVERSION_ID);
        if (speedConversion > 0) {
            AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null && movement.getBaseValue() > 0.0D) {
                double positiveRatio = Math.max(0.0D, movement.getValue() / movement.getBaseValue() - 1.0D);
                if (player.isSprinting()) {
                    positiveRatio = Math.max(0.0D, positiveRatio - expansion.sprintBonusExclusion());
                }
                double movementPercent = positiveRatio * 100.0D;
                bonus += Math.min(
                        expansion.speedConversionDamageCap(speedConversion),
                        movementPercent * expansion.speedConversionDamagePerMovementPercent(speedConversion)
                );
            }
        }

        int brawl = boundedLevel(state, BRAWL_ID);
        if (brawl > 0) {
            int nearby = countNearbyEnemies(player, expansion.brawlRadius(), expansion.brawlMaxTargets());
            bonus += expansion.brawlDamagePerEnemy(brawl) * nearby;
        }

        int glassCannon = boundedLevel(state, GLASS_CANNON_ID);
        if (glassCannon > 0) bonus += expansion.glassCannonDamageBonus(glassCannon);
        bonus += ActiveAbilityService.berserkOutgoingBonus(player);

        double result = damage * Math.max(0.0D, 1.0D + bonus);
        return Double.isFinite(result) ? result : damage;
    }

    private static int countNearbyEnemies(ServerPlayer player, double radius, int cap) {
        double radiusSquared = radius * radius;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        return Math.min(cap, candidates.size());
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
