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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class FishingSessionManager {
    private static final int HOOK_WATER_TIMEOUT_TICKS = 60;
    private static final int WATER_EXIT_GRACE_TICKS = 8;
    private static final int VISUAL_APPROACH_TICKS = 26;
    private static final double VISUAL_START_DISTANCE = 4.6;
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
                    updateHookedVisual(player, session, now);
                    sendState(player, session, "입질! 우클릭을 누르고 떼며 장력을 유지하세요.");
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
            finish(iterator, session, player, String.format("%s %.2f kg 포획! 가방 %d/%d", session.species.displayName(), weight, updated.catches().size(), PlayerFishingProfile.BAG_CAPACITY), true);
            return;
        }

        if (--session.hudCooldown <= 0) {
            session.hudCooldown = 4;
            sendState(player, session, "");
        }
    }

    private static void updateApproachVisual(ServerPlayer player, FishingSession session, long now) {
        if (player.fishing == null || session.species == null) return;
        ensureVisualFish(player, session);
        if (session.visualFish == null) return;

        double duration = Math.max(1.0, session.biteTick - session.visualStartTick);
        double t = clamp01((now - session.visualStartTick) / duration);
        double eased = 1.0 - Math.pow(1.0 - t, 2.0);
        double radius = VISUAL_START_DISTANCE + (VISUAL_HOOK_DISTANCE - VISUAL_START_DISTANCE) * eased;
        double angle = session.visualAngle + eased * 1.3 + (now - session.visualStartTick) * 0.025;
        Vec3 hook = player.fishing.position();
        double x = hook.x + Math.cos(angle) * radius;
        double y = hook.y - 0.85 + Math.sin(now * 0.27) * 0.10;
        double z = hook.z + Math.sin(angle) * radius;
        moveVisualFish(session.visualFish, x, y, z, hook.x, hook.z);
    }

    private static void updateHookedVisual(ServerPlayer player, FishingSession session, long now) {
        if (player.fishing == null || session.species == null) return;
        ensureVisualFish(player, session);
        if (session.visualFish == null) return;

        Vec3 hook = player.fishing.position();
        double resistance = Math.max(0.65, session.species.resistance());
        double fightRadius = 0.55 + (1.0 - session.progress) * 1.85;
        double angle = session.visualAngle + now * (0.075 + resistance * 0.025);
        double tensionKick = Math.max(0.0, session.tension - ReelMath.SAFE_MAX) * 1.3;
        double radius = fightRadius + tensionKick;
        double x = hook.x + Math.cos(angle) * radius;
        double y = hook.y - 0.78 + session.progress * 0.58 + Math.sin(now * 0.38) * 0.14;
        double z = hook.z + Math.sin(angle) * radius;
        moveVisualFish(session.visualFish, x, y, z, hook.x, hook.z);
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
        fish.setDeltaMovement(Vec3.ZERO);

        Vec3 hook = player.fishing.position();
        double x = hook.x + Math.cos(session.visualAngle) * VISUAL_START_DISTANCE;
        double y = hook.y - 0.85;
        double z = hook.z + Math.sin(session.visualAngle) * VISUAL_START_DISTANCE;
        fish.setPos(x, y, z);
        level.addFreshEntity(fish);
        session.visualFish = fish;
        moveVisualFish(fish, x, y, z, hook.x, hook.z);
    }

    private static Mob createVisualFish(ServerLevel level, FishSpecies species) {
        int variant = Math.floorMod(species.id().hashCode(), 4);
        return switch (variant) {
            case 0 -> EntityType.COD.create(level, EntitySpawnReason.EVENT);
            case 1 -> EntityType.SALMON.create(level, EntitySpawnReason.EVENT);
            case 2 -> EntityType.TROPICAL_FISH.create(level, EntitySpawnReason.EVENT);
            default -> EntityType.PUFFERFISH.create(level, EntitySpawnReason.EVENT);
        };
    }

    private static void moveVisualFish(Mob fish, double x, double y, double z, double targetX, double targetZ) {
        fish.setPos(x, y, z);
        fish.setDeltaMovement(Vec3.ZERO);
        float yaw = (float) (Math.toDegrees(Math.atan2(targetZ - z, targetX - x)) - 90.0);
        fish.setYRot(yaw);
        fish.setYBodyRot(yaw);
        fish.setYHeadRot(yaw);
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
