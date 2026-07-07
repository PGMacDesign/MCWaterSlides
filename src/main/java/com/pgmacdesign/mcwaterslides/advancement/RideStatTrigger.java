package com.pgmacdesign.mcwaterslides.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * One trigger for every ride statistic: {@code stat} names the metric
 * ("distance", "enclosed_distance", "speed", "jet_energized"), {@code min} the
 * threshold. Fired from the server ride tick and the jet energize edge.
 */
public class RideStatTrigger extends SimpleCriterionTrigger<RideStatTrigger.TriggerInstance> {

    public void trigger(ServerPlayer player, String stat, double value) {
        trigger(player, instance -> instance.stat().equals(stat) && value >= instance.min());
    }

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String stat, double min)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("stat").forGetter(TriggerInstance::stat),
                Codec.DOUBLE.optionalFieldOf("min", 0.0).forGetter(TriggerInstance::min)
        ).apply(instance, TriggerInstance::new));
    }
}
