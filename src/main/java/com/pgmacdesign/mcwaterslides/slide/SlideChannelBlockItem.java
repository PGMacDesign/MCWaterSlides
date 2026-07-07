package com.pgmacdesign.mcwaterslides.slide;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Scaffolding-style placement for channels: their open-top U-shape means the crosshair
 * usually lands on an interior surface (the floor plate), which vanilla resolves as
 * "place above" — the classic stacked-on-top surprise. Clicking a slide vertically
 * instead continues the run along the player's look direction, stepping up over
 * obstacles so uphill runs extend naturally.
 *
 * Horizontal clicks on outer wall faces keep vanilla behavior — that's how side-by-side
 * wide slides are laid. Tubes deliberately keep the vanilla BlockItem: their lid-top
 * click is how tall bores are stacked.
 */
public class SlideChannelBlockItem extends BlockItem {
    /** How far a vertical click walks forward looking for the next spot in the run. */
    private static final int EXTEND_RANGE = 2;

    public SlideChannelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockPos clicked = target.relative(context.getClickedFace().getOpposite());
        if (!(level.getBlockState(clicked).getBlock() instanceof SlideSurface)) {
            return context;
        }
        if (context.getClickedFace().getAxis().isHorizontal()
                && level.getBlockState(target).canBeReplaced(context)) {
            return context; // outer-wall side click: lateral placement (wide slides)
        }
        Direction dir = context.getHorizontalDirection();
        BlockPos pos = clicked;
        for (int i = 0; i < EXTEND_RANGE; i++) {
            pos = pos.relative(dir);
            if (level.getBlockState(pos).canBeReplaced(context)) {
                return BlockPlaceContext.at(context, pos, dir);
            }
            if (level.getBlockState(pos).getBlock() instanceof SlideSurface) {
                continue; // mid-run click: keep walking toward the run's end
            }
            BlockPos above = pos.above();
            if (level.getBlockState(above).canBeReplaced(context)) {
                return BlockPlaceContext.at(context, above, dir); // climb the obstacle
            }
            break;
        }
        return context; // odd geometry: fall back to vanilla rather than no-op
    }
}
