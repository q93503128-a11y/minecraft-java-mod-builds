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
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public final class FishingSessionManager {
    private static final int HOOK_WATER_TIMEOUT_TICKS = 60;
    private static final int WATER_EXIT_GRACE_TICKS = 8;
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
        ServerPlayerEvents.LEAVE.register(player -> SESSIONS.remove(player.getUUID()));
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
        FishingSession session = new FishingSession(now, FishingLocation.LAKESIDE);
        SESSIONS.put(player.getUUID(), session);
        sendState(player, session, "");
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, FishingSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FishingSession> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            FishingSession session = entry.getValue();
            long now = player.level().getGameTime();

            if (session.stage == FishingStage.WAITING_FOR_HOOK) {
                if (isHookAtFishingWater(player)) {
                    session.stage = FishingStage.WAITING_FOR_BITE;
                    session.biteTick = now + nextBiteDelay(player);
                    session.dryTicks = 0;
                    sendState(player, session, "");
                } else if (player.fishing != null && now - session.castTick > HOOK_WATER_TIMEOUT_TICKS) {
                    finish(iterator, player, "물에 찌를 던져 주세요.", true);
                } else if (player.fishing == null && now - session.castTick > 12) {
                    finish(iterator, player, "캐스팅하지 못했습니다.", false);
                }
                continue;
            }

            if (player.fishing == null) {
                finish(iterator, player, "낚싯줄이 풀렸습니다.", false);
                continue;
            }

            if (!isHookAtFishingWater(player)) {
                session.dryTicks++;
                if (session.dryTicks > WATER_EXIT_GRACE_TICKS) {
                    finish(iterator, player, "찌가 물 밖으로 나왔습니다.", true);
                }
                continue;
            }
            session.dryTicks = 0;

            if (session.stage == FishingStage.WAITING_FOR_BITE) {
                if (now >= session.biteTick) {
                    PlayerFishingProfile profile = FishingProfiles.get(player);
                    RodDefinition rod = FishingRods.byTier(profile.rodTier());
                    session.species = FishCatalog.pick(session.location, player.getRandom().nextDouble(), rod.luck());
                    session.stage = FishingStage.HOOKED;
                    session.tension = 0.42f;
                    session.progress = 0.0f;
                    session.reelHeld = false;
                    sendState(player, session, "입질! 우클릭을 누르고 떼며 장력을 유지하세요.");
                }
                continue;
            }

            tickHooked(player, session, iterator);
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
        return Math.max(28, Math.round(base * rod.lureTimeMultiplier()));
    }

    private static void tickHooked(ServerPlayer player, FishingSession session, Iterator<?> iterator) {
        PlayerFishingProfile profile = FishingProfiles.get(player);
        RodDefinition rod = FishingRods.byTier(profile.rodTier());
        float resistance = session.species.resistance();
        session.tension = ReelMath.updateTension(session.tension, session.reelHeld, resistance, rod.strength());
        session.progress = ReelMath.updateProgress(session.progress, session.tension, resistance, rod.controlBonus());

        if (ReelMath.isLineBroken(session.tension)) {
            finish(iterator, player, session.species.displayName() + "을(를) 놓쳤습니다.", true);
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
            finish(iterator, player, String.format("%s %.2f kg 포획! 가방 %d/%d", session.species.displayName(), weight, updated.catches().size(), PlayerFishingProfile.BAG_CAPACITY), true);
            return;
        }

        if (--session.hudCooldown <= 0) {
            session.hudCooldown = 4;
            sendState(player, session, "");
        }
    }

    private static void cancel(ServerPlayer player, String message) {
        SESSIONS.remove(player.getUUID());
        removeHook(player);
        sendIdle(player, message);
    }

    private static void finish(Iterator<?> iterator, ServerPlayer player, String message, boolean removeFishingHook) {
        if (removeFishingHook) removeHook(player);
        iterator.remove();
        sendIdle(player, message);
    }

    private static void sendState(ServerPlayer player, FishingSession session, String notice) {
        String species = session.species == null ? "" : session.species.displayName();
        ServerPlayNetworking.send(player, new FishingStatePayload(
                session.stage.ordinal(), session.tension, session.progress,
                species, session.location.displayName(), notice
        ));
    }

    private static void sendIdle(ServerPlayer player, String notice) {
        ServerPlayNetworking.send(player, new FishingStatePayload(
                FishingStage.values().length, 0.0f, 0.0f,
                "", FishingLocation.LAKESIDE.displayName(), notice
        ));
    }

    private static void removeHook(ServerPlayer player) {
        if (player.fishing != null) {
            player.fishing.discard();
            player.fishing = null;
        }
    }
}
