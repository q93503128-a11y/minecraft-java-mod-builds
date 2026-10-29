package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.network.ActiveAbilityNetworking;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respawn loadout sync is repeated after state-copy/network settlement so the HUD never stays blank. */
@Mixin(value = ActiveAbilityNetworking.class, remap = false)
abstract class ActiveLoadoutRespawnResyncMixin {
    @Unique private static final Map<UUID, Long> AUGMENTATION_SURVIVAL$PENDING_RESPAWN_SYNC = new HashMap<>();
    @Unique private static boolean augmentationSurvival$registered;

    @Inject(method = "initialize", at = @At("RETURN"))
    private static void augmentationSurvival$registerDelayedRespawnSync(CallbackInfo ci) {
        if (augmentationSurvival$registered) return;
        augmentationSurvival$registered = true;
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                AUGMENTATION_SURVIVAL$PENDING_RESPAWN_SYNC.put(
                        newPlayer.getUUID(),
                        newPlayer.level().getGameTime() + 2L
                )
        );
        ServerPlayerEvents.LEAVE.register(player -> AUGMENTATION_SURVIVAL$PENDING_RESPAWN_SYNC.remove(player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(ActiveLoadoutRespawnResyncMixin::augmentationSurvival$flushRespawnSync);
    }

    @Unique
    private static void augmentationSurvival$flushRespawnSync(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iterator = AUGMENTATION_SURVIVAL$PENDING_RESPAWN_SYNC.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.level().getGameTime() < entry.getValue()) continue;
            ActiveAbilityNetworking.syncNow(player);
            iterator.remove();
        }
    }
}
