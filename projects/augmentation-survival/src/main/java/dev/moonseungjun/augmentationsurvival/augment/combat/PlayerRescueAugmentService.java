package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Final co-op death-protection fallback after all personal/vanilla death protection has failed. */
public final class PlayerRescueAugmentService {
    public static final String RESCUE_ID = id("companion_rescue");
    private static final String DOWNED_UNTIL_KEY = RESCUE_ID + "#downed_until";
    private static final int DOWNED_TICKS = 10 * 20;
    private static final int RESCUE_CHANNEL_TICKS = 2 * 20;
    private static final int COOLDOWN_TICKS = 300 * 20;
    private static final double RESCUE_RANGE_SQUARED = 3.5D * 3.5D;
    private static final Map<UUID, DownedState> DOWNED = new HashMap<>();
    private static final Map<UUID, RescueChannel> CHANNELS = new HashMap<>();
    private static final Set<UUID> FORCED_DEATH = new HashSet<>();
    private static boolean initialized;

    private PlayerRescueAugmentService() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        AugmentCatalog.require(RESCUE_ID);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PlayerRescueAugmentService::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PlayerRescueAugmentService::afterDamage);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer rescuer)
                    || !(entity instanceof ServerPlayer target)) return InteractionResult.PASS;
            return tryBeginRescue(rescuer, target) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> restoreDownedState(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            DOWNED.remove(id);
            CHANNELS.remove(id);
            CHANNELS.entrySet().removeIf(entry -> entry.getValue().targetId.equals(id));
        });
        ServerTickEvents.END_SERVER_TICK.register(PlayerRescueAugmentService::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DOWNED.clear();
            CHANNELS.clear();
            FORCED_DEATH.clear();
        });
    }

    public static boolean isMultiplayerEligible(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        int players = 0;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (!candidate.isSpectator() && ++players >= 2) return true;
        }
        return false;
    }

    /** Called only after vanilla, Emergency Totem and Reincarnation have all returned false. */
    public static boolean tryPostVanillaDeathProtection(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player)
                || FORCED_DEATH.contains(player.getUUID())
                || source.typeHolder().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !owns(player)
                || !isMultiplayerEligible(player)) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        long now = player.level().getGameTime();
        if (state.cooldownReadyTick(RESCUE_ID) > now || isDowned(player)) return false;

        long expiresAt = now + DOWNED_TICKS;
        player.setHealth(1.0F);
        player.stopUsingItem();
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        AugmentationPlayerStateService.replace(player, state
                .withCooldownReadyTick(RESCUE_ID, now + COOLDOWN_TICKS)
                .withCooldownReadyTick(DOWNED_UNTIL_KEY, expiresAt));
        DOWNED.put(player.getUUID(), new DownedState(expiresAt));
        CHANNELS.entrySet().removeIf(entry -> entry.getKey().equals(player.getUUID())
                || entry.getValue().targetId.equals(player.getUUID()));
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = player.position().add(0.0D, 0.45D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 14, 0.45D, 0.25D, 0.45D, 0.025D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 0.65F, 0.55F);
        return true;
    }

    public static boolean isDowned(ServerPlayer player) { return DOWNED.containsKey(player.getUUID()); }
    public static boolean isActionLocked(ServerPlayer player) { return isDowned(player); }

    public static boolean tryBeginRescue(ServerPlayer rescuer, ServerPlayer target) {
        if (rescuer == target || rescuer.isDeadOrDying() || rescuer.isSpectator() || isDowned(rescuer)) return false;
        DownedState downed = DOWNED.get(target.getUUID());
        if (downed == null || target.isDeadOrDying() || rescuer.level() != target.level()) return false;
        if (rescuer.distanceToSqr(target) > RESCUE_RANGE_SQUARED) return false;
        CHANNELS.put(rescuer.getUUID(), new RescueChannel(target.getUUID(), rescuer.level().getGameTime() + RESCUE_CHANNEL_TICKS));
        ServerLevel level = (ServerLevel) rescuer.level();
        Vec3 at = target.position().add(0.0D, 0.65D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 5, 0.28D, 0.25D, 0.28D, 0.01D);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 0.82F);
        return true;
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        return !(entity instanceof ServerPlayer player) || !isDowned(player);
    }

    private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player && !blocked && damageTaken > 0.0F) CHANNELS.remove(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        ArrayList<ServerPlayer> timedOut = new ArrayList<>();
        Iterator<Map.Entry<UUID, DownedState>> downedIterator = DOWNED.entrySet().iterator();
        while (downedIterator.hasNext()) {
            Map.Entry<UUID, DownedState> entry = downedIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { downedIterator.remove(); continue; }
            long now = player.level().getGameTime();
            if (player.isDeadOrDying() || now >= entry.getValue().expiresAtTick) {
                downedIterator.remove();
                if (!player.isDeadOrDying()) timedOut.add(player);
                continue;
            }
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, Math.min(0.0D, velocity.y), 0.0D);
            player.hurtMarked = true;
            player.setSprinting(false);
            player.stopUsingItem();
            player.resetFallDistance();
            if (now % 10L == 0L && player.level() instanceof ServerLevel level) {
                Vec3 at = player.position().add(0.0D, 0.4D, 0.0D);
                level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 2, 0.24D, 0.12D, 0.24D, 0.004D);
            }
        }
        for (ServerPlayer player : timedOut) forceDeath(player);

        ArrayList<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, RescueChannel> entry : CHANNELS.entrySet()) {
            ServerPlayer rescuer = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getValue().targetId);
            if (!validChannel(rescuer, target)) { finished.add(entry.getKey()); continue; }
            long now = rescuer.level().getGameTime();
            if (now % 5L == 0L && target.level() instanceof ServerLevel level) {
                Vec3 from = rescuer.position().add(0.0D, 1.0D, 0.0D);
                Vec3 to = target.position().add(0.0D, 0.65D, 0.0D);
                for (int i = 1; i <= 3; i++) {
                    double t = i / 4.0D;
                    level.sendParticles(ParticleTypes.END_ROD, from.x + (to.x - from.x) * t,
                            from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (now >= entry.getValue().completeAtTick) {
                completeRescue(target);
                finished.add(entry.getKey());
            }
        }
        for (UUID rescuerId : finished) CHANNELS.remove(rescuerId);
    }

    private static boolean validChannel(ServerPlayer rescuer, ServerPlayer target) {
        return rescuer != null && target != null && !rescuer.isDeadOrDying() && !rescuer.isSpectator()
                && !isDowned(rescuer) && isDowned(target) && rescuer.level() == target.level()
                && rescuer.distanceToSqr(target) <= RESCUE_RANGE_SQUARED;
    }

    private static void completeRescue(ServerPlayer target) {
        DOWNED.remove(target.getUUID());
        CHANNELS.entrySet().removeIf(entry -> entry.getValue().targetId.equals(target.getUUID()));
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(target);
        AugmentationPlayerStateService.replace(target, latest.withCooldownReadyTick(DOWNED_UNTIL_KEY, 0L));
        target.setHealth(Math.max(1.0F, target.getMaxHealth() * 0.30F));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.resetFallDistance();
        ServerLevel level = (ServerLevel) target.level();
        Vec3 at = target.position().add(0.0D, 0.85D, 0.0D);
        level.sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, 8, 0.55D, 0.55D, 0.55D, 0.02D);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 10, 0.40D, 0.65D, 0.40D, 0.025D);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.65F, 1.20F);
    }

    private static void forceDeath(ServerPlayer player) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(player, latest.withCooldownReadyTick(DOWNED_UNTIL_KEY, 0L));
        UUID id = player.getUUID();
        FORCED_DEATH.add(id);
        try { player.hurtServer((ServerLevel) player.level(), player.damageSources().genericKill(), Float.MAX_VALUE); }
        finally { FORCED_DEATH.remove(id); }
    }

    private static void restoreDownedState(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        long stored = state.cooldownReadyTick(DOWNED_UNTIL_KEY);
        if (stored <= 0L) return;
        if (!owns(player)) {
            AugmentationPlayerStateService.replace(player, state.withCooldownReadyTick(DOWNED_UNTIL_KEY, 0L));
            return;
        }
        if (stored > player.level().getGameTime()) {
            DOWNED.put(player.getUUID(), new DownedState(stored));
            return;
        }
        forceDeath(player);
    }

    private static boolean owns(ServerPlayer player) { return AugmentationPlayerStateService.state(player).level(RESCUE_ID) > 0; }
    private static String id(String path) { return AugmentationSurvival.MOD_ID + ":" + path; }
    private record DownedState(long expiresAtTick) { }
    private record RescueChannel(UUID targetId, long completeAtTick) { }
}
