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
    /** Keyed like the channels — null = natural. */
    public static final Map<DyeColor, DeferredBlock<com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock>> SLIDE_TUBES = new LinkedHashMap<>();

    static {
        SLIDE_CHANNELS.put(null, registerChannel("slide_channel", null));
        SLIDE_TUBES.put(null, registerTube("slide_tube", null));
        for (DyeColor color : DyeColor.values()) {
            SLIDE_CHANNELS.put(color, registerChannel(color.getName() + "_slide_channel", color));
            SLIDE_TUBES.put(color, registerTube(color.getName() + "_slide_tube", color));
        }
    }

    private static DeferredBlock<com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock> registerTube(String name, DyeColor color) {
        return BLOCKS.registerBlock(name,
                props -> new com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock(color, props),
                BlockBehaviour.Properties.of()
                        .strength(1.5f, 6.0f)
                        .sound(SoundType.STONE)
                        .noOcclusion());
    }

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.machine.JetBlock> JET =
            BLOCKS.registerBlock("jet",
                    com.pgmacdesign.mcwaterslides.machine.JetBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.5f, 6.0f)
                            .sound(SoundType.COPPER));

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.machine.PumpHouseBlock> PUMP_HOUSE =
            BLOCKS.registerBlock("pump_house",
                    com.pgmacdesign.mcwaterslides.machine.PumpHouseBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0f, 6.0f)
                            .sound(SoundType.COPPER)
                            .lightLevel(state -> state.getValue(com.pgmacdesign.mcwaterslides.machine.PumpHouseBlock.LIT) ? 10 : 0));

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.machine.WaterConduitBlock> WATER_CONDUIT =
            BLOCKS.registerBlock("water_conduit",
                    com.pgmacdesign.mcwaterslides.machine.WaterConduitBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.0f, 4.0f)
                            .sound(SoundType.COPPER)
                            .noOcclusion());

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.machine.FloodValveBlock> FLOOD_VALVE =
            BLOCKS.registerBlock("flood_valve",
                    com.pgmacdesign.mcwaterslides.machine.FloodValveBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.5f, 6.0f)
                            .sound(SoundType.COPPER));

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.funnel.FunnelCoreBlock> FUNNEL_CORE =
            BLOCKS.registerBlock("funnel_core",
                    com.pgmacdesign.mcwaterslides.funnel.FunnelCoreBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(2.5f, 6.0f)
                            .sound(SoundType.COPPER)
                            .noOcclusion());

    // The tornado shell blocks are machine-stamped by the core only — no items, no drops.
    public static final DeferredBlock<net.minecraft.world.level.block.Block> FUNNEL_WALL =
            BLOCKS.registerBlock("funnel_wall",
                    net.minecraft.world.level.block.Block::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.COPPER)
                            .noOcclusion()
                            .noLootTable());

    public static final DeferredBlock<net.minecraft.world.level.block.Block> FUNNEL_WALL_ACCENT =
            BLOCKS.registerBlock("funnel_wall_accent",
                    net.minecraft.world.level.block.Block::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.COPPER)
                            .noOcclusion()
                            .noLootTable());

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock> SPLASH_POOL =
            BLOCKS.registerBlock("splash_pool",
                    com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5f, 6.0f)
                            .sound(SoundType.STONE)
                            .noOcclusion());

    /** Translucent glass variants (see-through all around) — standalone material, not a dye color. */
    public static final DeferredBlock<SlideChannelBlock> CLEAR_SLIDE_CHANNEL =
            BLOCKS.registerBlock("clear_slide_channel",
                    props -> new SlideChannelBlock(null, props),
                    BlockBehaviour.Properties.of()
                            .strength(1.0f, 4.0f)
                            .sound(SoundType.GLASS)
                            .noOcclusion());

    public static final DeferredBlock<com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock> CLEAR_SLIDE_TUBE =
            BLOCKS.registerBlock("clear_slide_tube",
                    props -> new com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock(null, props),
                    BlockBehaviour.Properties.of()
                            .strength(1.0f, 4.0f)
                            .sound(SoundType.GLASS)
                            .noOcclusion());

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
