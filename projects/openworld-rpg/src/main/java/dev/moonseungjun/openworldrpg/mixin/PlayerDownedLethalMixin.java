package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDamageApplicationContext;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.multiplayer.ProjectDownedRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bridges Minecraft's final lethal-death decision into the server-owned Downed state.
 *
 * <p>Vanilla death protection has priority. Only a hit that has already reduced a server player to
 * lethal health and was not saved by vanilla death protection may enter the exact encounter Downed
 * branch. Damage types that bypass invulnerability remain hard defeat paths.</p>
 */
@Mixin(LivingEntity.class)
public abstract class PlayerDownedLethalMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void openworldRpg$suppressDamageDuringRescueWindow(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)
                || !ProjectDownedRuntime.isDowned(player)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        /*
         * A project-owned external actor may have opened a one-shot damage authorization before
         * reaching LivingEntity#hurtServer. Consume that token even though the Downed gate rejects
         * the hit, otherwise a cancelled rescue-window hit could leak authorization into a later
         * damage call on the same server thread.
         */
        if (source.getEntity() instanceof LivingEntity attacker
                && ExternalActorBindingRuntime.ownsDamageAuthority(attacker)) {
            ProjectDamageApplicationContext.consumeIfAuthorized(self);
        }
        cir.setReturnValue(false);
    }

    @Inject(
            method = "checkTotemDeathProtection",
            at = @At("RETURN"),
            cancellable = true
    )
    private void openworldRpg$enterDownedAtFinalLethalBoundary(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (Boolean.TRUE.equals(cir.getReturnValue())
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)
                || player.getHealth() > 0.0F) {
            return;
        }

        var entered =
                ProjectDownedRuntime
                        .tryEnterEncounterDownedAfterFinalLethal(player);
        if (entered.status()
                != ProjectDownedRuntime.EnterStatus.ENTERED) {
            return;
        }

        /*
         * Keep Minecraft's backing LivingEntity alive while the project Downed state owns the
         * rescue window. This 1 HP is not a player-facing revive amount; successful revive later
         * writes the canonical 35% HP / 50% Stamina / 25% Mana state.
         */
        player.setHealth(1.0F);
        cir.setReturnValue(true);
    }
}
