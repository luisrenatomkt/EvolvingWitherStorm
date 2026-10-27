package com.example.witherstorm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import com.example.witherstorm.CommonProxy;
import com.example.witherstorm.entity.EntityStormSkull;
import com.example.witherstorm.entity.EntityWitherStorm;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(ClientStormRenderer.INSTANCE);
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

    @Override
    public void handleGhost(final int id, final double x, final double y, final double z, final float yaw, final float scale,
            final int stage, final int flags) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStormRenderer.INSTANCE.onGhost(id, x, y, z, yaw, scale, stage, flags);
            }
        });
    }
}
