package com.pgmacdesign.mcwaterslides.funnel;

import javax.annotation.Nullable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One item per funnel size; the size is baked into the placed blockstate. */
public class FunnelCoreItem extends BlockItem {
    private final FunnelSize size;

    public FunnelCoreItem(Block block, FunnelSize size, Properties properties) {
        super(block, properties);
        this.size = size;
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(FunnelCoreBlock.SIZE, size);
    }

    /** Per-size hover name (BlockItems default to the block's single name otherwise). */
    @Override
    public String getDescriptionId() {
        return "item.mcwaterslides.funnel_core_" + size.getSerializedName();
    }
}
