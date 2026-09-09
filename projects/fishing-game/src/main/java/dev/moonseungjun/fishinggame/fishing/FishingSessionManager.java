package dev.moonseungjun.fishinggame.fishing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public final class FishingSessionManager {
    private static final Map<UUID, FishingSession> SESSIONS = new HashMap<>();

    private FishingSessionManager() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!player.getItemInHand(hand).is(Items.FISHING_ROD)) return InteractionResult.PASS;

            // Suppress local vanilla use. The normal use packet still reaches the server,
            // where the first cast is allowed through so vanilla can provide the temporary hook anchor.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            FishingSession session = SESSIONS.get(serverPlayer.getUUID());
            if (session == null) {
                beginCast(serverPlayer);
                return InteractionResult.PASS;
            }

            if (session.stage == FishingStage.HOOKED) {
                session.reelImpulseTicks = Math.min(7, session.reelImpulseTicks + 3);
                return InteractionResult.SUCCESS;
            }

            cancel(serverPlayer, "낚싯줄을 거뒀다.");
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(FishingSessionManager::tick);
        ServerPlayerEvents.LEAVE.register(player -> SESSIONS.remove(player.getUUID()));
    }

    private static void beginCast(ServerPlayer player) {
        long now = player.level().getGameTime();
        int biteDelay = 50 + player.getRandom().nextInt(71); // 2.5–6.0 s
        SESSIONS.put(player.getUUID(), new FishingSession(now, now + biteDelay));
        overlay(player, "낚싯줄을 던졌다.");
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
                if (player.fishing != null) {
                    session.stage = FishingStage.WAITING_FOR_BITE;
                } else if (now - session.castTick > 12) {
                    overlay(player, "낚싯줄이 물에 닿지 않았다.");
                    iterator.remove();
                }
                continue;
            }

            if (player.fishing == null) {
                overlay(player, "낚싯줄이 풀렸다.");
                iterator.remove();
                continue;
            }

            if (session.stage == FishingStage.WAITING_FOR_BITE) {
                if (now >= session.biteTick) {
                    session.species = FishCatalog.pick(player.getRandom().nextDouble());
                    session.stage = FishingStage.HOOKED;
                    session.tension = 0.42f;
                    session.progress = 0.0f;
                    overlay(player, "입질! 우클릭 리듬으로 줄 장력을 유지해라.");
                }
                continue;
            }

            tickHooked(player, session, iterator);
        }
    }

    private static void tickHooked(ServerPlayer player, FishingSession session, Iterator<?> iterator) {
        boolean impulse = session.reelImpulseTicks > 0;
        if (session.reelImpulseTicks > 0) session.reelImpulseTicks--;

        float resistance = session.species.resistance();
        session.tension = ReelMath.updateTension(session.tension, impulse, resistance);
        session.progress = ReelMath.updateProgress(session.progress, session.tension, resistance);

        if (ReelMath.isLineBroken(session.tension)) {
            removeHook(player);
            overlay(player, "줄이 끊어졌다! " + session.species.displayName() + "을(를) 놓쳤다.");
            iterator.remove();
            return;
        }

        if (session.progress >= 1.0f) {
            double weight = session.species.rollWeight(player.getRandom().nextDouble());
            removeHook(player);
            overlay(player, String.format("%s 포획! %.2f kg", session.species.displayName(), weight));
            iterator.remove();
            return;
        }

        if (--session.hudCooldown <= 0) {
            session.hudCooldown = 4;
            int tensionPct = Math.round(session.tension * 100.0f);
            int progressPct = Math.round(session.progress * 100.0f);
            overlay(player, "장력 " + tensionPct + "%  |  포획 " + progressPct + "%  |  " + session.species.displayName());
        }
    }

    private static void cancel(ServerPlayer player, String message) {
        SESSIONS.remove(player.getUUID());
        removeHook(player);
        overlay(player, message);
    }

    private static void overlay(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal(message));
    }

    private static void removeHook(ServerPlayer player) {
        if (player.fishing != null) {
            player.fishing.discard();
            player.fishing = null;
        }
    }
}
