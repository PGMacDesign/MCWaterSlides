package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.current.CurrentFields;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.registry.ModSounds;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import com.pgmacdesign.mcwaterslides.slide.SlideSurface;
import com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/**
 * The feel layer, entirely client-side (highest multi-version churn — keep it seamed
 * here): the water-rush ride loop (one per rider, self-fading), start-swish and
 * splash-down transitions, rider wake/spray, jet-current visualization, and the subtle
 * speed-FOV kick. Numbers here are feel-pass tuning knobs, not physics.
 *
 * Local player FX read the predicted {@link RideState}; other riders (remote players,
 * mobs) are detected observationally — feet on a slide surface + real per-tick movement —
 * so every rider sounds and sprays the same on every client.
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, value = Dist.CLIENT)
public final class ClientFxHandler {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int REMOTE_RADIUS = 32;
    private static final int MAX_LOOPS = 10;

    /** Live loop per rider entity id — the exactly-one-loop invariant. */
    private static final Int2ObjectOpenHashMap<RideLoopSound> LOOPS = new Int2ObjectOpenHashMap<>();
    private static boolean wasRiding;
    private static double lastMomentum;
    private static float fovBoost;

    private ClientFxHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.level() instanceof ClientLevel level)) {
            LOOPS.clear();
            wasRiding = false;
            fovBoost = 0;
            return;
        }
        if (mc.isPaused()) {
            return;
        }

        LOOPS.values().removeIf(RideLoopSound::isStopped);

        // Jet current visualization: a legible stream of drift particles near the player.
        CurrentFields.forEachEnergizedNear(level, player.position(), 32, (jetPos, flow, field) -> {
            // Nozzle churn.
            if (RANDOM.nextInt(3) == 0) {
                Vec3 mouth = Vec3.atCenterOf(jetPos).add(Vec3.atLowerCornerOf(flow.getNormal()).scale(0.6));
                level.addParticle(ParticleTypes.BUBBLE_POP,
                        mouth.x + jitter(0.25), mouth.y + jitter(0.25), mouth.z + jitter(0.25),
                        flow.getStepX() * 0.3, flow.getStepY() * 0.3, flow.getStepZ() * 0.3);
            }
            // Flow streaks sampled along the field (dolphin-grace reads as rushing water).
            int samples = Math.min(3, field.size());
            for (int i = 0; i < samples; i++) {
                var cell = field.randomCell(RANDOM);
                level.addParticle(ParticleTypes.DOLPHIN,
                        cell.getX() + 0.3 + jitter(0.4), cell.getY() + 0.55 + jitter(0.2), cell.getZ() + 0.3 + jitter(0.4),
                        flow.getStepX() * 0.4, flow.getStepY() * 0.4, flow.getStepZ() * 0.4);
            }
        });

        tickLocalRider(mc, level, player);
        tickRemoteRiders(mc, level, player);
    }

    /** Local player: predicted ride state drives the loop, the transitions, and the wake. */
    private static void tickLocalRider(Minecraft mc, ClientLevel level, LocalPlayer player) {
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());

        boolean audio = MCWaterslidesConfig.FX_RIDE_AUDIO.get();
        if (audio) {
            if (state.riding && !wasRiding) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ModSounds.SWISH.get(), SoundSource.PLAYERS, 0.6f, 1.0f, false);
            }
            if (!state.riding && wasRiding && lastMomentum > 6.0 && inWaterOrPool(level, player)) {
                splashDown(level, player, lastMomentum);
            }
            ensureLoop(mc, player, () -> localRiding(mc), () -> localMomentum(mc));
        }

        if (state.riding && state.momentum > 2.0 && MCWaterslidesConfig.FX_SPRAY.get()) {
            spray(level, player, state.momentum);
        }

        float fovTarget = state.riding && MCWaterslidesConfig.FX_SPEED_FOV.get()
                ? (float) Mth.clamp(state.momentum / 22.0, 0.0, 1.0) : 0.0f;
        fovBoost = Mth.lerp(0.1f, fovBoost, fovTarget);

        wasRiding = state.riding;
        lastMomentum = state.momentum;
    }

    /** Other riders — remote players and mobs — detected from what this client can see. */
    private static void tickRemoteRiders(Minecraft mc, ClientLevel level, LocalPlayer player) {
        boolean audio = MCWaterslidesConfig.FX_RIDE_AUDIO.get();
        boolean sprayOn = MCWaterslidesConfig.FX_SPRAY.get();
        if (!audio && !sprayOn) {
            return;
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(REMOTE_RADIUS),
                e -> e != player && observedRiding(e))) {
            double speed = observedSpeed(entity);
            if (sprayOn) {
                spray(level, entity, speed);
            }
            if (audio && LOOPS.size() < MAX_LOOPS) {
                ensureLoop(mc, entity,
                        () -> !entity.isRemoved() && observedRiding(entity),
                        () -> observedSpeed(entity));
            }
        }
    }

    /** Exactly one live loop per rider: reuse the running instance or start a fresh one. */
    private static void ensureLoop(Minecraft mc, Entity rider,
                                   java.util.function.BooleanSupplier active,
                                   java.util.function.DoubleSupplier speed) {
        RideLoopSound existing = LOOPS.get(rider.getId());
        if (existing != null && !existing.isStopped()) {
            return;
        }
        if (!active.getAsBoolean()) {
            return;
        }
        RideLoopSound loop = new RideLoopSound(rider, active, speed);
        LOOPS.put(rider.getId(), loop);
        mc.getSoundManager().play(loop);
    }

    private static void splashDown(ClientLevel level, Entity entity, double speedBps) {
        float loud = (float) Mth.clamp(speedBps / 22.0, 0.25, 1.0);
        level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.SPLASH.get(), SoundSource.PLAYERS, 0.5f + loud * 0.5f, 1.0f, false);
        int burst = 12 + (int) (loud * 20);
        for (int i = 0; i < burst; i++) {
            level.addParticle(ParticleTypes.SPLASH,
                    entity.getX() + jitter(0.9), entity.getY() + 0.2, entity.getZ() + jitter(0.9),
                    jitter(0.2), 0.25 + RANDOM.nextDouble() * 0.3, jitter(0.2));
        }
        for (int i = 0; i < burst / 2; i++) {
            level.addParticle(ParticleTypes.BUBBLE_POP,
                    entity.getX() + jitter(0.7), entity.getY() + 0.1, entity.getZ() + jitter(0.7),
                    0, 0.1, 0);
        }
    }

    private static void spray(ClientLevel level, Entity entity, double speedBps) {
        double intensity = speedBps / 22.0;
        int count = 1 + (int) (intensity * 3);
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.SPLASH,
                    entity.getX() + jitter(0.4), entity.getY() + 0.15, entity.getZ() + jitter(0.4),
                    -entity.getDeltaMovement().x * 0.5, 0.1 + intensity * 0.15,
                    -entity.getDeltaMovement().z * 0.5);
        }
    }

    /** The speed-FOV kick: a smoothed, capped widening near the speed cap. Config-gated. */
    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (fovBoost > 0.01f) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0f + 0.07f * fovBoost));
        }
    }

    private static boolean localRiding(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && p.getData(ModAttachments.RIDE_STATE.get()).riding;
    }

    private static double localMomentum(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p == null ? 0 : p.getData(ModAttachments.RIDE_STATE.get()).momentum;
    }

    /** Observational rider test for entities whose ride state this client can't see. */
    private static boolean observedRiding(LivingEntity entity) {
        if (observedSpeed(entity) < 3.0) {
            return false;
        }
        BlockPos feet = BlockPos.containing(entity.position());
        return entity.level().getBlockState(feet).getBlock() instanceof SlideSurface
                || entity.level().getBlockState(feet.below()).getBlock() instanceof SlideSurface;
    }

    /** Per-tick position delta → blocks/sec (reliable client-side for remote entities). */
    private static double observedSpeed(LivingEntity entity) {
        double dx = entity.getX() - entity.xOld;
        double dz = entity.getZ() - entity.zOld;
        return Math.sqrt(dx * dx + dz * dz) * 20.0;
    }

    private static boolean inWaterOrPool(ClientLevel level, Entity entity) {
        BlockPos feet = BlockPos.containing(entity.position());
        return entity.isInWater()
                || level.getBlockState(feet).getBlock() instanceof SplashPoolBlock
                || level.getBlockState(feet.below()).getBlock() instanceof SplashPoolBlock;
    }

    private static double jitter(double range) {
        return (RANDOM.nextDouble() - 0.5) * 2 * range;
    }
}
