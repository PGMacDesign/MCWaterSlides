package com.pgmacdesign.mcwaterslides.machine;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Waterproof RF cable along the slide spine — fence-style connected block whose six
 * side booleans drive a multipart model. Connects to other conduits and to any block
 * exposing energy on the touched face (this mod's machines and any FE mod's).
 * Structure ported from MC3DPrint's MC3D Cable; transport lives in the block entity.
 */
public class WaterConduitBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<WaterConduitBlock> CODEC = simpleCodec(WaterConduitBlock::new);

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Map<Direction, BooleanProperty> BY_DIRECTION = new EnumMap<>(Direction.class);

    static {
        BY_DIRECTION.put(Direction.NORTH, NORTH);
        BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        BY_DIRECTION.put(Direction.EAST, EAST);
        BY_DIRECTION.put(Direction.WEST, WEST);
        BY_DIRECTION.put(Direction.UP, UP);
        BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARM = new EnumMap<>(Direction.class);

    static {
        ARM.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARM.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
        ARM.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARM.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARM.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARM.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
    }

    private final VoxelShape[] shapeByMask = new VoxelShape[64];

    public WaterConduitBlock(Properties properties) {
        super(properties);
        BlockState base = stateDefinition.any().setValue(WATERLOGGED, false);
        for (BooleanProperty prop : BY_DIRECTION.values()) {
            base = base.setValue(prop, false);
        }
        registerDefaultState(base);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterConduitBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WATER_CONDUIT.get(), WaterConduitBlockEntity::serverTick);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(BY_DIRECTION.get(dir), canConnectTo(level, pos.relative(dir), dir));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (level instanceof Level realLevel) {
            return state.setValue(BY_DIRECTION.get(direction), canConnectTo(realLevel, neighborPos, direction));
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private static boolean canConnectTo(Level level, BlockPos neighborPos, Direction dirToNeighbor) {
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof WaterConduitBlock) {
            return true;
        }
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dirToNeighbor.getOpposite()) != null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(BY_DIRECTION.get(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        VoxelShape cached = shapeByMask[mask];
        if (cached == null) {
            VoxelShape shape = CORE;
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.ordinal())) != 0) {
                    shape = Shapes.or(shape, ARM.get(dir));
                }
            }
            cached = shapeByMask[mask] = shape;
        }
        return cached;
    }
}
