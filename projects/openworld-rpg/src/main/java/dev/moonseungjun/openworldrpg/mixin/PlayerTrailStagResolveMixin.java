package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Consumes post-mitigation player damage into Trail Stag Resolve exactly once. */
@Mixin(Player.class)
public abstract class PlayerTrailStagResolveMixin {
    @ModifyVariable(
            method = "actuallyHurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float openworldRpg$consumeTrailStagResolve(float finalDamage) {
        Player self = (Player) (Object) this;
        if (!(self instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.getVehicle() instanceof TrailStagEntity stag)
                || !stag.ownedBy(serverPlayer)) {
            return finalDamage;
        }
        return stag.absorbResolvedDamage(serverPlayer, finalDamage);
    }
}
