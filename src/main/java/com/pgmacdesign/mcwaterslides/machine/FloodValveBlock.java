package com.pgmacdesign.mcwaterslides.machine;

import java.util.List;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fills the sealed volume it faces with water (RF per block), drains it when the
 * redstone signal drops. Signal semantics are a real valve: powered = open = water in.
 * Mode is derived from the blockstate every tick — never latched.
 */
public class FloodValveBlock extends DirectionalBlock implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FloodValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(FloodValveBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mcwaterslides.flood_valve.what").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mcwaterslides.flood_valve.how").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.mcwaterslides.flood_valve.leaks").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Right-click: a live status readout — the valve has no inventory, so this is its "GUI". */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FloodValveBlockEntity valve) {
            FloodValveBlockEntity.Status st = valve.status(state);
            player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.status")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            player.displayClientMessage(Component.translatable(st.powered()
                    ? "message.mcwaterslides.flood_valve.mode_fill"
                    : "message.mcwaterslides.flood_valve.mode_drain").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.energy",
                    st.energy(), st.maxEnergy()).withStyle(ChatFormatting.GRAY), false);
            if (st.leak() != null) {
                player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.leak_at",
                        st.leak().getX(), st.leak().getY(), st.leak().getZ()).withStyle(ChatFormatting.RED), false);
            } else if (st.powered() && st.volume() == 0) {
                // Zero volume + no leak = the seed cell in front of the face is solid. The
                // green "0 blocks" read like success and confused testers; call it out.
                player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.no_target")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else if (st.powered()) {
                player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.volume",
                        st.volume()).withStyle(ChatFormatting.GREEN), false);
            }
            if (st.energy() <= 0) {
                player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.no_rf")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve.placed")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof FloodValveBlockEntity valve) {
            valve.invalidateScan();
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodValveBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.FLOOD_VALVE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> FloodValveBlockEntity.serverTick(lvl, pos, st, (FloodValveBlockEntity) be);
    }
}
