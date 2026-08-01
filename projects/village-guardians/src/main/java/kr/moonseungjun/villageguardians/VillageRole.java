package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    GUARD_CAPTAIN(
            "guard_captain",
            "수비대장",
            "성문과 전열에서 적을 붙잡는 근접 지휘 역할입니다.",
            "검 피해가 증가하고 받는 피해가 감소합니다.",
            "주변 아군에게 힘과 저항을 부여하고 가까운 적을 휩씁니다.",
            "성문 수비, 보스 고정, 근접 전투"),
    RANGER(
            "ranger",
            "성벽 궁수",
            "성벽 위에서 활로 적의 진입을 끊는 원거리 전문 역할입니다.",
            "투사체 피해가 증가하며 성벽 위에서는 추가 피해를 줍니다.",
            "주변 아군에게 기동력과 야간 시야를 주고 원거리 집중 상태를 부여합니다.",
            "성벽 사격, 정예 적 제거, 도탄·불화살 운용"),
    ENGINEER(
            "engineer",
            "공병대장",
            "방어탑·용병과 시설 방어를 강화하는 공성 대응 역할입니다.",
            "마을 안에서 받는 피해가 줄고 방어탑의 공격 효율을 높입니다.",
            "주변 아군에게 강한 저항과 흡수 체력을 부여합니다.",
            "시설 방어, 장기전, 방어탑·용병 중심 운영"),
    MEDIC(
            "medic",
            "의무관",
            "큰 피해를 입은 파티를 회복시키는 전문 지원 역할입니다.",
            "받는 피해가 조금 감소하고 의무소 치료 효율이 높아집니다.",
            "주변 아군을 즉시 치유하고 재생·흡수 체력을 부여합니다.",
            "고난도 웨이브, 보스전, 멀티플레이 회복");

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
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "builder", "steward", "engineer" -> Optional.of(ENGINEER);
            case "quartermaster", "medic" -> Optional.of(MEDIC);
            case "scout", "ranger" -> Optional.of(RANGER);
            default -> Arrays.stream(values())
                    .filter(role -> role.id.equals(normalized))
                    .findFirst();
        };
    }

    public static String ids() {
        return String.join(", ", Arrays.stream(values()).map(VillageRole::id).toList());
    }
}
