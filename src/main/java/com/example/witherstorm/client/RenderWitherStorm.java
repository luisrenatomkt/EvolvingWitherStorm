package com.example.witherstorm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.example.witherstorm.entity.EntityWitherStorm;

public class RenderWitherStorm extends Render<EntityWitherStorm> {
    private final ModelWitherStorm model = new ModelWitherStorm();

    public RenderWitherStorm(RenderManager m) {
        super(m);
        this.shadowSize = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityWitherStorm e, ICamera cam, double cx, double cy, double cz) {
        double max = 192.0D + e.getRenderScale() * 24.0D;
        return e.getDistanceSq(cx, cy, cz) < max * max;
    }

    @Override
    public void doRender(EntityWitherStorm e, double x, double y, double z, float entityYaw, float pt) {
        float rs = e.getSmoothScale(pt);
        float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
        float age = e.ticksExisted + pt;
        float agit = (e.getFlags() & EntityWitherStorm.FLAG_ATTACK) != 0 ? 1.7F : 1.0F;
        if (e.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) < 128.0 * 128.0) BossStatus.setBossStatus(e, true);

        double hs = e.height / EntityWitherStorm.BASE_H;
        float offY = (float) (e.height * 0.5D + 1.2D * hs);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + offY, (float) z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        float g = 1.6F * rs; // 1 px de modelo = 0.1 bloco
        GlStateManager.scale(-g, -g, g);
        GlStateManager.enableRescaleNormal();
        bindTexture(StormTextures.body());
        model.renderStorm(age, e.getStage(), e.getTentacleCount(), agit);

        // brilho (olhos/nucleo)
        bindTexture(StormTextures.glow());
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        model.renderStorm(age, e.getStage(), e.getTentacleCount(), agit);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        int br = e.getBrightnessForRender(pt);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, br % 65536, br / 65536);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        renderAbsorption(e, x, y + offY, z, pt, age);
    }

    /** Blocos sendo puxados por tentaculos ate o corpo. Coordenadas relativas a camera. */
    private void renderAbsorption(EntityWitherStorm e, double cx, double cy, double cz, float pt, float age) {
        if (e.flying.isEmpty()) return;
        double ex = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt;
        double ey = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt;
        double ez = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt;
        double bodyX = cx, bodyY = cy, bodyZ = cz;
        // y do pe da entidade relativo a camera
        double entRelY = cy - (e.height * 0.5D + 1.2D * (e.height / EntityWitherStorm.BASE_H));

        int drawn = 0;
        for (EntityWitherStorm.FlyingBlock fb : e.flying) {
            float t = Math.min(1.0F, (fb.age + pt) / fb.life);
            float ease = t * t * (3.0F - 2.0F * t);
            double sx = fb.sx - ex + cx, sy = fb.sy - ey + entRelY, sz = fb.sz - ez + cz;
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
            bindTexture(TextureMap.locationBlocksTexture);
            Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlockBrightness(fb.state, 1.0F);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWitherStorm e) {
        return StormTextures.body();
    }
}
