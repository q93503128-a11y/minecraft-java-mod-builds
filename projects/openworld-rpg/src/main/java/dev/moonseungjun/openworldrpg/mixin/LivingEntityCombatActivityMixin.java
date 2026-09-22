package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCombatActivityMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void openworldRpg$markPlayerCombatActivity(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (amount <= 0.0F || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        long gameTick = level.getGameTime();
        var sourceEntity = source.getEntity();

        if (target instanceof Player player
                && sourceEntity instanceof LivingEntity attacker
                && attacker != player) {
            CombatStateServices.markHostileHpActivity(player.getUUID(), gameTick);
        }

        if (sourceEntity instanceof Player player && target != player) {
            CombatStateServices.markHostileHpActivity(player.getUUID(), gameTick);
        }
    }
}
