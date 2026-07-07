package com.pgmacdesign.mcwaterslides.slide;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The open U-channel ride surface. Reuses the vanilla {@link RailShape} vocabulary
 * (straight / four corners / four ascending) but with our own connection logic:
 * NOT a BaseRailBlock, so no rails tag (minecarts ignore it), no support-block
 * requirement (slides float), and full control of collision.
 *
 * Water is intrinsic cauldron-style — rendered by the model (animated water_still
 * quad, biome-tinted), no FluidState. A real fluid here would spread over the
 * channel walls into the world; faking it also makes "can't be dried" free.
 */
public class SlideChannelBlock extends Block implements SlideSurface {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

    @Nullable
    private final DyeColor color;

    public SlideChannelBlock(@Nullable DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH));
    }

    @Nullable
    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        RailShape shape = SlideConnections.computeShape(
                context.getLevel(), context.getClickedPos(),
                context.getHorizontalDirection().getAxis());
        return defaultBlockState().setValue(SHAPE, shape);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            SlideConnections.refreshNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            SlideConnections.refreshNeighbors(level, pos);
        } else {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            SlideConnections.refreshSelf(level, pos, state);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SlideChannelShapes.shape(state.getValue(SHAPE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SlideChannelShapes.shape(state.getValue(SHAPE));
    }

    /** Channels are mostly open — light passes, mobs shouldn't consider them full. */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
