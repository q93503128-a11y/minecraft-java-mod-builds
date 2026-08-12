package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Adds the actual Village Guardians stat contribution to every graded gear tooltip. */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageEquipmentTooltipClient {
    private VillageEquipmentTooltipClient() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        VillageEquipmentRaritySystem.Rarity rarity = VillageEquipmentRaritySystem.rarityOf(stack);
        if (rarity == null) return;

        int enhancement = VillageEquipmentRaritySystem.enhancementLevel(stack);
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.literal("마을 지키기 장비 효과").withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.literal("• "
                + VillageEquipmentRaritySystem.enhancementEffectSummary(stack, enhancement))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal("• 등급: " + rarity.displayName()
                + (enhancement > 0 ? "  ·  강화 +" + enhancement : ""))
                .withStyle(rarity.formatting()));
        event.getToolTip().add(Component.literal("다음 강화 수치와 가능 단계는 대장간에서 확인")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
