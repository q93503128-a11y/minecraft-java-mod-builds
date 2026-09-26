package kr.moonseungjun.campfiresessions.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChairBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ChairBlock> CODEC = simpleCodec(ChairBlock::new);
    private static final VoxelShape SOUTH = Shapes.or(
            Block.box(4.0, 6.5, 4.0, 12.0, 9.0, 12.0),
            Block.box(4.0, 9.0, 10.0, 12.0, 16.0, 12.0),
            Block.box(4.0, 0.0, 4.0, 5.5, 6.5, 5.5),
            Block.box(10.5, 0.0, 4.0, 12.0, 6.5, 5.5),
            Block.box(4.0, 0.0, 10.5, 5.5, 6.5, 12.0),
            Block.box(10.5, 0.0, 10.5, 12.0, 6.5, 12.0)
    );
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(SOUTH);

    public ChairBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
