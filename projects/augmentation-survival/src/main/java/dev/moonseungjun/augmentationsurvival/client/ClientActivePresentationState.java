package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-only presentation cache fed exclusively by server-confirmed active events. */
public final class ClientActivePresentationState {
    private static final long BANNER_MILLIS = 1350L;
    private static final Map<String, Entry> ACTIVE = new LinkedHashMap<>();
    private static Banner banner = Banner.EMPTY;

    private ClientActivePresentationState() {
    }

    public static void apply(ActivePresentationPayload payload) {
        long now = System.currentTimeMillis();
        String id = payload.abilityId();
        switch (payload.phase()) {
            case ActivePresentationPayload.INSTANT -> banner = new Banner(id, payload.phase(), now + BANNER_MILLIS);
            case ActivePresentationPayload.CHANNEL, ActivePresentationPayload.SUSTAINED -> {
                long durationMillis = payload.durationTicks() <= 0 ? 0L : payload.durationTicks() * 50L;
                long end = durationMillis <= 0L ? Long.MAX_VALUE : now + durationMillis;
                ACTIVE.put(id, new Entry(id, payload.phase(), now, end, durationMillis, payload.radius()));
                banner = new Banner(id, payload.phase(), now + BANNER_MILLIS);
            }
            case ActivePresentationPayload.TOGGLE_ON -> {
                ACTIVE.put(id, new Entry(id, payload.phase(), now, Long.MAX_VALUE, 0L, payload.radius()));
                banner = new Banner(id, payload.phase(), now + BANNER_MILLIS);
            }
            case ActivePresentationPayload.TOGGLE_OFF, ActivePresentationPayload.END, ActivePresentationPayload.CANCEL -> {
                ACTIVE.remove(id);
                banner = new Banner(id, payload.phase(), now + BANNER_MILLIS);
            }
            default -> { }
        }
    }

    /** Mirrors an already server-synchronized boolean runtime such as Phase Shift or Time Acceleration. */
    public static void setExternalActive(String abilityId, boolean active) {
        long now = System.currentTimeMillis();
        if (active) {
            ACTIVE.put(abilityId, new Entry(
                    abilityId,
                    ActivePresentationPayload.SUSTAINED,
                    now,
                    Long.MAX_VALUE,
                    0L,
                    0.0F
            ));
            banner = new Banner(abilityId, ActivePresentationPayload.SUSTAINED, now + BANNER_MILLIS);
        } else {
            ACTIVE.remove(abilityId);
        }
    }

    public static List<Entry> entries() {
        pruneExpired();
        return List.copyOf(new ArrayList<>(ACTIVE.values()));
    }

    public static Banner banner() {
        if (banner == Banner.EMPTY || banner.untilMillis() <= System.currentTimeMillis()) {
            return Banner.EMPTY;
        }
        return banner;
    }

    public static boolean isActive(String abilityId) {
        pruneExpired();
        return ACTIVE.containsKey(abilityId);
    }

    public static void clear() {
        ACTIVE.clear();
        banner = Banner.EMPTY;
    }

    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (entry.expiresAtMillis() != Long.MAX_VALUE && entry.expiresAtMillis() <= now) {
                iterator.remove();
            }
        }
    }

    public record Entry(
            String abilityId,
            int phase,
            long startedAtMillis,
            long expiresAtMillis,
            long durationMillis,
            float radius
    ) {
        public int remainingTicks() {
            if (expiresAtMillis == Long.MAX_VALUE) return -1;
            long remaining = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
            return (int) Math.ceil(remaining / 50.0D);
        }

        public float remainingFraction() {
            if (durationMillis <= 0L || expiresAtMillis == Long.MAX_VALUE) return 1.0F;
            long remaining = Math.max(0L, expiresAtMillis - System.currentTimeMillis());
            return Math.max(0.0F, Math.min(1.0F, remaining / (float) durationMillis));
        }
    }

    public record Banner(String abilityId, int phase, long untilMillis) {
        private static final Banner EMPTY = new Banner("", ActivePresentationPayload.INSTANT, 0L);
        public boolean visible() { return this != EMPTY && !abilityId.isBlank() && untilMillis > System.currentTimeMillis(); }
    }
}
