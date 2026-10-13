package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies authored negative healing-received modifiers at Minecraft's final heal boundary. */
@Mixin(LivingEntity.class)
public abstract class PlayerPoisonHealingReceivedMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float openworldRpg$applyViperPoisonHealingReceived(float amount) {
        if (!((Object) this instanceof ServerPlayer player) || amount <= 0.0F) {
            return amount;
        }
        return (float) (amount * R01MeadowViperPoisonRuntime.healingReceivedMultiplier(
                player,
                player.level().getGameTime()
        ));
    }
}
