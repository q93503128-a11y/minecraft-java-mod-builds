package kr.moonseungjun.villageguardians;

/**
 * Long-running campaign pacing. Days 1-100 are authored campaign progression;
 * day 100 is the final grand siege and later days remain optional endless play.
 */
public final class VillageWarfrontSystem {
    private VillageWarfrontSystem() {}

    public static String chapterName(int day) {
        int safe = Math.max(1, day);
        if (safe <= 4) return "변경 수비";
        if (safe <= 9) return "공성 전쟁";
        if (safe <= 14) return "저주 군단";
        if (safe <= 19) return "균열 공세";
        if (safe <= 29) return "검은 물결";
        if (safe <= 39) return "천공 침공";
        if (safe <= 49) return "사령 공성";
        if (safe <= 59) return "철의 일식";
        if (safe <= 69) return "폭풍 군단";
        if (safe <= 79) return "심연 진군";
        if (safe <= 89) return "멸망 공세";
        if (safe <= 99) return "최후 전쟁";
        if (safe == VillageCampaignProgression.CAMPAIGN_END_DAY) return "최종 대공성";
        return "끝없는 전쟁 " + endlessTier(safe);
    }

    public static String dayTitle(int day) {
        if (VillageCampaignProgression.isFinalSiege(day)) return "결전일 · " + chapterName(day);
        if (isMilestoneDay(day)) return "대침공일 · " + chapterName(day);
        return chapterName(day);
    }

    public static boolean isMilestoneDay(int day) {
        return day >= 5 && day % 5 == 0;
    }

    public static int endlessTier(int day) {
        return Math.max(1, (Math.max(101, day) - 101) / 5 + 1);
    }

    public static int bonusBossCount(int day, int wave, int maxWaves) {
        if (wave != maxWaves || day < 4) return 0;
        if (VillageCampaignProgression.isFinalSiege(day)) return Math.min(4, Math.max(1, maxWaves));
        if (day < 10) {
            if (isMilestoneDay(day)) return 1;
            return day == 4 || day == 7 ? 1 : 0;
        }
        int count = 1;
        if (day >= 40) count++;
        if (day >= 80 && isMilestoneDay(day)) count++;
        if (day > 100) count += Math.min(1, endlessTier(day) / 4);
        return Math.min(4, count);
    }

    public static int countBonus(int day) {
        int safe = Math.max(1, day);
        int chapterGrowth = Math.max(0, safe - 1) / 10 * 2;
        int milestone = isMilestoneDay(safe) ? 1 : 0;
        return Math.min(20, chapterGrowth + milestone);
    }

    public static float structureDamageMultiplier(int day) {
        int safe = Math.max(1, day);
        float campaign = 1.0f + Math.max(0, Math.min(100, safe) - 10) * 0.0035f;
        if (isMilestoneDay(safe)) campaign *= 1.08f;
        if (safe > 100) campaign += Math.min(0.20f, (safe - 100) * 0.002f);
        return Math.min(1.55f, campaign);
    }

    public static float rewardMultiplier(int day) {
        int safe = Math.max(1, day);
        float result = 1.0f + Math.max(0, Math.min(100, safe) - 1) * 0.028f;
        if (isMilestoneDay(safe)) result += 0.18f;
        if (safe > 100) result += Math.min(0.75f, (safe - 100) * 0.01f);
        return Math.min(4.75f, result);
    }

    public static String milestoneHint(int day) {
        if (VillageCampaignProgression.isFinalSiege(day)) {
            return "제100일 결전입니다. 모든 전선과 우두머리 공세가 동시에 압박합니다.";
        }
        if (!isMilestoneDay(day)) return "";
        return "이번 밤은 대침공일입니다. 마지막 웨이브의 우두머리 공세와 보상이 강화됩니다.";
    }
}
