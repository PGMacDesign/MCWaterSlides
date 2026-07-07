package com.pgmacdesign.mcwaterslides.slide;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The slide exit: a shallow basin that kills momentum smoothly and negates all landing
 * damage on contact. Tiles into larger pools — a side wall exists only where no pool
 * neighbors that side (fence logic, inverted). Momentum handling lives in the ride
 * ticker; the landing rule lives in the fall event.
 */
public class SplashPoolBlock extends Block implements SlideSurface {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final Map<Direction, BooleanProperty> BY_DIRECTION = new EnumMap<>(Direction.class);

    static {
        BY_DIRECTION.put(Direction.NORTH, NORTH);
        BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        BY_DIRECTION.put(Direction.EAST, EAST);
        BY_DIRECTION.put(Direction.WEST, WEST);
    }

    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);
    private static final Map<Direction, VoxelShape> WALL = new EnumMap<>(Direction.class);

    static {
        WALL.put(Direction.NORTH, Block.box(0, 2, 0, 16, 8, 2));
        WALL.put(Direction.SOUTH, Block.box(0, 2, 14, 16, 8, 16));
        WALL.put(Direction.WEST, Block.box(0, 2, 0, 2, 8, 16));
        WALL.put(Direction.EAST, Block.box(14, 2, 0, 16, 8, 16));
    }

    private final VoxelShape[] shapeByMask = new VoxelShape[16];

    public SplashPoolBlock(Properties properties) {
        super(properties);
        BlockState base = stateDefinition.any();
        for (BooleanProperty prop : BY_DIRECTION.values()) {
            base = base.setValue(prop, false);
        }
        registerDefaultState(base);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> entry : BY_DIRECTION.entrySet()) {
            state = state.setValue(entry.getValue(),
                    context.getLevel().getBlockState(context.getClickedPos().relative(entry.getKey()))
                            .getBlock() instanceof SplashPoolBlock);
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = BY_DIRECTION.get(direction);
        if (prop == null) {
            return state;
        }
        return state.setValue(prop, neighborState.getBlock() instanceof SplashPoolBlock);
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

    private VoxelShape shapeFor(BlockState state) {
        int mask = 0;
        int bit = 0;
        for (BooleanProperty prop : BY_DIRECTION.values()) {
            if (state.getValue(prop)) {
                mask |= 1 << bit;
            }
            bit++;
        }
        VoxelShape cached = shapeByMask[mask];
        if (cached == null) {
            VoxelShape shape = FLOOR;
            for (Map.Entry<Direction, BooleanProperty> entry : BY_DIRECTION.entrySet()) {
                if (!state.getValue(entry.getValue())) {
                    shape = Shapes.or(shape, WALL.get(entry.getKey()));
                }
            }
            cached = shapeByMask[mask] = shape;
        }
        return cached;
    }
}
