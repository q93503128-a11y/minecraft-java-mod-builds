package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    VANGUARD(
            "vanguard",
            "선봉검사",
            "검을 들고 전열을 돌파하며 다수의 적을 빠르게 정리하는 전사 계열입니다.",
            "기본 공격력과 최대 체력이 증가하고 근접 피해 일부를 체력으로 흡수합니다.",
            "회전 칼날·전투 고양·검기 난무·천붕 강하로 근접 전장을 장악합니다.",
            "성문 앞 전열, 적 밀집 구간, 공격적인 근접 전투"),
    RANGER(
            "ranger",
            "성루사수",
            "성벽과 고지에서 적의 진입을 끊는 궁수 계열입니다.",
            "활 충전 시간이 짧아지고 조준이 공중 위협을 우선 보정하며 공중 적에게 화살 피해가 18% 증가하고 화살로 처치하면 사용 화살을 회수합니다.",
            "신속 삼연사·추적 도탄·천공 화살비·성멸 대궁으로 원거리 전장을 제압합니다.",
            "성벽 상단, 방어탑 주변, 보스 약점 집중 사격"),
    ARCANIST(
            "arcanist",
            "비전술사",
            "원소와 비전력을 폭발시켜 광역 전투를 담당하는 마법 계열입니다.",
            "마법을 사용할 때 일정 확률로 같은 마법이 추가 발동하며 한 번의 시전에 최대 두 번 반복됩니다.",
            "홍염탄·빙결 지대·폭풍 회랑·천뢰 폭격으로 적 무리를 폭발시키고 제어합니다.",
            "중앙 후방, 성문 안쪽, 다수의 적이 겹치는 지점"),
    LUMINAR(
            "luminar",
            "성휘사제",
            "아군의 체력과 전투 지속력을 책임지는 치유 계열입니다.",
            "대상의 체력이 낮을수록 치유량과 보호막량이 크게 증폭됩니다.",
            "응급 성광·전군 정화·치유 성역·기적의 대성역으로 전투를 복구합니다.",
            "다인 전투 중심, 보스전 후방, 부상자가 모이는 방어선"),
    WARDEN(
            "warden",
            "철벽수호자",
            "방패와 중갑으로 적을 붙잡고 시설을 지키는 탱커 계열입니다.",
            "체력이 계속 재생되고 받는 피해가 감소하며 모든 넉백을 무효화합니다.",
            "수호 돌진·위압의 함성·거대 방패 태세·대수호 진군으로 적을 밀어냅니다.",
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
