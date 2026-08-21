package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.network.SkillSnapshotPayload;
import kr.moonseungjun.survivalascension.network.SkillUpdatePayload;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import java.util.HashMap;
import java.util.Map;

public final class ClientSkillState {
    private static final Map<String, Long> XP = new HashMap<>();
    private static SkillUpdatePayload lastUpdate;
    private static long lastUpdateMillis;
    private ClientSkillState() {}
    public static void onSnapshot(SkillSnapshotPayload payload) { XP.clear(); XP.putAll(payload.xp()); }
    public static void onUpdate(SkillUpdatePayload payload) { XP.put(payload.skillId(), payload.totalXp()); lastUpdate = payload; lastUpdateMillis = System.currentTimeMillis(); }
    public static long xp(SkillType skill) { return XP.getOrDefault(skill.id(), 0L); }
    public static int level(SkillType skill) { return SkillTuning.levelFromXp(xp(skill)); }
    public static SkillUpdatePayload lastUpdate() { return lastUpdate; }
    public static long updateAgeMillis() { return lastUpdate == null ? Long.MAX_VALUE : Math.max(0L, System.currentTimeMillis() - lastUpdateMillis); }
}
