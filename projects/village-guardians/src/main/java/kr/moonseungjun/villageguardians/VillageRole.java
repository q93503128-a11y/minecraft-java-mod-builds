package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    GUARD_CAPTAIN(
            "guard_captain",
            "수비대장",
            "전열에서 적을 붙잡고 아군의 공격과 방어를 동시에 끌어올리는 지휘 역할입니다.",
            "높은 레벨일수록 힘·저항 전술이 강해집니다.",
            "주변 아군에게 힘과 저항을 부여합니다.",
            "성문 수비와 보스 집중 전투"),
    BUILDER(
            "builder",
            "건축가",
            "시설이 무너지기 전까지 버티는 데 특화된 방어 지원 역할입니다.",
            "마을 안에서 받는 피해 감소 효과와 궁합이 좋습니다.",
            "주변 아군에게 강한 저항과 흡수 체력을 부여합니다.",
            "성벽이 밀리는 장기 방어전"),
    QUARTERMASTER(
            "quartermaster",
            "보급관",
            "교전 중 아군의 체력을 즉시 보충하고 전선을 유지하는 보급 역할입니다.",
            "상점·보급소 운영과 지원 스킬 트리에 잘 맞습니다.",
            "주변 아군을 즉시 치유하고 재생을 부여합니다.",
            "여러 명이 흩어져 싸우는 멀티플레이"),
    SCOUT(
            "scout",
            "정찰병",
            "빠른 이동으로 성벽과 건물 사이를 오가며 침투한 적을 처리합니다.",
            "이동·시야 중심의 전투 운영에 유리합니다.",
            "주변 아군에게 속도와 야간 시야를 부여합니다.",
            "성문 돌파 후 내부 기동전"),
    STEWARD(
            "steward",
            "관리관",
            "전투와 마을 운영을 균형 있게 보조하는 지속 지원 역할입니다.",
            "훈련·수리·보급을 자주 이용하는 성장형 플레이에 적합합니다.",
            "주변 아군에게 신속한 작업과 재생 효과를 부여합니다.",
            "낮 정비와 밤 전투를 모두 담당할 때"),
    MEDIC(
            "medic",
            "의무관",
            "큰 피해를 입은 파티를 한 번에 회복시키는 전문 치료 역할입니다.",
            "의무소 강화와 방어 스킬 트리의 효율이 높습니다.",
            "주변 아군을 크게 치유하고 재생·흡수 체력을 부여합니다.",
            "고난도 웨이브와 보스전");

    private final String id;
    private final String koreanName;
    private final String overview;
    private final String passive;
    private final String active;
    private final String recommended;

    VillageRole(
            String id,
            String koreanName,
            String overview,
            String passive,
            String active,
            String recommended) {
        this.id = id;
        this.koreanName = koreanName;
        this.overview = overview;
        this.passive = passive;
        this.active = active;
        this.recommended = recommended;
    }

    public String id() {
        return id;
    }

    public String shortName() {
        return koreanName;
    }

    public String displayName() {
        return koreanName;
    }

    public String overview() {
        return overview;
    }

    public String passive() {
        return passive;
    }

    public String active() {
        return active;
    }

    public String recommended() {
        return recommended;
    }

    public static Optional<VillageRole> parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(role -> role.id.equals(normalized))
                .findFirst();
    }

    public static String ids() {
        return String.join(", ", Arrays.stream(values()).map(VillageRole::id).toList());
    }
}
