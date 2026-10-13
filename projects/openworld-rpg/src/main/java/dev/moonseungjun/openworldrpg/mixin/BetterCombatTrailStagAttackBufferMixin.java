package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.entity.TrailStagEntity;
import dev.moonseungjun.openworldrpg.mount.TrailStagBufferedAttackRuntime;
import dev.moonseungjun.openworldrpg.mount.TrailStagLiveBinding;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Better Combat packet-boundary bridge for Trail Stag's authored attack-to-dismount transition.
 *
 * <p>The donor request object is deliberately treated as opaque here. After a legal server-side
 * dismount it is replayed two ticks later through Better Combat's own server handler, preserving the
 * pinned donor combo/hand/range validation instead of reconstructing it inside Openworld RPG.</p>
 */
@Pseudo
@Mixin(targets = "net.bettercombat.network.ServerNetwork", remap = false)
public abstract class BetterCombatTrailStagAttackBufferMixin {
    @Inject(
            method = "handleAttackRequest",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void openworldRpg$bufferTrailStagAttackAfterSafeDismount(
            @Coerce Object request,
            MinecraftServer server,
            ServerPlayer player,
            ServerGamePacketListenerImpl handler,
            CallbackInfo ci
    ) {
        if (!(player.getVehicle() instanceof TrailStagEntity stag) || !stag.ownedBy(player)) {
            return;
        }

        // Stop the mounted donor request first. If no safe separation exists, canon requires the
        // attack to be rejected while the rider remains mounted.
        ci.cancel();
        if (!TrailStagLiveBinding.requestSafeDismount(player, stag)) {
            return;
        }

        // One request only. A duplicate pending buffer fails closed rather than executing twice.
        TrailStagBufferedAttackRuntime.buffer(request, server, player, handler);
    }
}
