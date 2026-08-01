package kr.moonseungjun.arcanecircle.item;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class SpellbookItem extends Item {
    private final String spellId;

    public SpellbookItem(Properties properties, String spellId) {
        super(properties.stacksTo(1));
        this.spellId = spellId;
    }

    public String spellId() {
        return spellId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null) return InteractionResult.FAIL;
        MagicPlayerData.LearnResult learned = MagicPlayerData.get(serverLevel.getServer()).learnSpell(serverPlayer, spellId);
        if (!learned.learned()) {
            serverPlayer.sendSystemMessage(Component.literal("§c[주문서] §f" + learned.message()));
            return InteractionResult.FAIL;
        }
        if (!serverPlayer.hasInfiniteMaterials()) serverPlayer.getItemInHand(hand).shrink(1);
        serverPlayer.sendSystemMessage(Component.literal("§6[주문 습득] §f" + spell.circle() + "써클 §e"
                + spell.name() + "§f의 완성 회로를 마력핵에 각인했습니다."));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell != null) {
            tooltip.accept(Component.literal("§5[" + spell.circle() + "써클 주문서] §f" + spell.name()));
            tooltip.accept(Component.literal("§7" + spell.school().displayName() + " · " + spell.sigilAnchor().displayName()));
            tooltip.accept(Component.literal("§8" + spell.description()));
            tooltip.accept(Component.literal("§6우클릭하여 소모하고 영구 습득"));
            tooltip.accept(Component.literal("§7권장 거래 가치: 에메랄드 환산 "
                    + SpellCatalog.emeraldEquivalentPrice(spell.circle()) + "개"));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
