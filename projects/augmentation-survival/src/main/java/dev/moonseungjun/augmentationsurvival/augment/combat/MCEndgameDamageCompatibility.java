package dev.moonseungjun.augmentationsurvival.augment.combat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Bridges the pinned MCEndgame 26.2 player-damage replacement.
 *
 * MCEndgame handles Player#actuallyHurt at HEAD, applies absorption/HP itself, then cancels the
 * vanilla body. That means the normal post-magic damage hook is unreachable while MCEndgame is
 * loaded. This bridge moves only Augmentation Survival's final player-damage layers to the input
 * MCEndgame actually consumes; every other runtime keeps the normal post-magic path.
 */
public final class MCEndgameDamageCompatibility {
    private static final boolean ACTIVE = FabricLoader.getInstance().isModLoaded("mcendgame");

    private MCEndgameDamageCompatibility() {
    }

    public static boolean active() {
        return ACTIVE;
    }

    public static float modifyActuallyHurtInput(
            Player victim,
            ServerLevel level,
            DamageSource source,
            float damage
    ) {
        if (!ACTIVE
                || !(victim instanceof ServerPlayer player)
                || !(damage > 0.0F)
                || !Float.isFinite(damage)
                || player.isInvulnerableTo(level, source)) {
            return damage;
        }

        if (CombatProvenanceContext.isDelayedDamage()) {
            return ProtectionAugmentService.modifyDelayedDamage(
                    player,
                    CombatProvenanceContext.delayedDamageOr(damage)
            );
        }

        return CombatAugmentService.modifyPostMitigationDamage(player, source, damage);
    }
}
