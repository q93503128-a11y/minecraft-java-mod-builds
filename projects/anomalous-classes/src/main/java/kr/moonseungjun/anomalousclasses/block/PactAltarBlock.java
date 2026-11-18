package kr.moonseungjun.anomalousclasses.block;

import java.util.List;
import kr.moonseungjun.anomalousclasses.state.PactService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Stable blockstate ID ordering. Never reorder: placed altars persist in old worlds. */
public final class PactAltarBlock extends Block {
    public static final IntegerProperty PATRON = IntegerProperty.create("patron", 0, 15);
    private static final List<String> IDS = List.of(
            "isael", "velkan", "urgen", "ridna", "karda", "nerea", "shero", "hastel",
            "sevenwing", "merzad", "taron", "arken", "nahar", "rakiel", "ormas", "viar");

    public PactAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PATRON, 0));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PATRON);
    }
    public static String patronId(BlockState state) {
        return state.hasProperty(PATRON) ? IDS.get(state.getValue(PATRON)) : "";
    }
    public static int index(String patron) {
        return IDS.indexOf(patron);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            if (!PactService.isPact(server)) {
                server.displayClientMessage(Component.translatable(
                        "message.anomalousclasses.pact.not_caster_altar"), true);
            } else {
                String patron = patronId(state);
                if (index(patron) >= 0) PactService.previewAtAltar(server, patron, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
