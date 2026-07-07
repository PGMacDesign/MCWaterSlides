package com.pgmacdesign.mcwaterslides;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.registry.ModCreativeTabs;
import com.pgmacdesign.mcwaterslides.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MCWaterSlides.MOD_ID)
public class MCWaterSlides {
    public static final String MOD_ID = "mcwaterslides";

    // NeoForge injects the mod event bus + container into the @Mod constructor.
    public MCWaterSlides(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, MCWaterslidesConfig.SPEC);
    }
}
