package kr.moonseungjun.survivalascension.endgame;

import net.minecraft.util.RandomSource;

public enum AscensionTrialDoctrine {
    ONSLAUGHT("쇄도", "근접 압박과 측면 증원이 빠르게 겹칩니다."),
    PURSUIT("추격", "기동형 적이 전장을 넓게 쓰며 끊임없이 따라붙습니다."),
    SIEGE("봉쇄", "원거리·제어형 적이 전장을 분할하고 전진을 강요합니다.");

    private final String koreanName;
    private final String description;

    AscensionTrialDoctrine(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String koreanName() { return koreanName; }
    public String description() { return description; }

    public int reinforcementCount(int wave) {
        return switch (this) {
            case ONSLAUGHT -> wave >= 2 ? 2 : 1;
            case PURSUIT -> wave >= 3 ? 2 : 1;
            case SIEGE -> wave >= 2 ? 2 : 1;
        };
    }

    public static AscensionTrialDoctrine random(RandomSource random) {
        AscensionTrialDoctrine[] values = values();
        return values[random.nextInt(values.length)];
    }
}
