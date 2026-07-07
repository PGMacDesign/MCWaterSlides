package com.pgmacdesign.mcwaterslides.registry;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                        output.accept(ModItems.JET.get());
                        output.accept(ModItems.PUMP_HOUSE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
