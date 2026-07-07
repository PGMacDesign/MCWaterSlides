package com.pgmacdesign.mcwaterslides.registry;

import java.util.LinkedHashMap;
import java.util.Map;


import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MCWaterSlides.MOD_ID);

    /** Natural (undyed, null key) channel first, then the 16 dye colors — iteration order is tab order. */
    public static final Map<DyeColor, DeferredBlock<SlideChannelBlock>> SLIDE_CHANNELS = new LinkedHashMap<>();

    static {
        SLIDE_CHANNELS.put(null, registerChannel("slide_channel", null));
        for (DyeColor color : DyeColor.values()) {
            SLIDE_CHANNELS.put(color, registerChannel(color.getName() + "_slide_channel", color));
        }
    }

    private static DeferredBlock<SlideChannelBlock> registerChannel(String name, DyeColor color) {
        return BLOCKS.registerBlock(name,
                props -> new SlideChannelBlock(color, props),
                BlockBehaviour.Properties.of()
                        .strength(1.5f, 6.0f)
                        .sound(SoundType.STONE)
                        .noOcclusion());
    }

    private ModBlocks() {}
}
