package dev.moonseungjun.augmentationsurvival.augment.combat;

import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Bridges the pinned MCEndgame 26.2 player-damage replacement.
 *
 * <p>MCEndgame replaces the damage argument inside {@code Player#actuallyHurt} with the result of
 * its own {@code DamageService.applyDamage(...)} call before applying vanilla absorption/HP. The
 * compatibility boundary therefore has to modify that calculated result, not the incoming
 * {@code actuallyHurt} argument.</p>
 */
public final class MCEndgameDamageCompatibility {
    private MCEndgameDamageCompatibility() {
    }

    public static boolean active() {
        // Do not cache this at class initialization. Mixin helper classes can be resolved before
        // every mod entrypoint has finished bootstrapping in development/GameTest environments.
        return FabricLoader.getInstance().isModLoaded("mcendgame");
    }

    /** Applies Augmentation Survival's final player layers to MCEndgame's post-reduction result. */
    public static Optional<Float> modifyCalculatedDamage(
            Player victim,
            ServerLevel level,
            DamageSource source,
            Optional<Float> calculatedDamage
    ) {
        if (!active()
                || !(victim instanceof ServerPlayer player)
                || calculatedDamage.isEmpty()
                || player.isInvulnerableTo(level, source)) {
            return calculatedDamage;
        }

        float damage = calculatedDamage.get();
        if (!(damage > 0.0F) || !Float.isFinite(damage)) {
            return calculatedDamage;
        }

        float adjusted;
        if (CombatProvenanceContext.isDelayedDamage()) {
            // Delayed installments were already mitigated when they were reserved. MCEndgame may
            // calculate them again while rebuilding its DamageSourceResult, so restore the exact
            // reserved installment before allowing current augment protection pools to consume it.
            adjusted = ProtectionAugmentService.modifyDelayedDamage(
                    player,
                    CombatProvenanceContext.delayedDamageOr(damage)
            );
        } else {
            adjusted = CombatAugmentService.modifyPostMitigationDamage(player, source, damage);
        }

        if (!Float.isFinite(adjusted)) {
            return calculatedDamage;
        }
        return Optional.of(Math.max(0.0F, adjusted));
    }
}
