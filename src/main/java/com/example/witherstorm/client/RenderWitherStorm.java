package com.example.witherstorm.client;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import com.example.witherstorm.entity.EntityWitherStorm;

/**
 * Render "vazio": o Wither Storm e desenhado por {@link ClientStormRenderer} (RenderWorldLastEvent), porque o render
 * de entidades do 1.8.9 nao desenha entidades cujo chunk central nao esta carregado no cliente.
 */
public class RenderWitherStorm extends Render<EntityWitherStorm> {
    public RenderWitherStorm(RenderManager m) {
        super(m);
        this.shadowSize = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityWitherStorm e, ICamera cam, double cx, double cy, double cz) {
        return false;
    }

    @Override
    public void doRender(EntityWitherStorm e, double x, double y, double z, float yaw, float pt) {
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWitherStorm e) {
        return StormTextures.body();
    }
}
