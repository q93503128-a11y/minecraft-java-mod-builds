package io.github.q93503128.turnbound.mixin;

import io.github.q93503128.turnbound.TurnboundCheckpointService;
import io.github.q93503128.turnbound.TurnboundWorldGate;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla /kill can call Entity.kill directly without passing through
 * LivingEntity.damage/ALLOW_DEATH or ServerPlayerEntity.onDeath.
 * Do not allow a copied-world TURNBOUND explorer to be discarded.
 */
@Mixin(Entity.class)
public abstract class EntityKillMixin {
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void turnbound$handleDirectKill(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (!TurnboundWorldGate.active(player.getServer())) return;
        player.setHealth(player.getMaxHealth());
        TurnboundCheckpointService.returnAfterDefeat(player);
        ci.cancel();
    }
}
