package com.example.witherstorm;

import net.minecraft.entity.boss.EntityWither;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityWitherStorm;
import com.example.witherstorm.evolution.KillTracker;

public class StormEvents {
    /** Wither vanilla invocado -> vira Wither Storm no mesmo local. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.world.isRemote || !StormConfig.convertVanillaWither) return;
        if (e.entity instanceof EntityWither) {
            e.setCanceled(true);
            EntityWitherStorm s = new EntityWitherStorm(e.world);
            s.setLocationAndAngles(e.entity.posX, e.entity.posY, e.entity.posZ, e.entity.rotationYaw, 0.0F);
            e.world.spawnEntityInWorld(s);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        if (e.entityLiving.worldObj.isRemote) return;
        KillTracker.handle(e.entityLiving, e.source);
    }
}
