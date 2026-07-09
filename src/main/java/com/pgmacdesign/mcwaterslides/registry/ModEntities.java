package com.pgmacdesign.mcwaterslides.registry;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.entity.TubeRaftEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MCWaterSlides.MOD_ID);

    /** The inner tube: a boat-like single-seat raft that flows through slides, water, and funnels. */
    public static final Supplier<EntityType<TubeRaftEntity>> INNER_TUBE =
            ENTITY_TYPES.register("inner_tube", () ->
                    EntityType.Builder.<TubeRaftEntity>of(TubeRaftEntity::new, MobCategory.MISC)
                            .sized(1.0f, 0.4f)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                            .build("inner_tube"));

    private ModEntities() {}
}
