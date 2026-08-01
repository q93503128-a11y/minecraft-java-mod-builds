package kr.moonseungjun.arcanecircle.item;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
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

public final class BeginnerGrimoireItem extends Item {
    public BeginnerGrimoireItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        MagicPlayerData.LearnResult learned = MagicPlayerData.get(serverLevel.getServer()).learnPrimer(serverPlayer);
        if (!learned.learned()) {
            serverPlayer.sendSystemMessage(Component.literal("§7[초심자 마도서] §f" + learned.message()));
            return InteractionResult.FAIL;
        }
        if (!serverPlayer.hasInfiniteMaterials()) serverPlayer.getItemInHand(hand).shrink(1);
        serverPlayer.sendSystemMessage(Component.literal(
                "§5[첫 각인] §f다섯 개의 §d1써클 기초 주문§f을 익혔습니다. 숫자키를 누르고 유지한 뒤 놓아 시전하세요."));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("§5[초심자 마도서]"));
        tooltip.accept(Component.literal("§f1써클 기초 주문 5종을 영구 습득한다."));
        tooltip.accept(Component.literal("§7" + SpellCatalog.primerSpells().stream().map(spell -> spell.name())
                .reduce((a, b) -> a + " · " + b).orElse("")));
        tooltip.accept(Component.literal("§6우클릭하여 소모하고 마력핵에 각인"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
