package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    VANGUARD(
            "vanguard",
            "선봉검사",
            "검을 들고 전열을 돌파하며 다수의 적을 빠르게 정리하는 전사 계열입니다.",
            "검과 도끼 피해가 증가하고 적을 쓰러뜨릴수록 짧은 전투 가속을 얻습니다.",
            "회전 참격·돌진·검기 폭풍처럼 범위와 연속 공격에 특화된 기술을 사용합니다.",
            "성문 앞 전열, 적 밀집 구간, 공격적인 근접 전투"),
    RANGER(
            "ranger",
            "성루사수",
            "성벽과 고지에서 적의 진입을 끊는 궁수 계열입니다.",
            "투사체 피해가 증가하며 성벽 위에서는 추가 사거리 보정과 피해를 얻습니다.",
            "연발·관통·도탄·화염 사격을 조합해 후방에서 정예 적을 제거합니다.",
            "성벽 상단, 방어탑 주변, 보스 약점 집중 사격"),
    ARCANIST(
            "arcanist",
            "비전술사",
            "원소와 비전력을 폭발시켜 광역 전투를 담당하는 마법 계열입니다.",
            "역할 기술 피해가 증가하고 기술 재사용 대기시간이 조금 짧아집니다.",
            "화염 구체·서리 고리·연쇄 번개·비전 폭발로 적 무리를 제어합니다.",
            "중앙 후방, 성문 안쪽, 다수의 적이 겹치는 지점"),
    LUMINAR(
            "luminar",
            "성휘사제",
            "아군의 체력과 전투 지속력을 책임지는 치유 계열입니다.",
            "치유량이 증가하고 주변 아군이 치명적인 피해를 버틸 수 있도록 보호합니다.",
            "즉시 치유·정화·재생 장막·생명 성역을 상황에 맞게 장착합니다.",
            "다인 전투 중심, 보스전 후방, 부상자가 모이는 방어선"),
    WARDEN(
            "warden",
            "철벽수호자",
            "방패와 중갑으로 적을 붙잡고 시설을 지키는 탱커 계열입니다.",
            "받는 피해가 크게 감소하며 방패를 들면 추가 저항과 밀쳐내기 저항을 얻습니다.",
            "도발·방패 충격·철벽 진형·수호 결계로 적의 시선을 끌고 아군을 보호합니다.",
            "북문 정면, 보스 고정, 파괴 직전 시설 앞");

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
            case "guard_captain", "warrior", "vanguard" -> Optional.of(VANGUARD);
            case "scout", "ranger", "archer" -> Optional.of(RANGER);
            case "mage", "wizard", "arcanist" -> Optional.of(ARCANIST);
            case "quartermaster", "medic", "healer", "luminar" -> Optional.of(LUMINAR);
            case "builder", "steward", "engineer", "tank", "warden" -> Optional.of(WARDEN);
            default -> Arrays.stream(values())
                    .filter(role -> role.id.equals(normalized))
                    .findFirst();
        };
    }

    public static String ids() {
        return String.join(", ", Arrays.stream(values()).map(VillageRole::id).toList());
    }
}
