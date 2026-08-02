package kr.moonseungjun.villageguardians;

/** Readable forecast assembled from the actual day-based enemy roster. */
public final class VillageWaveIntelSystem {
    private VillageWaveIntelSystem() {}

    public static String report() {
        int day = VillageCouncilState.currentDay();
        StringBuilder text = new StringBuilder();
        text.append("§b현재 전황\n§f").append(VillageRaidSystem.status()).append("\n\n");
        text.append("§b예상 일반 병과\n§f").append(baseRoster(day)).append("\n\n");
        text.append("§b특수 병과 해금\n§f").append(specialRoster(day)).append("\n\n");
        text.append("§6예상 보스\n§f").append(boss(day)).append("\n\n");
        text.append("§7실제 웨이브 특성은 표준·물량·중장갑·공성·추격·주술·광란·재생 중 하나로 정해집니다. ")
                .append("강제 진군 때문에 잔존 적이 있어도 약 60초 후 다음 웨이브가 합류할 수 있습니다.");
        return text.toString();
    }

    private static String baseRoster(int day) {
        String roster = "전열병, 돌격병";
        if (day >= 2) roster += ", 방패병";
        if (day >= 3) roster += ", 사수";
        if (day >= 4) roster += ", 폭파병";
        if (day >= 5) roster += ", 파쇄병";
        return roster;
    }

    private static String specialRoster(int day) {
        if (day < 6) return "아직 주술·지휘 병과는 확인되지 않았습니다.";
        String roster = "저주술사";
        if (day >= 8) roster += ", 전쟁 고수";
        if (day >= 9) roster += ", 탑 사냥꾼";
        if (day >= 11) roster += ", 강령술사";
        return roster;
    }

    private static String boss(int day) {
        int cycle = Math.floorMod(Math.max(3, day) - 3, 4);
        return switch (cycle) {
            case 0 -> "공성 야수 · 시설 피해와 충격파에 특화";
            case 1 -> "철의 전쟁군주 · 주변 적을 강화하는 지휘형";
            case 2 -> "역병 대주교 · 독·회복·포탑 교란형";
            default -> "공포 기사 · 암흑·흡혈·근접 압박형";
        };
    }
}
