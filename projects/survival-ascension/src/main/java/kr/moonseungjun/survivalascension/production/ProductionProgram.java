package kr.moonseungjun.survivalascension.production;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public enum ProductionProgram {
    METALWORKS(
            "metalworks", "제련 배치",
            List.of(
                    Input.item(Items.RAW_IRON, "철 원석", 96),
                    Input.item(Items.RAW_COPPER, "구리 원석", 96),
                    Input.item(Items.COAL, "석탄", 64)
            )),
    TIMBERWORKS(
            "timberworks", "구조재 배치",
            List.of(
                    Input.tag(ItemTags.LOGS, "통나무", 192),
                    Input.item(Items.COBBLESTONE, "조약돌", 384),
                    Input.item(Items.IRON_INGOT, "철 주괴", 32)
            )),
    PROVISIONS(
            "provisions", "식량 배치",
            List.of(
                    Input.item(Items.WHEAT, "밀", 128),
                    Input.item(Items.CARROT, "당근", 64),
                    Input.item(Items.POTATO, "감자", 64),
                    Input.item(Items.BEETROOT, "비트", 32)
            )),
    PRECISION(
            "precision", "정밀 부품 배치",
            List.of(
                    Input.item(Items.REDSTONE, "레드스톤", 128),
                    Input.item(Items.AMETHYST_SHARD, "자수정 조각", 64),
                    Input.item(Items.GOLD_INGOT, "금 주괴", 32),
                    Input.item(Items.QUARTZ, "네더 석영", 64)
            ));

    private final String id;
    private final String koreanName;
    private final List<Input> inputs;

    ProductionProgram(String id, String koreanName, List<Input> inputs) {
        this.id = id;
        this.koreanName = koreanName;
        this.inputs = inputs;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public List<Input> inputs() { return inputs; }

    public static ProductionProgram fromId(String id) {
        for (ProductionProgram program : values()) if (program.id.equals(id)) return program;
        return null;
    }

    public record Input(Item item, TagKey<Item> tag, String label, int amount) {
        public static Input item(Item item, String label, int amount) { return new Input(item, null, label, amount); }
        public static Input tag(TagKey<Item> tag, String label, int amount) { return new Input(null, tag, label, amount); }
        public boolean matches(ItemStack stack) { return item != null ? stack.is(item) : tag != null && stack.is(tag); }
    }
}
