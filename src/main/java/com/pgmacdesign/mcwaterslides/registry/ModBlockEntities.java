package com.pgmacdesign.mcwaterslides.registry;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MCWaterSlides.MOD_ID);

    public static final Supplier<BlockEntityType<JetBlockEntity>> JET =
            BLOCK_ENTITIES.register("jet", () ->
                    BlockEntityType.Builder.of(JetBlockEntity::new, ModBlocks.JET.get()).build(null));

    public static final Supplier<BlockEntityType<com.pgmacdesign.mcwaterslides.machine.PumpHouseBlockEntity>> PUMP_HOUSE =
            BLOCK_ENTITIES.register("pump_house", () ->
                    BlockEntityType.Builder.of(com.pgmacdesign.mcwaterslides.machine.PumpHouseBlockEntity::new,
                            ModBlocks.PUMP_HOUSE.get()).build(null));

    private ModBlockEntities() {}
}
