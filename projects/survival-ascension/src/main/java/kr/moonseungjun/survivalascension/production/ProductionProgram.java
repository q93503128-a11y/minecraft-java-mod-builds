package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public enum ProductionProgram {
    METALWORKS(
            "metalworks", "제련 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 32),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 12)
            )),
    TIMBERWORKS(
            "timberworks", "구조재 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.WOOD, 32),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 64),
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 6)
            )),
    PROVISIONS(
            "provisions", "식량 배치",
            List.of(Input.resource(SharedEconomyCompat.ResourceCategory.FOOD, 54))),
    PRECISION(
            "precision", "정밀 부품 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 30),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 24)
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

    public record Input(SharedEconomyCompat.ResourceCategory category, String label, int amount) {
        public static Input resource(SharedEconomyCompat.ResourceCategory category, int amount) {
            return new Input(category, category.koreanName(), amount);
        }
        public boolean matches(ItemStack stack) { return SharedEconomyCompat.matches(category, stack); }
    }
}
