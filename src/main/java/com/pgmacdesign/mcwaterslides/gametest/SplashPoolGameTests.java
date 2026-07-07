package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class SplashPoolGameTests {

    /** A jet-driven rider is caught by the pool: stops inside it, unharmed. */
    @GameTest(template = "empty5", timeoutTicks = 300)
    public static void splashPoolStopsRider(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.SPLASH_POOL.get());
        helper.setBlock(new BlockPos(2, 1, 4), ModBlocks.SPLASH_POOL.get());
        helper.setBlock(new BlockPos(2, 1, 0),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 0)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();

        Husk zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 1)); // husks don't burn at noon
        float startHealth = zombie.getHealth();
        helper.runAfterDelay(120, () -> {
            double relZ = zombie.position().z - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).z;
            double speed = zombie.getDeltaMovement().horizontalDistance();
            if (relZ < 3.0 || relZ > 5.0) {
                helper.fail("zombie not caught in the pool: z=" + relZ);
            } else if (speed > 0.05) {
                helper.fail("zombie still moving in the pool: " + speed);
            } else if (zombie.getHealth() < startHealth) {
                helper.fail("zombie took damage in the pool");
            } else {
                helper.succeed();
            }
        });
    }

    /** Falling onto a pool negates all landing damage — ridden or not. */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void splashPoolNegatesFallDamage(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SPLASH_POOL.get());
        // Highest spawn the template allows (~3.5-block fall — normally ~0.5 hearts).
        Husk zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 4, 2)); // husks don't burn at noon
        zombie.setHealth(zombie.getMaxHealth());
        helper.runAfterDelay(60, () -> {
            if (!zombie.isAlive()) {
                helper.fail("zombie died landing in the pool");
            } else if (zombie.getHealth() < zombie.getMaxHealth()) {
                helper.fail("zombie took fall damage: " + zombie.getHealth() + "/" + zombie.getMaxHealth());
            } else {
                helper.succeed();
            }
        });
    }
}
