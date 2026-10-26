package dev.moonseungjun.augmentationsurvival.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/** Shared connected-player lifecycle for concurrent server GameTests. */
final class GameTestPlayers {
    private static final AtomicInteger NEXT_NAME = new AtomicInteger();
    private static final Map<GameTestHelper, List<ServerPlayer>> TRACKED = new IdentityHashMap<>();

    private GameTestPlayers() {
    }

    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), nextProfileName());
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(
                server,
                level,
                cookie.gameProfile(),
                cookie.clientInformation()
        );
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        TRACKED.computeIfAbsent(helper, ignored -> new ArrayList<>()).add(player);
        return player;
    }

    static void runAfterDelay(GameTestHelper helper, long delayTicks, Runnable action) {
        helper.runAfterDelay(delayTicks, () -> {
            try {
                action.run();
            } catch (RuntimeException | Error exception) {
                retireAll(helper);
                helper.fail(failureMessage(exception));
            }
        });
    }

    static void succeed(GameTestHelper helper) {
        retireAll(helper);
        helper.succeed();
    }

    static void invoke(Object suite, GameTestHelper helper, Method method) throws ReflectiveOperationException {
        try {
            method.invoke(suite, helper);
        } catch (ReflectiveOperationException exception) {
            retireAll(helper);
            throw exception;
        } catch (RuntimeException | Error exception) {
            retireAll(helper);
            throw exception;
        }
    }

    static void retireAll(GameTestHelper helper) {
        List<ServerPlayer> players = TRACKED.remove(helper);
        if (players == null) {
            return;
        }
        for (ServerPlayer player : List.copyOf(players)) {
            retire(player);
        }
    }

    private static void retire(ServerPlayer player) {
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        if (server.getPlayerList().getPlayers().contains(player)) {
            server.getPlayerList().remove(player);
        }
        if (!player.isRemoved()) {
            player.discard();
        }
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static String nextProfileName() {
        return "asgt_" + Integer.toUnsignedString(NEXT_NAME.incrementAndGet(), 36);
    }
}
