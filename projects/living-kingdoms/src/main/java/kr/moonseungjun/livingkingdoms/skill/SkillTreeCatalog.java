package kr.moonseungjun.livingkingdoms.skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-authoritative skill definitions used by both validation and the codex UI. */
public final class SkillTreeCatalog {
    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();

    static {
        add("combat_endurance", "combat", "전투 지구력", "받는 피해가 5% 감소합니다.", 1);
        add("combat_training", "combat", "무기 숙련", "직접 가하는 피해가 10% 증가합니다.", 2,
                "combat_endurance");
        add("combat_last_stand", "combat", "최후의 버팀", "체력이 35% 이하일 때 받는 피해가 추가로 15% 감소합니다.", 3,
                "combat_training");

        add("explore_trailblazer", "exploration", "길잡이", "살아있는 왕국 대륙에서 이동 속도가 증가합니다.", 1);
        add("explore_night_sight", "exploration", "밤눈", "어두운 장소에서도 시야를 확보합니다.", 2,
                "explore_trailblazer");
        add("explore_safe_fall", "exploration", "가벼운 착지", "추락 피해가 40% 감소합니다.", 2,
                "explore_trailblazer");

        add("life_gatherer", "livelihood", "숙련 채집", "자연 자원을 채집할 때 20% 확률로 산출물이 하나 늘어납니다.", 1);
        add("life_artisan", "livelihood", "장인의 손", "블록 채집 경험치가 25% 증가합니다.", 2,
                "life_gatherer");
        add("life_masterwork", "livelihood", "명품 제작", "향후 전문 작업대 제작의 고급 도면을 해금합니다.", 3,
                "life_artisan");

        add("society_citizen_ties", "society", "시민 연줄", "경미한 재산 범죄의 수배도 증가량이 1 감소합니다.", 1);
        add("society_escape_routes", "society", "탈출 경로", "관할 밖에 머물면 수배도가 서서히 감소합니다.", 2,
                "society_citizen_ties");
        add("society_respected", "society", "지역 명망", "향후 평판·협상 시스템의 상위 선택지를 해금합니다.", 3,
                "society_escape_routes");

        add("arcana_attunement", "arcana", "마력 감응", "마법 계열 성장의 기초를 엽니다.", 1);
        add("arcana_channeling", "arcana", "마력 순환", "향후 주문 자원 회복 속도를 높입니다.", 2,
                "arcana_attunement");
        add("arcana_spellcraft", "arcana", "주문 설계", "향후 주문 제작과 룬 조합의 상위 도면을 해금합니다.", 3,
                "arcana_channeling");
    }

    private SkillTreeCatalog() {
    }

    public static Map<String, SkillNode> nodes() {
        return Map.copyOf(NODES);
    }

    public static SkillNode node(String id) {
        return NODES.get(id);
    }

    public static int initialPoints(String speciesId) {
        return "human".equals(speciesId) ? 4 : 3;
    }

    public static int effectiveCost(SkillNode node, String speciesId) {
        int discount = 0;
        if ("elf".equals(speciesId) && "exploration".equals(node.branch())) discount = 1;
        if ("dwarf".equals(speciesId) && "livelihood".equals(node.branch())) discount = 1;
        return Math.max(1, node.cost() - discount);
    }

    public static String speciesTraitTitle(String speciesId) {
        return switch (speciesId) {
            case "elf" -> "수림 감각";
            case "dwarf" -> "석골과 장인 기질";
            default -> "다재다능";
        };
    }

    public static String speciesTraitDescription(String speciesId) {
        return switch (speciesId) {
            case "elf" -> "어둠 속 시야를 확보하며 탐험 계열 기술 비용이 1 감소합니다.";
            case "dwarf" -> "받는 피해가 5% 감소하며 생활·제작 계열 기술 비용이 1 감소합니다.";
            default -> "다른 종족보다 초기 기술 점수를 1점 더 받습니다.";
        };
    }

    private static void add(String id, String branch, String title, String description, int cost,
                            String... prerequisites) {
        SkillNode node = new SkillNode(id, branch, title, description, cost, List.of(prerequisites));
        if (NODES.putIfAbsent(id, node) != null) {
            throw new IllegalStateException("Duplicate skill node: " + id);
        }
    }

    public record SkillNode(String id, String branch, String title, String description,
                            int cost, List<String> prerequisites) {
        public SkillNode {
            prerequisites = List.copyOf(prerequisites);
        }
    }
}
