package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityNetworking;
import dev.moonseungjun.augmentationsurvival.network.ActiveLoadoutStatePayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retries early join/respawn loadout sync until the client play channel is actually ready. */
@Mixin(value = ActiveAbilityNetworking.class, remap = false)
abstract class ActiveLoadoutRespawnResyncMixin {
    @Unique private static final Map<UUID, PendingSync> AUGMENTATION_SURVIVAL$PENDING_SYNC = new HashMap<>();
    @Unique private static boolean augmentationSurvival$registered;

    @Inject(method = "initialize", at = @At("RETURN"))
    private static void augmentationSurvival$registerDelayedLoadoutSync(CallbackInfo ci) {
        if (augmentationSurvival$registered) return;
        augmentationSurvival$registered = true;
        ServerPlayerEvents.JOIN.register(player -> augmentationSurvival$schedule(player, 1L));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                augmentationSurvival$schedule(newPlayer, 2L)
        );
        ServerPlayerEvents.LEAVE.register(player -> AUGMENTATION_SURVIVAL$PENDING_SYNC.remove(player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(ActiveLoadoutRespawnResyncMixin::augmentationSurvival$flushPendingSync);
    }

    @Unique
    private static void augmentationSurvival$schedule(ServerPlayer player, long delayTicks) {
        long now = player.level().getGameTime();
        AUGMENTATION_SURVIVAL$PENDING_SYNC.put(
                player.getUUID(),
                new PendingSync(now + Math.max(1L, delayTicks), now + 100L)
        );
    }

    @Unique
    private static void augmentationSurvival$flushPendingSync(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingSync>> iterator = AUGMENTATION_SURVIVAL$PENDING_SYNC.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingSync> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            long now = player.level().getGameTime();
            PendingSync pending = entry.getValue();
            if (now < pending.nextAttemptTick()) continue;

            if (ServerPlayNetworking.canSend(player, ActiveLoadoutStatePayload.TYPE)) {
                ActiveAbilityNetworking.syncNow(player);
                iterator.remove();
                continue;
            }
            if (now >= pending.expiresAtTick()) {
                iterator.remove();
                continue;
            }
            entry.setValue(new PendingSync(now + 2L, pending.expiresAtTick()));
        }
    }

    @Unique
    private record PendingSync(long nextAttemptTick, long expiresAtTick) {
    }
}
