package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.settlement.SettlementService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class PioneerMarkerItem extends Item {
    public PioneerMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos markerPos = context.getClickedPos().relative(context.getClickedFace());
        SettlementService.FoundResult result = SettlementService.foundAt(player, markerPos);
        player.sendSystemMessage(Component.literal(result.message()));
        if (!result.founded()) return InteractionResult.FAIL;

        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
