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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * Interior is ~12px: a lone tube is a crawl bore (the forced swim pose fits riders
 * through). Stacking a second tube of the same shape on top merges the pair into a
 * tall bore — the lower tube drops its lid ({@code OPEN_UP}), the upper drops its
 * floor ({@code OPEN_DOWN}). Horizontal connectivity wins over vertical stacking, so
 * bare columns still compute VERTICAL shafts.
 */
public class SlideTubeBlock extends Block implements SlideSurface {
    public static final EnumProperty<TubeShape> SHAPE = EnumProperty.create("shape", TubeShape.class);
    /** Lid dropped: a same-shape tube directly above forms a tall bore with this one. */
    public static final BooleanProperty OPEN_UP = BooleanProperty.create("open_up");
    /** Floor dropped: this tube is the ceiling half of a tall bore. */
    public static final BooleanProperty OPEN_DOWN = BooleanProperty.create("open_down");

    private static final Map<TubeShape, VoxelShape[]> SHAPES = new EnumMap<>(TubeShape.class);

    static {
        VoxelShape lid = Block.box(0, 14, 0, 16, 16, 16);
        for (TubeShape shape : TubeShape.values()) {
            VoxelShape[] variants = new VoxelShape[4];
            if (shape == TubeShape.VERTICAL) {
                VoxelShape shaft = Shapes.or(
                        Block.box(0, 0, 0, 2, 16, 16),
                        Block.box(14, 0, 0, 16, 16, 16),
                        Block.box(2, 0, 0, 14, 16, 2),
                        Block.box(2, 0, 14, 14, 16, 16));
                for (int i = 0; i < 4; i++) {
                    variants[i] = shaft;
                }
            } else {
                RailShape rail = shape.toRail();
                boolean ascending = SlidePhysics.ascentDirection(rail) != null;
                if (ascending) {
                    // Ascending shapes never pair (full-height walls, no lid to drop).
                    VoxelShape base = SlideChannelShapes.shape(rail);
                    for (int i = 0; i < 4; i++) {
                        variants[i] = base;
                    }
                } else {
                    VoxelShape walls = SlideChannelShapes.walls(rail);
                    for (int i = 0; i < 4; i++) {
                        boolean openUp = (i & 1) != 0;
                        boolean openDown = (i & 2) != 0;
                        VoxelShape v = openDown ? walls : Shapes.or(SlideChannelShapes.FLOOR, walls);
                        variants[i] = openUp ? v : Shapes.or(v, lid);
                    }
                }
            }
            SHAPES.put(shape, variants);
        }
    }

    @Nullable
    private final DyeColor color;

    public SlideTubeBlock(@Nullable DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, TubeShape.NORTH_SOUTH)
                .setValue(OPEN_UP, false)
                .setValue(OPEN_DOWN, false));
    }

    @Nullable
    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, OPEN_UP, OPEN_DOWN);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(defaultBlockState(), context.getLevel(), context.getClickedPos(),
                context.getHorizontalDirection().getAxis());
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

    /** Recompute this tube's shape + bore pairing in place (no-op when unchanged). */
    public void refresh(Level level, BlockPos pos, BlockState state) {
        BlockState computed = computeState(state, level, pos, axisOf(state.getValue(SHAPE)));
        if (computed != state) {
            level.setBlock(pos, computed, Block.UPDATE_ALL);
        }
    }

    /** Tubes directly above/below may need to flip to/from VERTICAL or re-pair bores. */
    private static void refreshVerticalNeighbors(Level level, BlockPos pos) {
        for (BlockPos np : new BlockPos[]{pos.above(), pos.below()}) {
            BlockState ns = level.getBlockState(np);
            if (ns.getBlock() instanceof SlideTubeBlock tube) {
                tube.refresh(level, np, ns);
            }
        }
    }

    private BlockState computeState(BlockState base, LevelReader level, BlockPos pos, Direction.Axis fallbackAxis) {
        TubeShape shape = computeShape(level, pos, fallbackAxis);
        boolean openUp = false;
        boolean openDown = false;
        RailShape rail = shape.toRail();
        if (rail != null && SlidePhysics.ascentDirection(rail) == null) {
            openUp = rail == pairRail(level, pos.above());
            openDown = rail == pairRail(level, pos.below());
        }
        return base.setValue(SHAPE, shape).setValue(OPEN_UP, openUp).setValue(OPEN_DOWN, openDown);
    }

    /**
     * The flat rail shape the tube at {@code pos} pairs with, or null if it isn't a
     * horizontal-run tube. Computed from the world (existence-based), never from the
     * neighbor's stored state — cascading refreshes stay deterministic.
     */
    @Nullable
    private static RailShape pairRail(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlideTubeBlock)) {
            return null;
        }
        if (!hasHorizontalSlideNeighbor(level, pos)) {
            return null; // would compute VERTICAL — shafts don't pair
        }
        RailShape rail = SlideConnections.computeShape(level, pos, axisOf(state.getValue(SHAPE)));
        return SlidePhysics.ascentDirection(rail) == null ? rail : null;
    }

    private static boolean hasHorizontalSlideNeighbor(LevelReader level, BlockPos pos) {
        for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            if (SlideConnections.isSlide(level.getBlockState(pos.relative(d)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vertical wins when a slide sits directly above or below AND no same-level
     * horizontal neighbor claims this block for a run; otherwise the channel logic
     * decides (existence-based, oscillation-free — same guarantees as channels).
     */
    private TubeShape computeShape(LevelReader level, BlockPos pos, Direction.Axis fallbackAxis) {
        boolean above = SlideConnections.isSlide(level.getBlockState(pos.above()));
        boolean below = SlideConnections.isSlide(level.getBlockState(pos.below()));
        if ((above || below) && !hasHorizontalSlideNeighbor(level, pos)) {
            return TubeShape.VERTICAL;
        }
        return TubeShape.fromRail(SlideConnections.computeShape(level, pos, fallbackAxis));
    }

    private static Direction.Axis axisOf(TubeShape shape) {
        return switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.Axis.X;
            default -> Direction.Axis.Z;
        };
    }

    private static VoxelShape shapeFor(BlockState state) {
        int i = (state.getValue(OPEN_UP) ? 1 : 0) | (state.getValue(OPEN_DOWN) ? 2 : 0);
        return SHAPES.get(state.getValue(SHAPE))[i];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
