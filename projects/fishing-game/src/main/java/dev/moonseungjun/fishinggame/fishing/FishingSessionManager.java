package dev.moonseungjun.fishinggame.fishing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.network.ProfileSnapshotPayload;
import dev.moonseungjun.fishinggame.profile.CatchEntry;
import dev.moonseungjun.fishinggame.profile.FishingProfiles;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.progression.RodDefinition;
import dev.moonseungjun.fishinggame.world.FishingWorldManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class FishingSessionManager {
    private static final int HOOK_WATER_TIMEOUT_TICKS = 60;
    private static final int WATER_EXIT_GRACE_TICKS = 8;
    private static final int VISUAL_APPROACH_TICKS = 32;
    private static final double VISUAL_START_DISTANCE = 5.4;
    private static final double VISUAL_HOOK_DISTANCE = 0.55;
    private static final Map<UUID, FishingSession> SESSIONS = new HashMap<>();

    private FishingSessionManager() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!player.getItemInHand(hand).is(Items.FISHING_ROD)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            FishingSession session = SESSIONS.get(serverPlayer.getUUID());
            if (session == null) {
                if (FishingProfiles.get(serverPlayer).bagFull()) {
                    sendIdle(serverPlayer, "어획 가방이 가득 찼습니다. B에서 판매해 주세요.");
                    return InteractionResult.SUCCESS;
                }
                beginCast(serverPlayer);
                return InteractionResult.PASS;
            }

            if (session.stage == FishingStage.HOOKED) {
                return InteractionResult.SUCCESS;
            }

            cancel(serverPlayer, "낚싯줄을 거뒀습니다.");
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(FishingSessionManager::tick);
        ServerPlayerEvents.LEAVE.register(player -> {
            FishingSession session = SESSIONS.remove(player.getUUID());
            discardVisualFish(session);
        });
    }

    public static void setReelHeld(ServerPlayer player, boolean held) {
        FishingSession session = SESSIONS.get(player.getUUID());
        if (session != null && session.stage == FishingStage.HOOKED) {
            session.reelHeld = held;
        }
    }

    public static void syncProfile(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ProfileSnapshotPayload(FishingProfiles.get(player)));
    }

    public static void syncIdle(ServerPlayer player) {
        sendIdle(player, "");
    }

    public static void sellAll(ServerPlayer player) {
        PlayerFishingProfile current = FishingProfiles.get(player);
        int soldFor = current.bagValue();
        if (soldFor <= 0) {
            sendIdle(player, "판매할 물고기가 없습니다.");
            return;
        }
        PlayerFishingProfile next = current.sellAll();
        FishingProfiles.set(player, next);
        syncProfile(player);
        sendIdle(player, soldFor + " 코인에 모두 판매했습니다.");
    }

    public static void buyRod(ServerPlayer player, int requestedTier) {
        PlayerFishingProfile current = FishingProfiles.get(player);
        RodDefinition nextRod = FishingRods.nextAfter(current.rodTier());
        if (nextRod == null) {
            sendIdle(player, "이미 가장 좋은 낚싯대를 사용 중입니다.");
            return;
        }
        if (requestedTier != nextRod.tier()) return;
        if (current.coins() < nextRod.price()) {
            sendIdle(player, "코인이 부족합니다. 필요: " + nextRod.price());
            return;
        }
        PlayerFishingProfile next = current.withRodTierAndCoins(nextRod.tier(), current.coins() - nextRod.price());
        FishingProfiles.set(player, next);
        syncProfile(player);
        sendIdle(player, nextRod.displayName() + "을(를) 장착했습니다.");
    }

    private static void beginCast(ServerPlayer player) {
        long now = player.level().getGameTime();
        FishingLocation location = FishingWorldManager.locationFor(player);
        FishingSession session = new FishingSession(now, location);
        SESSIONS.put(player.getUUID(), session);
        sendState(player, session, "");
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, FishingSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FishingSession> entry = iterator.next();
            FishingSession session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                discardVisualFish(session);
                iterator.remove();
                continue;
            }

            long now = player.level().getGameTime();

            if (session.stage == FishingStage.WAITING_FOR_HOOK) {
                if (isHookAtFishingWater(player)) {
                    PlayerFishingProfile profile = FishingProfiles.get(player);
                    RodDefinition rod = FishingRods.byTier(profile.rodTier());
                    session.species = FishCatalog.pick(session.location, player.getRandom().nextDouble(), rod.luck());
                    session.stage = FishingStage.WAITING_FOR_BITE;
                    session.biteTick = now + nextBiteDelay(player);
                    session.visualStartTick = Math.max(now + 5, session.biteTick - VISUAL_APPROACH_TICKS);
                    session.visualAngle = player.getRandom().nextDouble() * Math.PI * 2.0;
                    session.dryTicks = 0;
                    sendState(player, session, "");
                } else if (player.fishing != null && now - session.castTick > HOOK_WATER_TIMEOUT_TICKS) {
                    finish(iterator, session, player, "물에 찌를 던져 주세요.", true);
                } else if (player.fishing == null && now - session.castTick > 12) {
                    finish(iterator, session, player, "캐스팅하지 못했습니다.", false);
                }
                continue;
            }

            if (player.fishing == null) {
                finish(iterator, session, player, "낚싯줄이 풀렸습니다.", false);
                continue;
            }

            if (!isHookAtFishingWater(player)) {
                session.dryTicks++;
                if (session.dryTicks > WATER_EXIT_GRACE_TICKS) {
                    finish(iterator, session, player, "찌가 물 밖으로 나왔습니다.", true);
                }
                continue;
            }
            session.dryTicks = 0;

            if (session.stage == FishingStage.WAITING_FOR_BITE) {
                if (now >= session.visualStartTick) {
                    updateApproachVisual(player, session, now);
                }
                if (now >= session.biteTick) {
                    ensureVisualFish(player, session);
                    session.stage = FishingStage.HOOKED;
                    session.tension = 0.42f;
                    session.progress = 0.0f;
                    session.reelHeld = false;
                    session.burstTicks = 0;
                    session.burstStrength = 0.0f;
                    session.nextBurstTick = now + 16 + player.getRandom().nextInt(22);
                    updateHookedVisual(player, session, now);
                    playBiteFx(player);
                    sendState(player, session, "입질! 안전 구간을 지키며 감아 올리세요.");
                }
                continue;
            }

            tickHooked(player, session, iterator, now);
        }
    }

    private static boolean isHookAtFishingWater(ServerPlayer player) {
        if (player.fishing == null) return false;
        if (player.fishing.isInWater()) return true;
        var hookPos = player.fishing.blockPosition();
        var level = player.fishing.level();
        return level.getFluidState(hookPos).is(FluidTags.WATER)
                || level.getFluidState(hookPos.below()).is(FluidTags.WATER);
    }

    private static int nextBiteDelay(ServerPlayer player) {
        RodDefinition rod = FishingRods.byTier(FishingProfiles.get(player).rodTier());
        int base = 50 + player.getRandom().nextInt(71);
        return Math.max(32, Math.round(base * rod.lureTimeMultiplier()));
    }

    private static void tickHooked(ServerPlayer player, FishingSession session, Iterator<?> iterator, long now) {
        PlayerFishingProfile profile = FishingProfiles.get(player);
        RodDefinition rod = FishingRods.byTier(profile.rodTier());
        float resistance = session.species.resistance();

        session.tension = ReelMath.updateTension(session.tension, session.reelHeld, resistance, rod.strength());
        updateFightBurst(player, session, rod, now);
        session.progress = ReelMath.updateProgress(session.progress, session.tension, resistance, rod.controlBonus());
        updateHookedVisual(player, session, now);

        if (ReelMath.isLineBroken(session.tension)) {
            finish(iterator, session, player, session.species.displayName() + "을(를) 놓쳤습니다.", true);
            return;
        }

        if (session.progress >= 1.0f) {
            double weight = session.species.rollWeight(player.getRandom().nextDouble());
            double length = session.species.rollLength(player.getRandom().nextDouble());
            CatchEntry caught = new CatchEntry(
                    session.species.id(),
                    Math.max(1, (int) Math.round(weight * 1000.0)),
                    Math.max(1, (int) Math.round(length * 10.0)),
                    session.species.valueFor(weight)
            );
            PlayerFishingProfile updated = profile.addCatch(caught);
            FishingProfiles.set(player, updated);
            syncProfile(player);
            playCatchFx(player, session);
            finish(iterator, session, player, String.format("%s %.2f kg 포획! 가방 %d/%d", session.species.displayName(), weight, updated.catches().size(), PlayerFishingProfile.BAG_CAPACITY), true);
            return;
        }

        if (--session.hudCooldown <= 0) {
            session.hudCooldown = 4;
            sendState(player, session, "");
        }
    }

    private static void updateFightBurst(ServerPlayer player, FishingSession session, RodDefinition rod, long now) {
        if (session.burstTicks > 0) {
            session.burstTicks--;
            if (session.burstTicks == 0) session.burstStrength = 0.0f;
            return;
        }
        if (now < session.nextBurstTick) return;

        float resistance = session.species.resistance();
        session.burstStrength = 0.55f
                + Math.min(0.75f, Math.max(0.0f, resistance - 0.65f) * 0.55f)
                + player.getRandom().nextFloat() * 0.22f;
        session.burstTicks = 6 + Math.min(8, Math.round(resistance * 4.0f));
        session.burstHeading = player.getRandom().nextDouble() * Math.PI * 2.0;
        int cooldown = Math.max(22, 52 - Math.round(resistance * 10.0f));
        session.nextBurstTick = now + cooldown + player.getRandom().nextInt(22);
        session.tension = FishPresentationMath.burstPull(session.tension, resistance, rod.strength(), session.burstStrength);

        if (session.visualFish != null && !session.visualFish.isRemoved()) {
            ServerLevel level = player.level();
            Vec3 pos = session.visualFish.position();
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y + 0.15, pos.z, 6, 0.28, 0.12, 0.28, 0.035);
        }
    }

    private static void updateApproachVisual(ServerPlayer player, FishingSession session, long now) {
        if (player.fishing == null || session.species == null) return;
        ensureVisualFish(player, session);
        if (session.visualFish == null) return;

        double duration = Math.max(1.0, session.biteTick - session.visualStartTick);
        double t = clamp01((now - session.visualStartTick) / duration);
        double radius = FishPresentationMath.approachRadius(t, VISUAL_START_DISTANCE, VISUAL_HOOK_DISTANCE);
        double weave = FishPresentationMath.approachWeave(t, session.visualAngle * 0.7);
        double forwardX = Math.cos(session.visualAngle);
        double forwardZ = Math.sin(session.visualAngle);
        double sideX = -forwardZ;
        double sideZ = forwardX;
        Vec3 hook = player.fishing.position();

        double x = hook.x + forwardX * radius + sideX * weave;
        double y = hook.y - 1.18 + t * 0.34 + Math.sin(now * 0.23 + session.visualAngle) * 0.08;
        double z = hook.z + forwardZ * radius + sideZ * weave;
        moveVisualFish(session.visualFish, x, y, z, hook.x, hook.y - 0.75, hook.z);

        if ((now - session.visualStartTick) % 4 == 0 && t > 0.20) {
            player.level().sendParticles(ParticleTypes.BUBBLE, x, y + 0.10, z, 2, 0.10, 0.06, 0.10, 0.015);
        }
    }

    private static void updateHookedVisual(ServerPlayer player, FishingSession session, long now) {
        if (player.fishing == null || session.species == null) return;
        ensureVisualFish(player, session);
        if (session.visualFish == null) return;

        Vec3 hook = player.fishing.position();
        float burst = session.burstTicks > 0 ? session.burstStrength : 0.0f;
        double radius = FishPresentationMath.fightRadius(session.progress, session.species.resistance(), burst, session.tension);
        double calmAngle = session.visualAngle
                + Math.sin(now * 0.064 + session.visualAngle) * 1.05
                + Math.sin(now * 0.021 + session.visualAngle * 0.5) * 0.46;
        double angle = burst > 0.0f
                ? session.burstHeading + Math.sin(now * 0.31) * 0.24
                : calmAngle;

        double x = hook.x + Math.cos(angle) * radius;
        double y = hook.y - 1.02 + session.progress * 0.50 + Math.sin(now * 0.22 + session.visualAngle) * 0.11;
        if (burst > 0.0f) y -= 0.10 * burst;
        double z = hook.z + Math.sin(angle) * radius;
        moveVisualFish(session.visualFish, x, y, z, hook.x, hook.y - 0.45, hook.z);
    }

    private static void ensureVisualFish(ServerPlayer player, FishingSession session) {
        if (session.visualFish != null && !session.visualFish.isRemoved()) return;
        if (session.species == null || player.fishing == null) return;

        ServerLevel level = player.level();
        Mob fish = createVisualFish(level, session.species);
        if (fish == null) return;

        fish.setNoAi(true);
        fish.setNoGravity(true);
        fish.setInvulnerable(true);
        fish.setSilent(true);
        fish.setPersistenceRequired();
        fish.setDeltaMovement(Vec3.ZERO);
        var scaleAttribute = fish.getAttribute(Attributes.SCALE);
        if (scaleAttribute != null) {
            scaleAttribute.setBaseValue(FishPresentationMath.scaleFor(session.species));
        }

        Vec3 hook = player.fishing.position();
        double x = hook.x + Math.cos(session.visualAngle) * VISUAL_START_DISTANCE;
        double y = hook.y - 1.18;
        double z = hook.z + Math.sin(session.visualAngle) * VISUAL_START_DISTANCE;
        fish.setPos(x, y, z);
        level.addFreshEntity(fish);
        session.visualFish = fish;
        moveVisualFish(fish, x, y, z, hook.x, hook.y - 0.75, hook.z);
    }

    private static Mob createVisualFish(ServerLevel level, FishSpecies species) {
        return switch (species.id()) {
            case "bluegill" -> EntityTypes.TROPICAL_FISH.create(level, EntitySpawnReason.EVENT);
            case "trout", "largemouth", "salmon", "tuna", "oarfish", "ancient_sturgeon" ->
                    EntityTypes.SALMON.create(level, EntitySpawnReason.EVENT);
            default -> EntityTypes.COD.create(level, EntitySpawnReason.EVENT);
        };
    }

    private static void moveVisualFish(Mob fish, double x, double y, double z, double targetX, double targetY, double targetZ) {
        Vec3 previous = fish.position();
        Vec3 next = new Vec3(x, y, z);
        Vec3 motion = next.subtract(previous);
        fish.setDeltaMovement(motion);
        fish.setPos(x, y, z);

        double aimX = motion.horizontalDistanceSqr() > 0.0001 ? motion.x : targetX - x;
        double aimZ = motion.horizontalDistanceSqr() > 0.0001 ? motion.z : targetZ - z;
        double horizontal = Math.sqrt(aimX * aimX + aimZ * aimZ);
        double aimY = Math.abs(motion.y) > 0.001 ? motion.y : targetY - y;
        float yaw = (float) (Math.toDegrees(Math.atan2(aimZ, aimX)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(aimY, Math.max(0.001, horizontal))));
        pitch = Math.max(-28.0f, Math.min(28.0f, pitch));
        fish.setYRot(yaw);
        fish.setYBodyRot(yaw);
        fish.setYHeadRot(yaw);
        fish.setXRot(pitch);
    }

    private static void playBiteFx(ServerPlayer player) {
        if (player.fishing == null) return;
        ServerLevel level = player.level();
        Vec3 hook = player.fishing.position();
        level.sendParticles(ParticleTypes.SPLASH, hook.x, hook.y + 0.12, hook.z, 8, 0.32, 0.05, 0.32, 0.16);
        level.sendParticles(ParticleTypes.BUBBLE, hook.x, hook.y - 0.22, hook.z, 6, 0.24, 0.12, 0.24, 0.04);
        level.playSound(null, hook.x, hook.y, hook.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS,
                0.65f, 0.92f + player.getRandom().nextFloat() * 0.16f);
    }

    private static void playCatchFx(ServerPlayer player, FishingSession session) {
        ServerLevel level = player.level();
        Vec3 pos = session.visualFish != null && !session.visualFish.isRemoved()
                ? session.visualFish.position()
                : player.position();
        level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.25, pos.z, 10, 0.35, 0.18, 0.35, 0.18);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7f, 1.28f);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void cancel(ServerPlayer player, String message) {
        FishingSession session = SESSIONS.remove(player.getUUID());
        discardVisualFish(session);
        removeHook(player);
        sendIdle(player, message);
    }

    private static void finish(Iterator<?> iterator, FishingSession session, ServerPlayer player, String message, boolean removeFishingHook) {
        discardVisualFish(session);
        if (removeFishingHook) removeHook(player);
        iterator.remove();
        sendIdle(player, message);
    }

    private static void discardVisualFish(FishingSession session) {
        if (session != null && session.visualFish != null) {
            session.visualFish.discard();
            session.visualFish = null;
        }
    }

    private static void sendState(ServerPlayer player, FishingSession session, String notice) {
        String species = session.stage == FishingStage.HOOKED && session.species != null
                ? session.species.displayName()
                : "";
        ServerPlayNetworking.send(player, new FishingStatePayload(
                session.stage.ordinal(), session.tension, session.progress,
                species, session.location.displayName(), notice
        ));
    }

    private static void sendIdle(ServerPlayer player, String notice) {
        ServerPlayNetworking.send(player, new FishingStatePayload(
                FishingStage.values().length, 0.0f, 0.0f,
                "", FishingWorldManager.locationFor(player).displayName(), notice
        ));
    }

    private static void removeHook(ServerPlayer player) {
        if (player.fishing != null) {
            player.fishing.discard();
            player.fishing = null;
        }
    }
}
