package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.registry.ModItems;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tint layers: index 0 = the water surface (biome water color, like vanilla fluids);
 * index 1 = the channel lining (dye color; pale ceramic on the undyed block). One
 * grayscale model/texture set serves all 17 colors.
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientColorHandlers {
    /** Vanilla default water tint, used where no level context exists (inventory). */
    public static final int DEFAULT_WATER = 0xFF3F76E4;
    /** Undyed lining: pale ceramic. */
    public static final int NATURAL_LINING = 0xFFE8E4DC;
    /** Clear glass: no dye tint, so the translucent texture shows its own colour. */
    public static final int CLEAR_GLASS = 0xFFFFFFFF;

    private ClientColorHandlers() {}

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        ModBlocks.SLIDE_CHANNELS.values().forEach(holder ->
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex == 0) {
                        return level != null && pos != null
                                ? 0xFF000000 | BiomeColors.getAverageWaterColor(level, pos)
                                : DEFAULT_WATER;
                    }
                    return liningColor(((SlideChannelBlock) state.getBlock()).color());
                }, holder.get()));
        ModBlocks.SLIDE_TUBES.values().forEach(holder ->
                event.register((state, level, pos, tintIndex) -> {
                    if (tintIndex == 0) {
                        return level != null && pos != null
                                ? 0xFF000000 | BiomeColors.getAverageWaterColor(level, pos)
                                : DEFAULT_WATER;
                    }
                    return liningColor(((SlideTubeBlock) state.getBlock()).color());
                }, holder.get()));
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) {
                return level != null && pos != null
                        ? 0xFF000000 | BiomeColors.getAverageWaterColor(level, pos)
                        : DEFAULT_WATER;
            }
            return NATURAL_LINING;
        }, ModBlocks.SPLASH_POOL.get());
        for (var clear : java.util.List.of(ModBlocks.CLEAR_SLIDE_CHANNEL.get(), ModBlocks.CLEAR_SLIDE_TUBE.get())) {
            event.register((state, level, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    return level != null && pos != null
                            ? 0xFF000000 | BiomeColors.getAverageWaterColor(level, pos)
                            : DEFAULT_WATER;
                }
                return CLEAR_GLASS;
            }, clear);
        }
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        ModItems.SLIDE_CHANNEL_ITEMS.values().forEach(holder ->
                event.register((stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return DEFAULT_WATER;
                    }
                    var block = ((BlockItem) stack.getItem()).getBlock();
                    return liningColor(((SlideChannelBlock) block).color());
                }, holder.get()));
        ModItems.SLIDE_TUBE_ITEMS.values().forEach(holder ->
                event.register((stack, tintIndex) -> {
                    if (tintIndex == 0) {
                        return DEFAULT_WATER;
                    }
                    var block = ((BlockItem) stack.getItem()).getBlock();
                    return liningColor(((SlideTubeBlock) block).color());
                }, holder.get()));
        event.register((stack, tintIndex) -> tintIndex == 0 ? DEFAULT_WATER : NATURAL_LINING,
                ModItems.SPLASH_POOL.get());
        for (var clear : java.util.List.of(ModItems.CLEAR_SLIDE_CHANNEL.get(), ModItems.CLEAR_SLIDE_TUBE.get())) {
            event.register((stack, tintIndex) -> tintIndex == 0 ? DEFAULT_WATER : CLEAR_GLASS, clear);
        }
    }

    private static int liningColor(DyeColor color) {
        return color == null ? NATURAL_LINING : 0xFF000000 | color.getTextureDiffuseColor();
    }
}
