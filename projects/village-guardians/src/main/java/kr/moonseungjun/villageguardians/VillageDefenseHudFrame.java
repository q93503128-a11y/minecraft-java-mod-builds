package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stable server-to-client combat HUD frame carried inside the existing string payload.
 * Human-readable status strings stay out of the parsing contract: each field has one fixed slot.
 */
public record VillageDefenseHudFrame(
        int day,
        String phase,
        int level,
        String experience,
        String role,
        int coins,
        int supplies,
        String raidMode,
        int wave,
        int maxWaves,
        int enemyCount,
        int nextSeconds,
        String trait,
        String defenseName,
        int defenseCurrent,
        int defenseMaximum,
        int northPressure,
        int westPressure,
        int eastPressure,
        int rearPressure,
        String alert) {

    private static final String SEP = "\u001F";
    private static final int FIELD_COUNT = 21;

    public static VillageDefenseHudFrame from(ServerPlayer player) {
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String xp = progress.level() >= RpgProgress.MAX_LEVEL
                ? "MAX"
                : progress.experience() + "/" + progress.experienceToNextLevel();
        String role = VillageCouncilState.roleOf(player.getUUID())
                .map(VillageRole::shortName)
                .orElse("역할 없음");
        VillageRaidSystem.RaidHudSnapshot raid = VillageRaidSystem.hudSnapshot();
        DefensePoint defense = weakestDefense();
        String alert = VillageRespawnSystem.isDowned(player)
                ? plain(VillageRespawnSystem.hudText(player))
                : defense.current() * 4 <= Math.max(1, defense.maximum())
                ? "방어선 위험 · " + defense.name()
                : raid.active() && raid.enemyCount() >= 16
                ? "적 밀집 · 다전선 대응 필요"
                : "";
        return new VillageDefenseHudFrame(
                VillageCouncilState.currentDay(),
                VillageCouncilState.currentPhase().koreanName(),
                progress.level(), xp, role,
                VillageProgressionSystem.coins(player), VillageProgressionSystem.supplies(),
                raid.mode(), raid.wave(), raid.maxWaves(), raid.enemyCount(), raid.nextSeconds(), raid.trait(),
                defense.name(), defense.current(), defense.maximum(),
                raid.north(), raid.west(), raid.east(), raid.rear(), alert);
    }

    public String encode() {
        return String.join(SEP,
                Integer.toString(day), safe(phase), Integer.toString(level), safe(experience), safe(role),
                Integer.toString(coins), Integer.toString(supplies), safe(raidMode), Integer.toString(wave),
                Integer.toString(maxWaves), Integer.toString(enemyCount), Integer.toString(nextSeconds), safe(trait),
                safe(defenseName), Integer.toString(defenseCurrent), Integer.toString(defenseMaximum),
                Integer.toString(northPressure), Integer.toString(westPressure), Integer.toString(eastPressure),
                Integer.toString(rearPressure), safe(alert));
    }

    public static VillageDefenseHudFrame decode(String encoded) {
        String[] p = (encoded == null ? "" : encoded).split(SEP, -1);
        if (p.length != FIELD_COUNT) return empty();
        return new VillageDefenseHudFrame(
                number(p[0]), p[1], number(p[2]), p[3], p[4], number(p[5]), number(p[6]), p[7],
                number(p[8]), number(p[9]), number(p[10]), number(p[11]), p[12], p[13], number(p[14]),
                Math.max(1, number(p[15])), number(p[16]), number(p[17]), number(p[18]), number(p[19]), p[20]);
    }

    public boolean valid() {
        return day > 0 && defenseMaximum > 0;
    }

    public static VillageDefenseHudFrame empty() {
        return new VillageDefenseHudFrame(0, "", 0, "", "", 0, 0, "SAFE",
                0, 0, 0, 0, "", "방어선", 0, 1, 0, 0, 0, 0, "");
    }

    private static DefensePoint weakestDefense() {
        DefensePoint weakest = null;
        float lowest = Float.MAX_VALUE;
        for (VillageSiegeSegmentSystem.Segment segment : VillageSiegeSegmentSystem.Segment.values()) {
            int current = VillageSiegeSegmentSystem.currentHp(segment);
            int maximum = Math.max(1, VillageSiegeSegmentSystem.maxHp(segment));
            float ratio = current / (float) maximum;
            if (ratio < lowest) {
                lowest = ratio;
                weakest = new DefensePoint(segment.displayName(), current, maximum);
            }
        }
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.WALLS) continue;
            int current = VillageProgressionSystem.durability(building);
            int maximum = Math.max(1, VillageProgressionSystem.maxDurability(building));
            float ratio = current / (float) maximum;
            if (ratio < lowest) {
                lowest = ratio;
                weakest = new DefensePoint(building.displayName(), current, maximum);
            }
        }
        return weakest == null ? new DefensePoint("방어선", 1, 1) : weakest;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace(SEP, " ");
    }

    private static int number(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static String plain(String value) {
        String stripped = ChatFormatting.stripFormatting(value == null ? "" : value);
        return stripped == null ? "" : stripped;
    }

    private record DefensePoint(String name, int current, int maximum) {}
}
