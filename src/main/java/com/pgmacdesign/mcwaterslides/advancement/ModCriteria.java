package com.pgmacdesign.mcwaterslides.advancement;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCriteria {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, MCWaterSlides.MOD_ID);

    public static final Supplier<RideStatTrigger> RIDE_STAT =
            TRIGGERS.register("ride_stat", RideStatTrigger::new);

    private ModCriteria() {}
}
