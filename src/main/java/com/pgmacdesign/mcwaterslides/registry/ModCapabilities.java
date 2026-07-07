package com.pgmacdesign.mcwaterslides.registry;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {
    private ModCapabilities() {}

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        // Standard energy on every side — any RF mod's cables/generators interop, no config.
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.JET.get(),
                (be, side) -> be.energyHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PUMP_HOUSE.get(),
                (be, side) -> be.energyHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PUMP_HOUSE.get(),
                (be, side) -> be.itemHandler(side));
    }
}
