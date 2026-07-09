package com.pgmacdesign.mcwaterslides.entity;

import com.pgmacdesign.mcwaterslides.registry.ModEntities;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Drops an {@link TubeRaftEntity} where you look — onto water (floats) OR onto a slide/funnel
 * surface (rides). Fluid.ANY so aiming at a water surface lands on it like a boat; a block hit
 * lands the tube on top of the block you clicked.
 */
public class TubeRaftItem extends Item {
    public TubeRaftItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            Vec3 loc = hit.getLocation();
            TubeRaftEntity raft = new TubeRaftEntity(ModEntities.INNER_TUBE.get(), level);
            raft.setPos(loc.x, loc.y + 0.1, loc.z);
            raft.setYRot(player.getYRot());
            level.addFreshEntity(raft);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, loc);
        }
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
