package com.example.witherstorm.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.example.witherstorm.entity.EntityStormSkull;

public class RenderStormSkull extends Render<EntityStormSkull> {
    public RenderStormSkull(RenderManager m) {
        super(m);
    }

    @Override
    public void doRender(EntityStormSkull e, double x, double y, double z, float yaw, float pt) {
        float s = e.getSkullSize() * 0.5F;
        float t = e.ticksExisted + pt;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + e.height / 2.0F, (float) z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.rotate(t * 11.0F, 0, 1, 0);
        GlStateManager.rotate(t * 7.0F, 1, 0, 0);
        RenderUtil.cube(s * 0.6F, 0.12F, 0.02F, 0.2F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderUtil.cube(s * 0.85F, 0.5F, 0.2F, 0.9F, 0.35F);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityStormSkull e) {
        return TextureMap.locationBlocksTexture;
    }
}
