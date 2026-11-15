package kr.moonseungjun.anomalousclasses.block;

import kr.moonseungjun.anomalousclasses.state.PuppetService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** First usable assembly action. The full workshop screen will host repairs/alterations. */
public final class PuppetWorkshopBlock extends Block {
    public PuppetWorkshopBlock(Properties properties) { super(properties); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            PuppetService.openWorkshop(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
