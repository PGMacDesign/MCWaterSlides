package com.pgmacdesign.mcwaterslides.registry;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCWaterSlides.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mcwaterslides"))
                    .icon(() -> new ItemStack(ModItems.SLIDE_CHANNEL_ITEMS.get(null).get()))
                    .displayItems((parameters, output) -> {
                        ModItems.SLIDE_CHANNEL_ITEMS.values().forEach(output::accept);
                        ModItems.SLIDE_TUBE_ITEMS.values().forEach(output::accept);
                        output.accept(ModItems.JET.get());
                        output.accept(ModItems.PUMP_HOUSE.get());
                        output.accept(ModItems.WATER_CONDUIT.get());
                        output.accept(ModItems.FLOOD_VALVE.get());
                        output.accept(ModItems.SPLASH_POOL.get());
                        addGuideBook(output);
                    })
                    .build());

    /**
     * Show the Patchouli guide right in our tab when Patchouli is present — its own
     * {@code creative_tab} auto-insert into a modded tab is unreliable, and a book the
     * player can't find reads as "missing". Soft-dep: pure registry lookups + string
     * ids, no gradle dependency; a no-op when Patchouli is absent.
     */
    @SuppressWarnings("unchecked")
    private static void addGuideBook(CreativeModeTab.Output output) {
        Item book = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("patchouli:guide_book")).orElse(null);
        DataComponentType<ResourceLocation> bookComponent =
                (DataComponentType<ResourceLocation>) BuiltInRegistries.DATA_COMPONENT_TYPE
                        .get(ResourceLocation.parse("patchouli:book"));
        if (book == null || bookComponent == null) {
            return;
        }
        ItemStack stack = new ItemStack(book);
        stack.set(bookComponent, ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, "guide"));
        output.accept(stack);
    }

    private ModCreativeTabs() {}
}
