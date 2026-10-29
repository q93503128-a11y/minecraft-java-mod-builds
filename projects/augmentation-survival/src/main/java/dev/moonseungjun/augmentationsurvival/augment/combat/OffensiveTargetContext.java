package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

/** Scoped target policy for player-owned offensive augments: anything attackable except allies. */
public final class OffensiveTargetContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private OffensiveTargetContext() { }

    public static void push() { DEPTH.set(DEPTH.get() + 1); }
    public static void pop() {
        int next = DEPTH.get() - 1;
        if (next <= 0) DEPTH.remove();
        else DEPTH.set(next);
    }
    public static boolean active() { return DEPTH.get() > 0; }

    public static boolean isValid(Entity source, LivingEntity candidate) {
        if (candidate == null || candidate == source || candidate.isDeadOrDying() || candidate.isRemoved()) return false;
        if (source != null) {
            if (CompanionAugmentService.areAllied(source, candidate)
                    || source.isAlliedTo(candidate)
                    || candidate.isAlliedTo(source)) return false;
            if (source instanceof ServerPlayer player && candidate instanceof TamableAnimal tame && tame.isTame()) {
                LivingEntity owner = tame.getOwner();
                if (owner != null && owner.getUUID().equals(player.getUUID())) return false;
            }
        }
        return true;
    }
}
