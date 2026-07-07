package com.pgmacdesign.mcwaterslides.machine;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
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
        // Aim along the player's view: the jet pushes where you're looking.
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection())
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
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
