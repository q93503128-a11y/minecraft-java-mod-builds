package kr.moonseungjun.villageguardians;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum VillageRole {
    VANGUARD(
            "vanguard",
            "선봉검사",
            "근접 돌파와 광역 처치에 강한 전사입니다.",
            "공격력·최대 체력 증가, 근접 피해 흡혈.",
            "근접 광역·돌진·검기 공격.",
            "전열과 적 밀집 구간"),
    RANGER(
            "ranger",
            "성루사수",
            "장거리 사격과 대공전에 강한 궁수입니다.",
            "활 충전 단축, 대공 피해 +18%, 처치 시 화살 회수.",
            "추적·다중 사격·광역 화살 공격.",
            "성벽과 후방 고지"),
    ARCANIST(
            "arcanist",
            "비전술사",
            "광역 피해와 군중 제어에 강한 마법사입니다.",
            "마법이 일정 확률로 최대 2회 추가 발동.",
            "화염·빙결·폭풍·번개 광역 마법.",
            "후방과 적 밀집 구간"),
    LUMINAR(
            "luminar",
            "성휘사제",
            "회복과 보호에 특화된 지원가입니다.",
            "체력이 낮은 아군일수록 치유·보호 효과 증가.",
            "치유·정화·보호막·부활.",
            "후방과 아군 밀집 구간"),
    WARDEN(
            "warden",
            "철벽수호자",
            "도발과 피해 경감에 특화된 수호자입니다.",
            "체력 재생, 피해 감소, 넉백 면역.",
            "도발·밀치기·방벽·진형 유지.",
            "성문과 시설 전면");

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
