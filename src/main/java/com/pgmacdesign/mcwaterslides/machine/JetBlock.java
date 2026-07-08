package com.pgmacdesign.mcwaterslides.machine;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock;
import com.pgmacdesign.mcwaterslides.slide.TubeShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The mod's physics primitive: pushes riders through the water/channel current ahead of
 * its facing (all 6 directions — straight up is the uphill/geyser story). Hopper-style
 * redstone: a signal DISABLES the jet. {@code ENERGIZED} mirrors the server's RF gate
 * into the blockstate so clients can predict thrust without knowing buffer contents.
 */
public class JetBlock extends DirectionalBlock implements EntityBlock {
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty ENERGIZED = BooleanProperty.create("energized");

    public JetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ENABLED, true)
                .setValue(ENERGIZED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(JetBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, ENERGIZED);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Aim along the player's view — except when mounting against a slide, where the
        // thrust projects onto the run's axis (sign from the look direction): hidden
        // side/floor jets push along the slide, not into it.
        Direction facing = context.getNearestLookingDirection();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        Direction projected = runProjectedFacing(level.getBlockState(clicked), context.getRotation());
        if (projected == null) {
            // Not placed onto a slide face, but dropped beside/under/over one — still aim
            // ALONG the run so a jet touching a slide just works (the common "row of jets
            // next to the slide" build). Without this the jet keeps the look direction and
            // pushes sideways/up, energizing the water but adding no forward thrust.
            projected = adjacentRunFacing(level, context.getClickedPos(), context.getRotation());
        }
        if (projected != null) {
            facing = projected;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    /** Run-projected facing from any adjacent slide (first neighbor that resolves an axis). */
    @Nullable
    private static Direction adjacentRunFacing(Level level, BlockPos pos, float yawDegrees) {
        for (Direction d : Direction.values()) {
            Direction projected = runProjectedFacing(level.getBlockState(pos.relative(d)), yawDegrees);
            if (projected != null) {
                return projected;
            }
        }
        return null;
    }

    /** The run-projected thrust direction, or null when the clicked block isn't a slide run. */
    @Nullable
    private static Direction runProjectedFacing(BlockState clicked, float yawDegrees) {
        Direction.Axis axis;
        if (clicked.getBlock() instanceof SlideChannelBlock) {
            axis = runAxis(clicked.getValue(SlideChannelBlock.SHAPE));
        } else if (clicked.getBlock() instanceof SlideTubeBlock) {
            TubeShape shape = clicked.getValue(SlideTubeBlock.SHAPE);
            if (shape == TubeShape.VERTICAL) {
                return Direction.UP; // shaft mount: the geyser/climb story
            }
            axis = runAxis(shape.toRail());
        } else {
            return null;
        }
        if (axis == null) {
            return null; // corners span two axes — keep aim-where-you-look
        }
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        double component = axis == Direction.Axis.X ? -Mth.sin(yaw) : Mth.cos(yaw);
        return Direction.fromAxisAndDirection(axis,
                component >= 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    @Nullable
    private static Direction.Axis runAxis(RailShape rail) {
        return switch (rail) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> Direction.Axis.Z;
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.Axis.X;
            default -> null;
        };
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean enabled = !level.hasNeighborSignal(pos);
        if (enabled != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, enabled), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof JetBlockEntity jet) {
            // Any neighborhood change may have altered the water ahead — recheck lazily.
            jet.invalidateField();
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JetBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.JET.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> JetBlockEntity.serverTick(lvl, pos, st, (JetBlockEntity) be);
    }
}
