package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Transient server-authored HUD notices rendered above the spell bar instead of vanilla action text. */
public final class ArcaneNoticeService {
    private static final AtomicLong NEXT_SEQUENCE = new AtomicLong();
    private static final Map<UUID, Notice> NOTICES = new HashMap<>();

    private ArcaneNoticeService() {}

    public static void push(ServerPlayer player, Component component) {
        push(player, component, 70);
    }

    public static void push(ServerPlayer player, Component component, int durationTicks) {
        if (player == null || component == null) return;
        long now = clock(player);
        NOTICES.put(player.getUUID(), new Notice(
                NEXT_SEQUENCE.incrementAndGet(),
                component.getString().replace(';', ' ').replace('|', ' '),
                now + Math.max(20, durationTicks)));
    }

    public static long sequence(ServerPlayer player) {
        Notice notice = live(player);
        return notice == null ? 0L : notice.sequence();
    }

    public static String text(ServerPlayer player) {
        Notice notice = live(player);
        return notice == null ? "" : notice.text();
    }

    public static int ttl(ServerPlayer player) {
        Notice notice = live(player);
        if (notice == null) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, notice.expiresAt() - clock(player)));
    }

    public static void clear(UUID playerId) {
        NOTICES.remove(playerId);
    }

    public static void clearAll() {
        NOTICES.clear();
    }

    private static Notice live(ServerPlayer player) {
        Notice notice = NOTICES.get(player.getUUID());
        if (notice == null) return null;
        if (notice.expiresAt() <= clock(player)) {
            NOTICES.remove(player.getUUID());
            return null;
        }
        return notice;
    }

    private static long clock(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().overworld().getGameTime();
    }

    private record Notice(long sequence, String text, long expiresAt) {}
}
