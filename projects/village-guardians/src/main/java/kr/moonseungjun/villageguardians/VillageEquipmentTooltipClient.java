package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Adds the actual Village Guardians stat, weapon-family and set contribution to every graded gear tooltip. */
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
        float flatMelee = VillageEquipmentRaritySystem.flatAttackBonus(stack, false);
        float flatProjectile = VillageEquipmentRaritySystem.flatAttackBonus(stack, true);
        String flatText = flatMelee > 0.0f
                ? String.format(java.util.Locale.ROOT, " · 기본 근접 피해 +%.1f", flatMelee)
                : flatProjectile > 0.0f
                ? String.format(java.util.Locale.ROOT, " · 기본 원거리 피해 +%.1f", flatProjectile)
                : "";
        event.getToolTip().add(Component.literal("• 전장 단계: "
                + VillageEquipmentRaritySystem.combatTierName(stack) + flatText)
                .withStyle(ChatFormatting.YELLOW));
        event.getToolTip().add(Component.literal("• "
                + VillageEquipmentRaritySystem.enhancementEffectSummary(stack, enhancement))
                .withStyle(ChatFormatting.GRAY));
        String style = VillageWeaponStyleSystem.tooltip(stack);
        if (!style.isBlank()) {
            event.getToolTip().add(Component.literal("• 무기 계열: " + style).withStyle(ChatFormatting.DARK_AQUA));
        }
        VillageEquipmentSetSystem.EquipmentSet set = VillageEquipmentSetSystem.setOf(stack);
        if (set != null) {
            var player = Minecraft.getInstance().player;
            int count = player == null ? 0 : VillageEquipmentSetSystem.countEquipped(player, set);
            event.getToolTip().add(Component.literal("• 세트: " + set.displayName() + " " + count + "/5")
                    .withStyle(ChatFormatting.GOLD));
            for (int required : new int[]{2, 3, 4, 5}) {
                boolean active = count >= required;
                ChatFormatting color = active
                        ? required == 5 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW
                        : ChatFormatting.DARK_GRAY;
                event.getToolTip().add(Component.literal(
                        "  " + (active ? "◆ " : "◇ ") + required + "셋 · " + set.pieceEffect(required))
                        .withStyle(color));
                if (required == 5 && !set.capstoneText().isBlank()) {
                    event.getToolTip().add(Component.literal("      └ " + set.capstoneText())
                            .withStyle(active ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
                }
            }
        }
        event.getToolTip().add(Component.literal("• 등급: " + rarity.displayName()
                + (enhancement > 0 ? "  ·  강화 +" + enhancement : ""))
                .withStyle(rarity.formatting()));
        event.getToolTip().add(Component.literal("전장 단계는 기본 피해, 등급·강화는 배율을 올립니다. 같은 영웅 장비도 후반 단계가 더 강합니다.")
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.literal("다음 강화 수치와 가능 단계는 대장간에서 확인")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
