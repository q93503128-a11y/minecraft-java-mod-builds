package io.github.q93503128.turnbound.mixin;

import io.github.q93503128.turnbound.TurnboundCheckpointService;
import io.github.q93503128.turnbound.TurnboundWorldGate;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * /kill or a third-party command can call onDeath directly, bypassing Fabric's
 * fatal-DAMAGE event. Active TURNBOUND explorers have no vanilla death drops.
 * Never intercept death on an unmarked world.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void turnbound$cancelVanillaDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!TurnboundWorldGate.active(player.getServer())) return;
        player.setHealth(player.getMaxHealth());
        TurnboundCheckpointService.returnAfterDefeat(player);
        ci.cancel();
    }
}
