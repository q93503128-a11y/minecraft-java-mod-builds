package kr.moonseungjun.survivalascension.infrastructure;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public enum InfrastructureProject {
    QUARRY_NETWORK(
            "quarry_network", "채석장 네트워크", "채굴 Lv.90 터널 굴착 5×5×8",
            List.of(
                    new Requirement(Items.COBBLESTONE, "조약돌", 1024),
                    new Requirement(Items.IRON_INGOT, "철 주괴", 256),
                    new Requirement(Items.REDSTONE, "레드스톤", 128),
                    new Requirement(Items.DIAMOND, "다이아몬드", 32)
            )),
    IRRIGATION_WORKS(
            "irrigation_works", "관개 시설", "농사 Lv.30 실제 씨앗 소비 자동 재파종",
            List.of(
                    new Requirement(Items.COPPER_INGOT, "구리 주괴", 512),
                    new Requirement(Items.IRON_INGOT, "철 주괴", 128),
                    new Requirement(Items.REDSTONE, "레드스톤", 128),
                    new Requirement(Items.GLASS, "유리", 128),
                    new Requirement(Items.SLIME_BALL, "슬라임볼", 32)
            )),
    BUILDER_FOUNDRY(
            "builder_foundry", "건축 공방", "건축 Lv.90 입체 채우기 5×5×5",
            List.of(
                    new Requirement(Items.STONE_BRICKS, "석재 벽돌", 1024),
                    new Requirement(Items.IRON_INGOT, "철 주괴", 256),
                    new Requirement(Items.COPPER_INGOT, "구리 주괴", 256),
                    new Requirement(Items.REDSTONE, "레드스톤", 128),
                    new Requirement(Items.OBSIDIAN, "흑요석", 64)
            )),
    COMBAT_ACADEMY(
            "combat_academy", "전투 훈련장", "전투 Lv.90 질주 충격파 · 반경 5.5블록 최대 12체",
            List.of(
                    new Requirement(Items.IRON_INGOT, "철 주괴", 512),
                    new Requirement(Items.GOLD_INGOT, "금 주괴", 256),
                    new Requirement(Items.EMERALD, "에메랄드", 128),
                    new Requirement(Items.REDSTONE, "레드스톤", 128),
                    new Requirement(Items.ECHO_SHARD, "메아리 조각", 32)
            ));

    private final String id;
    private final String koreanName;
    private final String benefit;
    private final List<Requirement> requirements;

    InfrastructureProject(String id, String koreanName, String benefit, List<Requirement> requirements) {
        this.id = id;
        this.koreanName = koreanName;
        this.benefit = benefit;
        this.requirements = requirements;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public String benefit() { return benefit; }
    public List<Requirement> requirements() { return requirements; }

    public static InfrastructureProject fromId(String id) {
        for (InfrastructureProject project : values()) if (project.id.equals(id)) return project;
        return null;
    }

    public record Requirement(Item item, String label, int amount) {}
}
