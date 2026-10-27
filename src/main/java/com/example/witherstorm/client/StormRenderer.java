package com.example.witherstorm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;

import org.lwjgl.opengl.GL11;

import com.example.witherstorm.entity.EntityWitherStorm;

/** Desenha o Storm (real ou "fantasma"). Chamado dentro de RenderWorldLastEvent; x,y,z = posicao dos pes relativa a camera. */
public final class StormRenderer {
    private StormRenderer() {}

    private static ModelWitherStorm model;

    public static void draw(EntityWitherStorm e, double x, double y, double z, float pt, float age,
            double ex, double ey, double ez, boolean real) {
        if (model == null) model = new ModelWitherStorm();
        Minecraft mc = Minecraft.getMinecraft();
        float rs = e.getSmoothScale(pt);
        float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
        float agit = (e.getFlags() & EntityWitherStorm.FLAG_ATTACK) != 0 ? 1.7F : 1.0F;
        double hs = e.height / EntityWitherStorm.BASE_H;
        float offY = (float) (e.height * 0.5D + 1.2D * hs);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + offY, (float) z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        float g = 1.6F * rs; // 1 px de modelo = 0.1 bloco
        GlStateManager.scale(-g, -g, g);
        GlStateManager.enableRescaleNormal();
        mc.getTextureManager().bindTexture(StormTextures.body());
        model.renderStorm(age, e.getStage(), e.getTentacleCount(), agit);

        // brilho (olhos/nucleo)
        mc.getTextureManager().bindTexture(StormTextures.glow());
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        model.renderStorm(age, e.getStage(), e.getTentacleCount(), agit);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        if (real) renderAbsorption(e, x, y, z, offY, pt, age, ex, ey, ez);
    }

    /** Blocos sendo puxados por tentaculos ate o corpo. */
    private static void renderAbsorption(EntityWitherStorm e, double x, double y, double z, float offY, float pt, float age,
            double ex, double ey, double ez) {
        if (e.flying.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        double bodyX = x, bodyY = y + offY, bodyZ = z;
        int drawn = 0;
        for (EntityWitherStorm.FlyingBlock fb : e.flying) {
            float t = Math.min(1.0F, (fb.age + pt) / fb.life);
            float ease = t * t * (3.0F - 2.0F * t);
            double sx = fb.sx - ex + x, sy = fb.sy - ey + y, sz = fb.sz - ez + z;
            double px = sx + (bodyX - sx) * ease, py = sy + (bodyY - sy) * ease, pz = sz + (bodyZ - sz) * ease;

            if (drawn < 12) {
                drawn++;
                GlStateManager.pushMatrix();
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                RenderUtil.beam(bodyX, bodyY, bodyZ, px, py, pz, 0.25F + 0.04F * Math.min(10.0F, e.getSmoothScale(pt)), age, 0.14F, 0.04F, 0.2F);
                GlStateManager.enableCull();
                GlStateManager.enableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.popMatrix();
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) px, (float) py, (float) pz);
            float sc = 1.0F - 0.6F * ease;
            GlStateManager.scale(sc, sc, sc);
            GlStateManager.rotate((fb.age + pt) * 25.0F, 0, 1, 0);
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            mc.getBlockRendererDispatcher().renderBlockBrightness(fb.state, 1.0F);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
