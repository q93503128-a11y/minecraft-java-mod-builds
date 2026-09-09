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
    private static final int HOOK_WATER_TIMEOUT_TICKS = 60;
    private static final Map<UUID, FishingSession> SESSIONS = new HashMap<>();

    private FishingSessionManager() {
    }

    public static void initialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!player.getItemInHand(hand).is(Items.FISHING_ROD)) return InteractionResult.PASS;

            // Suppress local vanilla retrieval while still allowing the initial use packet
            // to reach the server. The server lets the first use continue so vanilla creates
            // the temporary fishing-hook anchor.
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            FishingSession session = SESSIONS.get(serverPlayer.getUUID());
            if (session == null) {
                beginCast(serverPlayer);
                return InteractionResult.PASS;
            }

            if (session.stage == FishingStage.HOOKED) {
                // Reel pressure is driven by the client's held-use state packet. Returning
                // SUCCESS here prevents vanilla from retrieving the temporary hook.
                return InteractionResult.SUCCESS;
            }

            cancel(serverPlayer, "낚싯줄을 거뒀다.");
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

    private static void beginCast(ServerPlayer player) {
        long now = player.level().getGameTime();
        SESSIONS.put(player.getUUID(), new FishingSession(now));
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
                if (player.fishing != null && player.fishing.isInWater()) {
                    session.stage = FishingStage.WAITING_FOR_BITE;
                    session.biteTick = now + nextBiteDelay(player);
                    overlay(player, "찌가 물에 닿았다. 입질을 기다리는 중...");
                } else if (player.fishing != null && now - session.castTick > HOOK_WATER_TIMEOUT_TICKS) {
                    finish(iterator, player, "물속에 찌를 던져야 한다.", true);
                } else if (player.fishing == null && now - session.castTick > 12) {
                    finish(iterator, player, "낚싯줄을 던지지 못했다.", false);
                }
                continue;
            }

            if (player.fishing == null) {
                finish(iterator, player, "낚싯줄이 풀렸다.", false);
                continue;
            }

            if (!player.fishing.isInWater()) {
                finish(iterator, player, "찌가 물 밖으로 나왔다.", true);
                continue;
            }

            if (session.stage == FishingStage.WAITING_FOR_BITE) {
                if (now >= session.biteTick) {
                    session.species = FishCatalog.pick(player.getRandom().nextDouble());
                    session.stage = FishingStage.HOOKED;
                    session.tension = 0.42f;
                    session.progress = 0.0f;
                    session.reelHeld = false;
                    overlay(player, "입질! 우클릭을 누르고 떼며 장력을 유지해라.");
                }
                continue;
            }

            tickHooked(player, session, iterator);
        }
    }

    private static int nextBiteDelay(ServerPlayer player) {
        return 50 + player.getRandom().nextInt(71); // 2.5–6.0 s after the hook reaches water
    }

    private static void tickHooked(ServerPlayer player, FishingSession session, Iterator<?> iterator) {
        float resistance = session.species.resistance();
        session.tension = ReelMath.updateTension(session.tension, session.reelHeld, resistance);
        session.progress = ReelMath.updateProgress(session.progress, session.tension, resistance);

        if (ReelMath.isLineBroken(session.tension)) {
            finish(iterator, player, "줄이 끊어졌다! " + session.species.displayName() + "을(를) 놓쳤다.", true);
            return;
        }

        if (session.progress >= 1.0f) {
            double weight = session.species.rollWeight(player.getRandom().nextDouble());
            finish(iterator, player, String.format("%s 포획! %.2f kg", session.species.displayName(), weight), true);
            return;
        }

        if (--session.hudCooldown <= 0) {
            session.hudCooldown = 4;
            int tensionPct = Math.round(session.tension * 100.0f);
            int progressPct = Math.round(session.progress * 100.0f);
            String input = session.reelHeld ? "릴 감는 중" : "릴 놓음";
            overlay(player, "장력 " + tensionPct + "%  |  포획 " + progressPct + "%  |  " + input + "  |  " + session.species.displayName());
        }
    }

    private static void cancel(ServerPlayer player, String message) {
        SESSIONS.remove(player.getUUID());
        removeHook(player);
        overlay(player, message);
    }

    private static void finish(Iterator<?> iterator, ServerPlayer player, String message, boolean removeFishingHook) {
        if (removeFishingHook) {
            removeHook(player);
        }
        overlay(player, message);
        iterator.remove();
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
