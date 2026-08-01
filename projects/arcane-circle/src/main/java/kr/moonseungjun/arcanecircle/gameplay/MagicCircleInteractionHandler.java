package kr.moonseungjun.arcanecircle.gameplay;

import kr.moonseungjun.arcanecircle.magic.MagicRune;
import kr.moonseungjun.arcanecircle.magic.SpellRecipe;
import kr.moonseungjun.arcanecircle.registry.ModBlocks;
import kr.moonseungjun.arcanecircle.world.ArcaneCircleWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MagicCircleInteractionHandler {
    private MagicCircleInteractionHandler() {}

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.MAGIC_CIRCLE.get())) return;
        ItemStack heldItem = event.getItemStack();
        Optional<MagicRune> rune = MagicRune.fromStack(heldItem);
        if (!heldItem.isEmpty() && rune.isEmpty()) return;
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getPlayer() != null) {
            handleServerInteraction(serverLevel, event.getPos(), event.getPlayer(), heldItem, rune);
        }
        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
    }

    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!event.getState().is(ModBlocks.MAGIC_CIRCLE.get())) return;
        ArcaneCircleWorldData.get(serverLevel.getServer()).remove(event.getPos());
    }

    private static void handleServerInteraction(ServerLevel level, BlockPos pos, Player player,
                                                 ItemStack heldItem, Optional<MagicRune> rune) {
        ArcaneCircleWorldData data = ArcaneCircleWorldData.get(level.getServer());
        if (heldItem.isEmpty()) {
            if (player.isShiftKeyDown()) {
                refundRunes(player, data.clear(pos));
                player.sendOverlayMessage(Component.translatable("message.arcanecircle.cleared"));
                return;
            }
            List<Integer> runes = data.runes(pos);
            if (runes.size() < 3) {
                player.sendOverlayMessage(Component.translatable("message.arcanecircle.status", runes.size(), describe(runes)));
                return;
            }
            Optional<SpellRecipe> recipe = SpellRecipe.match(runes);
            if (recipe.isEmpty()) {
                SpellRecipe.drawFailure(level, pos);
                player.sendOverlayMessage(Component.translatable("message.arcanecircle.unstable", describe(runes)));
                return;
            }
            SpellRecipe spell = recipe.get();
            int affected = spell.cast(level, pos, player);
            data.clear(pos);
            player.sendSystemMessage(Component.translatable("message.arcanecircle.cast", spell.displayName(), affected));
            return;
        }
        MagicRune selectedRune = rune.orElseThrow();
        int slot = data.addRune(pos, selectedRune.code());
        if (slot < 0) {
            player.sendOverlayMessage(Component.translatable("message.arcanecircle.full"));
            return;
        }
        if (!player.getAbilities().instabuild) heldItem.shrink(1);
        SpellRecipe.drawInsertionPulse(level, pos, slot);
        player.sendOverlayMessage(Component.translatable("message.arcanecircle.inserted", slot, selectedRune.displayName()));
    }

    private static String describe(List<Integer> runeCodes) {
        if (runeCodes.isEmpty()) return "비어 있음";
        return runeCodes.stream().map(MagicRune::fromCode)
                .map(optional -> optional.map(MagicRune::displayName).orElse("알 수 없는 룬"))
                .collect(Collectors.joining(" → "));
    }

    private static void refundRunes(Player player, List<Integer> runeCodes) {
        for (int code : runeCodes) {
            MagicRune.fromCode(code).ifPresent(rune -> {
                ItemStack refund = rune.item().getDefaultInstance();
                if (!player.addItem(refund)) player.drop(refund, false);
            });
        }
    }
}
