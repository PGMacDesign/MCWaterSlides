package com.pgmacdesign.mcwaterslides.slide;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.ride.SlidePhysics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The enclosed tube: a lidded channel with the same auto-connect vocabulary plus
 * {@link TubeShape#VERTICAL} shafts (drops and jet climbs). Riding one = committed —
 * that falls out of the enclosure geometry (the lid has collision), never a flag.
 *
 * Interior is ~12px: players fit because vanilla auto-crawl (0.6 blocks) engages under
 * the lid; most mobs simply don't fit horizontal tubes (chickens do). Vertical shafts
 * fit everything 0.6 wide.
 */
public class SlideTubeBlock extends Block implements SlideSurface {
    public static final EnumProperty<TubeShape> SHAPE = EnumProperty.create("shape", TubeShape.class);

    private static final Map<TubeShape, VoxelShape> SHAPES = new EnumMap<>(TubeShape.class);

    static {
        VoxelShape lid = Block.box(0, 14, 0, 16, 16, 16);
        for (TubeShape shape : TubeShape.values()) {
            if (shape == TubeShape.VERTICAL) {
                SHAPES.put(shape, Shapes.or(
                        Block.box(0, 0, 0, 2, 16, 16),
                        Block.box(14, 0, 0, 16, 16, 16),
                        Block.box(2, 0, 0, 14, 16, 2),
                        Block.box(2, 0, 14, 14, 16, 16)));
            } else {
                RailShape rail = shape.toRail();
                VoxelShape base = SlideChannelShapes.shape(rail);
                // Ascending shapes already run full-height walls; a lid would seal the
                // stepped entry. Flat runs and corners get the 2px lid.
                boolean ascending = SlidePhysics.ascentDirection(rail) != null;
                SHAPES.put(shape, ascending ? base : Shapes.or(base, lid));
            }
        }
    }

    @Nullable
    private final DyeColor color;

    public SlideTubeBlock(@Nullable DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(SHAPE, TubeShape.NORTH_SOUTH));
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
        return defaultBlockState().setValue(SHAPE,
                computeShape(context.getLevel(), context.getClickedPos(),
                        context.getHorizontalDirection().getAxis()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            // Self-refresh covers raw setBlock placements (pistons, commands, tests) that
            // bypass getStateForPlacement; a player placement is already computed (no-op).
            refresh(level, pos, state);
            SlideConnections.refreshNeighbors(level, pos);
            refreshVerticalNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !newState.is(this)) {
            SlideConnections.refreshNeighbors(level, pos);
            refreshVerticalNeighbors(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            refresh(level, pos, state);
        }
    }

    /** Recompute this tube's shape in place (no-op when unchanged). */
    public void refresh(Level level, BlockPos pos, BlockState state) {
        TubeShape computed = computeShape(level, pos, axisOf(state.getValue(SHAPE)));
        if (computed != state.getValue(SHAPE)) {
            level.setBlock(pos, state.setValue(SHAPE, computed), Block.UPDATE_ALL);
        }
    }

    /** Tubes directly above/below may need to flip to/from VERTICAL. */
    private static void refreshVerticalNeighbors(Level level, BlockPos pos) {
        for (BlockPos np : new BlockPos[]{pos.above(), pos.below()}) {
            BlockState ns = level.getBlockState(np);
            if (ns.getBlock() instanceof SlideTubeBlock tube) {
                tube.refresh(level, np, ns);
            }
        }
    }

    /**
     * Vertical wins when a slide sits directly above or below AND no same-level
     * horizontal neighbor claims this block for a run; otherwise the channel logic
     * decides (existence-based, oscillation-free — same guarantees as channels).
     */
    private TubeShape computeShape(LevelReader level, BlockPos pos, Direction.Axis fallbackAxis) {
        boolean above = SlideConnections.isSlide(level.getBlockState(pos.above()));
        boolean below = SlideConnections.isSlide(level.getBlockState(pos.below()));
        if (above || below) {
            boolean horizontal = false;
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                if (SlideConnections.isSlide(level.getBlockState(pos.relative(d)))) {
                    horizontal = true;
                    break;
                }
            }
            if (!horizontal) {
                return TubeShape.VERTICAL;
            }
        }
        return TubeShape.fromRail(SlideConnections.computeShape(level, pos, fallbackAxis));
    }

    private static Direction.Axis axisOf(TubeShape shape) {
        return switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.Axis.X;
            default -> Direction.Axis.Z;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SHAPE));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SHAPE));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
