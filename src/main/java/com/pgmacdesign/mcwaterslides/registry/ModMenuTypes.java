package com.pgmacdesign.mcwaterslides.registry;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.PumpHouseMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, MCWaterSlides.MOD_ID);

    public static final Supplier<MenuType<PumpHouseMenu>> PUMP_HOUSE =
            MENU_TYPES.register("pump_house", () -> IMenuTypeExtension.create(PumpHouseMenu::new));

    private ModMenuTypes() {}
}
