package kr.moonseungjun.survivalascension.infrastructure;

import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public enum InfrastructureProject {
    QUARRY_NETWORK(
            "quarry_network", "채석장 네트워크", "채굴 Lv.90 터널 5×5×8 · Lv.100 7×7×10", 0,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 48, 1),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 2),
                    exact("diamond", Items.DIAMOND, "다이아몬드", 6, 3)
            )),
    IRRIGATION_WORKS(
            "irrigation_works", "관개 시설", "농사 Lv.30 실제 씨앗 소비 자동 재파종", 0,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 120, 0, 1),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 2),
                    exact("glass", Items.GLASS, "유리", 32, 3),
                    exact("slime", Items.SLIME_BALL, "슬라임볼", 8, 4)
            )),
    BUILDER_FOUNDRY(
            "builder_foundry", "건축 공방", "건축 Lv.90 입체 5×5×5 · Lv.100 7×7×7", 0,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 96, 1, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 3),
                    exact("obsidian", Items.OBSIDIAN, "흑요석", 12, 4)
            )),
    COMBAT_ACADEMY(
            "combat_academy", "전투 훈련장", "전투 Lv.90 질주 전방 균열선 6.5블록/10체 · Lv.100 8블록/14체 · 현장 숙련 10블록/18체", 0,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 144, 0, 1),
                    exact("emerald", Items.EMERALD, "에메랄드", 16, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 24, 3),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 4, 4)
            )),
    CIVIL_WORKS(
            "civil_works", "토목 공사소", "전설 단계 · 건축 Lv.60 3폭 도로/교량 17칸 → Lv.90 33 · Lv.100 49 · 현장 숙련 65", 1,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 896, 0, 1, 2),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 96, 3, 4)
            )),
    INDUSTRIAL_WORKS(
            "industrial_works", "산업 가공소", "전설 단계 · 채굴·벌목·농사·정밀자원을 4계통 대량 생산망으로 연결", 1,
            List.of(
                    shared("stone", SharedEconomyCompat.ResourceCategory.STONE, 192, 0),
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 192, 1, 2),
                    exact("redstone", Items.REDSTONE, "레드스톤", 48, 3),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 24, 4)
            )),
    APEX_TRACKING_POST(
            "apex_tracking_post", "정점 추적소", "전설 단계 · 완수한 원정권에서 반복 정점 사냥 개방", 1,
            List.of(
                    shared("metal", SharedEconomyCompat.ResourceCategory.METAL, 144, 0, 1),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 48, 2),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 4, 3),
                    exact("nether_star", Items.NETHER_STAR, "네더의 별", 1, 4)
            )),
    ASCENSION_NEXUS(
            "ascension_nexus", "승천 중추", "종말 단계 · 기동 Lv.90 공중 돌진 2회 / Lv.100 3회 · 완공 후 승천 시련", 2,
            List.of(
                    exact("nether_star", Items.NETHER_STAR, "네더의 별", 1, 0),
                    exact("dragon_breath", Items.DRAGON_BREATH, "드래곤의 숨결", 8, 1),
                    exact("obsidian", Items.OBSIDIAN, "흑요석", 64, 2),
                    exact("amethyst", Items.AMETHYST_SHARD, "자수정 조각", 64, 3),
                    exact("echo_shard", Items.ECHO_SHARD, "메아리 조각", 8, 4)
            ));

    private final String id;
    private final String koreanName;
    private final String benefit;
    private final int requiredWorldStage;
    private final List<Requirement> requirements;

    InfrastructureProject(String id, String koreanName, String benefit, int requiredWorldStage, List<Requirement> requirements) {
        this.id = id;
        this.koreanName = koreanName;
        this.benefit = benefit;
        this.requiredWorldStage = requiredWorldStage;
        this.requirements = requirements;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public String benefit() { return benefit; }
    public int requiredWorldStage() { return requiredWorldStage; }
    public List<Requirement> requirements() { return requirements; }

    public static InfrastructureProject fromId(String id) {
        for (InfrastructureProject project : values()) if (project.id.equals(id)) return project;
        return null;
    }

    private static Requirement shared(String key, SharedEconomyCompat.ResourceCategory category, int amount, Integer... legacyIndices) {
        return new Requirement(key, category.koreanName() + " 재화", amount, null, category, List.of(legacyIndices));
    }

    private static Requirement exact(String key, Item item, String label, int amount, Integer... legacyIndices) {
        return new Requirement(key, label, amount, item, null, List.of(legacyIndices));
    }

    public record Requirement(
            String key,
            String label,
            int amount,
            Item item,
            SharedEconomyCompat.ResourceCategory resourceCategory,
            List<Integer> legacyIndices
    ) {
        public boolean isSharedResource() { return resourceCategory != null; }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            return resourceCategory != null ? SharedEconomyCompat.matches(resourceCategory, stack) : item != null && stack.is(item);
        }
    }
}
