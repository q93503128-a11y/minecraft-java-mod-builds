package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.SkillSnapshotPayload;
import kr.moonseungjun.survivalascension.network.SkillUpdatePayload;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientSkillState {
    private static final Map<String, Long> XP = new HashMap<>();
    private static final Map<String, RecentSkillUpdate> RECENT = new HashMap<>();
    private static final long RECENT_WINDOW_MILLIS = 4_000L;

    private static SkillUpdatePayload lastUpdate;
    private static long lastUpdateMillis;

    private ClientSkillState() {}

    public static void onSnapshot(SkillSnapshotPayload payload) {
        XP.clear();
        XP.putAll(payload.xp());
        RECENT.clear();
        lastUpdate = null;
        lastUpdateMillis = 0L;
    }

    public static void onUpdate(SkillUpdatePayload payload) {
        long now = System.currentTimeMillis();
        XP.put(payload.skillId(), payload.totalXp());
        lastUpdate = payload;
        lastUpdateMillis = now;

        long gained = Math.max(0L, payload.totalXp() - payload.fromTotalXp());
        RecentSkillUpdate previous = RECENT.get(payload.skillId());
        if (previous != null && now - previous.changedAtMillis() <= RECENT_WINDOW_MILLIS) {
            RECENT.put(payload.skillId(), new RecentSkillUpdate(
                    payload,
                    now,
                    previous.gainedXp() + gained,
                    Math.min(previous.fromLevel(), payload.fromLevel())
            ));
        } else {
            RECENT.put(payload.skillId(), new RecentSkillUpdate(payload, now, gained, payload.fromLevel()));
        }
    }

    public static long xp(SkillType skill) {
        return XP.getOrDefault(skill.id(), 0L);
    }

    public static int level(SkillType skill) {
        return SkillTuning.levelFromXp(xp(skill));
    }

    public static SkillUpdatePayload lastUpdate() {
        return lastUpdate;
    }

    public static long updateAgeMillis() {
        return lastUpdate == null ? Long.MAX_VALUE : Math.max(0L, System.currentTimeMillis() - lastUpdateMillis);
    }

    public static List<RecentSkillUpdate> recentUpdates() {
        long now = System.currentTimeMillis();
        RECENT.entrySet().removeIf(entry -> now - entry.getValue().changedAtMillis() > RECENT_WINDOW_MILLIS);
        return new ArrayList<>(RECENT.values());
    }

    public record RecentSkillUpdate(
            SkillUpdatePayload payload,
            long changedAtMillis,
            long gainedXp,
            int fromLevel
    ) {
        public boolean levelUp() {
            return payload.level() > fromLevel;
        }
    }
}
