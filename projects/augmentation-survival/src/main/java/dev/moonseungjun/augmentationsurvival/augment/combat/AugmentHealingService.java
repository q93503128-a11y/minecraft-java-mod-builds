package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

/** Server-owned entry point for non-natural healing and external heal interception. */
public final class AugmentHealingService {
    private static final String HEALING_AMPLIFICATION_ID = id("healing_amplification");
    private static final String OVERFLOW_HEALING_ID = id("overflow_healing");
    private static final String HEALING_SHARE_ID = id("healing_share");
    private static final ThreadLocal<Integer> MANAGED_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> NATURAL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<ExternalHealFrame> EXTERNAL_FRAME = new ThreadLocal<>();

    private AugmentHealingService() {
    }

    public static float healPrimary(ServerPlayer player, float baseAmount) {
        if (!(baseAmount > 0.0F) || !Float.isFinite(baseAmount) || player.isDeadOrDying()) return 0.0F;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        float amplified = amplify(state, baseAmount);
        float before = player.getHealth();
        runManaged(() -> player.heal(amplified));
        float actualHp = Math.max(0.0F, player.getHealth() - before);
        applyOverflow(player, state, Math.max(0.0F, amplified - actualHp));
        afterPrimaryHealing(player, state, actualHp);
        return actualHp;
    }

    /** Shared heals may use the recipient's amplification, but never overflow/share/heal waves again. */
    public static float healShared(LivingEntity target, float baseAmount) {
        if (!(baseAmount > 0.0F) || !Float.isFinite(baseAmount) || target.isDeadOrDying()) return 0.0F;
        float amount = baseAmount;
        if (target instanceof ServerPlayer player) amount = amplify(AugmentationPlayerStateService.state(player), baseAmount);
        float before = target.getHealth();
        float finalAmount = amount;
        runManaged(() -> target.heal(finalAmount));
        return Math.max(0.0F, target.getHealth() - before);
    }

    /** Called by the generic LivingEntity heal mixin before an unowned heal invocation. */
    public static float interceptExternalHealAmount(LivingEntity target, float baseAmount) {
        if (!(target instanceof ServerPlayer player)
                || !(baseAmount > 0.0F)
                || !Float.isFinite(baseAmount)
                || player.isDeadOrDying()
                || MANAGED_DEPTH.get() > 0
                || NATURAL_DEPTH.get() > 0) {
            return baseAmount;
        }
        EXTERNAL_FRAME.remove();
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        float amplified = amplify(state, baseAmount);
        EXTERNAL_FRAME.set(new ExternalHealFrame(player, state, amplified, player.getHealth()));
        return amplified;
    }

    /** Completes overflow/share/heal-trigger semantics once after an external non-natural heal. */
    public static void finishExternalHeal(LivingEntity target) {
        ExternalHealFrame frame = EXTERNAL_FRAME.get();
        if (frame == null || frame.player != target) return;
        EXTERNAL_FRAME.remove();
        float actualHp = Math.max(0.0F, frame.player.getHealth() - frame.healthBefore);
        applyOverflow(frame.player, frame.state, Math.max(0.0F, frame.amplifiedAmount - actualHp));
        afterPrimaryHealing(frame.player, frame.state, actualHp);
    }

    public static void beginNaturalHealingContext() {
        NATURAL_DEPTH.set(NATURAL_DEPTH.get() + 1);
    }

    public static void endNaturalHealingContext() {
        NATURAL_DEPTH.set(Math.max(0, NATURAL_DEPTH.get() - 1));
    }

    private static void afterPrimaryHealing(ServerPlayer player, AugmentationPlayerState state, float actualHp) {
        if (!(actualHp > 0.0F)) return;
        int shareLevel = boundedLevel(state, HEALING_SHARE_ID);
        if (shareLevel > 0) shareActualHealing(player, actualHp, shareLevel);
        CombatExpansionService.onPrimaryHealing(player, actualHp);
    }

    private static void applyOverflow(ServerPlayer player, AugmentationPlayerState state, float overflow) {
        int overflowLevel = boundedLevel(state, OVERFLOW_HEALING_ID);
        if (overflowLevel <= 0 || !(overflow > 0.0F)) return;
        float cap = (float) (CombatTuning.current().overflowHealingAbsorptionCapPerLevel() * overflowLevel);
        ProtectionAugmentService.grantAbsorption(player, ProtectionAugmentService.OVERFLOW_HEALING_POOL, overflow, cap);
    }

    private static void shareActualHealing(ServerPlayer source, float actualHp, int shareLevel) {
        CombatTuning.Snapshot tuning = CombatTuning.current();
        float sharedAmount = (float) (actualHp * tuning.healingSharePerLevel() * shareLevel);
        if (!(sharedAmount > 0.0F)) return;
        double radius = tuning.healingShareRadius();
        double radiusSquared = radius * radius;
        ServerLevel level = (ServerLevel) source.level();
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                source.getBoundingBox().inflate(radius),
                candidate -> candidate != source
                        && !candidate.isDeadOrDying()
                        && source.distanceToSqr(candidate) <= radiusSquared
                        && isHealingAlly(source, candidate)
        );
        for (LivingEntity target : nearby) healShared(target, sharedAmount);
    }

    public static boolean isHealingAlly(ServerPlayer source, LivingEntity candidate) {
        if (source.isAlliedTo(candidate) || CompanionAugmentService.areAllied(source, candidate)) return true;
        return candidate instanceof TamableAnimal tamable && tamable.isTame() && tamable.isOwnedBy(source);
    }

    private static float amplify(AugmentationPlayerState state, float baseAmount) {
        int level = boundedLevel(state, HEALING_AMPLIFICATION_ID);
        double multiplier = 1.0D + CombatTuning.current().healingAmplificationPerLevel() * level;
        double amplified = baseAmount * multiplier;
        if (!Double.isFinite(amplified)) return baseAmount;
        return (float) Math.min(Float.MAX_VALUE, amplified);
    }

    private static void runManaged(Runnable action) {
        MANAGED_DEPTH.set(MANAGED_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            MANAGED_DEPTH.set(Math.max(0, MANAGED_DEPTH.get() - 1));
        }
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }

    private record ExternalHealFrame(ServerPlayer player, AugmentationPlayerState state, float amplifiedAmount, float healthBefore) { }
}
