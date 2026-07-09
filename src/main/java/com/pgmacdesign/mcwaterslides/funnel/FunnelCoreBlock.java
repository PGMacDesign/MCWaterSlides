package com.pgmacdesign.mcwaterslides.funnel;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The heart of a funnel: a passive drain grate placed at the bowl's center-bottom. On placement it
 * stamps a stepped bowl of {@link ModBlocks#FUNNEL_WALL} around itself (size S/M/L); on removal it
 * clears that bowl. Its own cell has NO collision — riders that spiral into the drain drop straight
 * through into whatever exit you build below (a vertical tube, an open shaft, or nothing → they
 * splash out). The swirl itself is driven per-rider by the ride ticker via {@link FunnelFields};
 * this block only marks the center and shapes the walls.
 */
public class FunnelCoreBlock extends Block implements EntityBlock {
    public static final EnumProperty<FunnelSize> SIZE = EnumProperty.create("size", FunnelSize.class);
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 3, 16);

    public FunnelCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SIZE, FunnelSize.MEDIUM));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(FunnelCoreBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIZE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // open drain — riders fall through the center
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockState wall = ModBlocks.FUNNEL_WALL.get().defaultBlockState();
            for (BlockPos c : FunnelShape.bowlCells(pos, state.getValue(SIZE))) {
                if (level.getBlockState(c).canBeReplaced()) {
                    level.setBlock(c, wall, Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (BlockPos c : FunnelShape.bowlCells(pos, state.getValue(SIZE))) {
                if (level.getBlockState(c).is(ModBlocks.FUNNEL_WALL.get())) {
                    level.removeBlock(c, false);
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
