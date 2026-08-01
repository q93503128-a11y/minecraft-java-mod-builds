package kr.moonseungjun.villageguardians;

/**
 * Long-running campaign pacing. The campaign never hard-ends: every five days
 * introduces a milestone siege and days beyond twenty continue in endless-war tiers.
 */
public final class VillageWarfrontSystem {
    private VillageWarfrontSystem() {}

    public static String chapterName(int day) {
        int safe = Math.max(1, day);
        if (safe <= 4) return "변경 수비";
        if (safe <= 9) return "공성 전쟁";
        if (safe <= 14) return "저주 군단";
        if (safe <= 19) return "균열 공세";
        return "끝없는 전쟁 " + endlessTier(safe);
    }

    public static String dayTitle(int day) {
        if (isMilestoneDay(day)) return "대침공일 · " + chapterName(day);
        return chapterName(day);
    }

    public static boolean isMilestoneDay(int day) {
        return day >= 5 && day % 5 == 0;
    }

    public static int endlessTier(int day) {
        return Math.max(1, (Math.max(20, day) - 20) / 5 + 1);
    }

    public static int bonusBossCount(int day, int wave, int maxWaves) {
        if (wave != maxWaves || day < 3) return 0;
        int count = 1;
        if (isMilestoneDay(day)) count++;
        if (day >= 20) count += Math.min(2, endlessTier(day) / 2);
        return Math.min(4, count);
    }

    public static int countBonus(int day) {
        int safe = Math.max(1, day);
        int chapter = Math.max(0, (safe - 1) / 5);
        return Math.min(28, chapter * 3 + Math.max(0, safe - 15) / 2);
    }

    public static float healthMultiplier(int day) {
        int safe = Math.max(1, day);
        if (safe <= 12) return 1.0f;
        return Math.min(1.85f, 1.0f + (safe - 12) * 0.025f);
    }

    public static float structureDamageMultiplier(int day) {
        int safe = Math.max(1, day);
        float chapter = 1.0f + Math.max(0, safe - 10) * 0.012f;
        return Math.min(1.45f, isMilestoneDay(safe) ? chapter * 1.12f : chapter);
    }

    public static float rewardMultiplier(int day) {
        int safe = Math.max(1, day);
        float result = 1.0f + Math.max(0, safe - 1) * 0.025f;
        if (isMilestoneDay(safe)) result += 0.30f;
        return Math.min(2.25f, result);
    }

    public static String milestoneHint(int day) {
        if (!isMilestoneDay(day)) return "";
        return "이번 밤은 대침공일입니다. 마지막 웨이브에 복수의 우두머리가 등장하며 보상이 증가합니다.";
    }
}
