package com.pgmacdesign.mcwaterslides.funnel;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The heart of a tornado funnel: a solid collar block sitting at the EXIT throat's
 * bottom-center, FACING the direction riders fire out. On placement it stamps the
 * side-lying half-cone shell behind itself ({@link FunnelShape}); on removal it clears it.
 * Riders skid over this block on their way out — place it level with the slide you want
 * to catch them. The ride itself is driven per-rider by the ride ticker via
 * {@link FunnelFields}; this block only anchors the frame and shapes the walls.
 */
public class FunnelCoreBlock extends Block implements EntityBlock {
    public static final EnumProperty<FunnelSize> SIZE = EnumProperty.create("size", FunnelSize.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public FunnelCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SIZE, FunnelSize.MEDIUM)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(FunnelCoreBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE, FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // exit fires the way the placer is looking — aim it at your catch slide
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockState wall = ModBlocks.FUNNEL_WALL.get().defaultBlockState();
            BlockState accent = ModBlocks.FUNNEL_WALL_ACCENT.get().defaultBlockState();
            for (FunnelShape.ShellCell c : FunnelShape.shellCells(pos, state.getValue(FACING), state.getValue(SIZE))) {
                if (level.getBlockState(c.pos()).canBeReplaced()) {
                    level.setBlock(c.pos(), c.accent() ? accent : wall, Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (FunnelShape.ShellCell c : FunnelShape.shellCells(pos, state.getValue(FACING), state.getValue(SIZE))) {
                BlockState cur = level.getBlockState(c.pos());
                if (cur.is(ModBlocks.FUNNEL_WALL.get()) || cur.is(ModBlocks.FUNNEL_WALL_ACCENT.get())) {
                    level.removeBlock(c.pos(), false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FunnelBlockEntity(pos, state);
    }
}
