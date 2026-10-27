package com.example.witherstorm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import com.example.witherstorm.CommonProxy;
import com.example.witherstorm.entity.EntityStormSkull;
import com.example.witherstorm.entity.EntityWitherStorm;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityWitherStorm.class, new IRenderFactory<EntityWitherStorm>() {
            @Override
            public Render<? super EntityWitherStorm> createRenderFor(RenderManager manager) {
                return new RenderWitherStorm(manager);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityStormSkull.class, new IRenderFactory<EntityStormSkull>() {
            @Override
            public Render<? super EntityStormSkull> createRenderFor(RenderManager manager) {
                return new RenderStormSkull(manager);
            }
        });
    }

    @Override
    public void handleAbsorb(final int entityId, final int x, final int y, final int z, final int stateId) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.theWorld == null) return;
                Entity e = mc.theWorld.getEntityByID(entityId);
                if (e instanceof EntityWitherStorm) ((EntityWitherStorm) e).addFlyingBlock(x, y, z, stateId);
            }
        });
    }
}
