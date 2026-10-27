package com.example.witherstorm.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** Modelo procedural: torso, 3-7 cabecas, espinhos (estagio 4+), nucleo (5+) e ate 24 tentaculos segmentados. */
public class ModelWitherStorm extends ModelBase {
    public static final int MAX_TENT = 24, SEGS = 5;
    private static final float S = 0.0625F;

    private final ModelRenderer torso, core, headC, headL, headR, headXL, headXR, headFL, headFR;
    private final ModelRenderer[] spikes = new ModelRenderer[8];
    private final ModelRenderer[][] tent = new ModelRenderer[MAX_TENT][SEGS];

    private ModelRenderer part(int u, int v) {
        ModelRenderer r = new ModelRenderer(this, u, v);
        r.setTextureSize(64, 64);
        return r;
    }

    private ModelRenderer head(int size, float x, float y, float ry) {
        ModelRenderer h = part(0, 0);
        float s = size / 2f;
        h.addBox(-s, -s, -s, size, size, size);
        h.setRotationPoint(x, y, 0);
        h.rotateAngleY = ry;
        ModelRenderer eL = part(0, 56);
        eL.addBox(-s + 0.5f, -size / 6f, -s - 0.5f, 2, 1, 1);
        ModelRenderer eR = part(0, 56);
        eR.addBox(s - 2.5f, -size / 6f, -s - 0.5f, 2, 1, 1);
        h.addChild(eL);
        h.addChild(eR);
        return h;
    }

    public ModelWitherStorm() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        torso = part(0, 0);
        torso.addBox(-5, -5, -5, 10, 10, 10);
        core = part(0, 56);
        core.addBox(-2, -2, -6.5f, 4, 4, 2);
        headC = head(8, 0, -9, 0);
        headL = head(6, -9, -6, -0.3f);
        headR = head(6, 9, -6, 0.3f);
        headXL = head(6, -5, -13, -0.15f);
        headXR = head(6, 5, -13, 0.15f);
        headFL = head(6, -14, -2, -0.5f);
        headFR = head(6, 14, -2, 0.5f);
        for (int i = 0; i < 8; i++) {
            ModelRenderer sp = part(0, 0);
            sp.addBox(5, -1, -1, 11, 2, 2);
            sp.setRotationPoint(0, 0, 0);
            sp.rotateAngleY = (float) (i * Math.PI / 4.0);
            sp.rotateAngleZ = -0.45f;
            spikes[i] = sp;
        }
        int[] w = { 4, 4, 3, 3, 2 };
        for (int i = 0; i < MAX_TENT; i++) {
            ModelRenderer parent = null;
            for (int k = 0; k < SEGS; k++) {
                ModelRenderer r = part(0, 0);
                r.addBox(-w[k] / 2f, 0, -w[k] / 2f, w[k], 7, w[k]);
                r.setRotationPoint(0, k == 0 ? 5 : 7, 0);
                if (parent != null) parent.addChild(r);
                tent[i][k] = r;
                parent = r;
            }
        }
    }

    public void renderStorm(float age, int stage, int tentCount, float agit) {
        int tc = Math.max(0, Math.min(tentCount, MAX_TENT));
        for (int i = 0; i < tc; i++) {
            double th = Math.PI * 2.0 * i / tc;
            float ph = age * 0.08f * agit + i * 0.7f;
            ModelRenderer root = tent[i][0];
            root.setRotationPoint((float) Math.cos(th) * 4f, 5f, (float) Math.sin(th) * 4f);
            root.rotateAngleX = (float) Math.sin(th) * 0.55f + (float) Math.sin(ph) * 0.15f * agit;
            root.rotateAngleZ = -(float) Math.cos(th) * 0.55f + (float) Math.cos(ph * 0.9f) * 0.15f * agit;
            for (int k = 1; k < SEGS; k++) {
                tent[i][k].rotateAngleX = (float) Math.sin(ph - k * 0.6f) * 0.35f * agit;
                tent[i][k].rotateAngleZ = (float) Math.cos(ph * 0.8f - k * 0.5f) * 0.25f * agit;
            }
        }
        float sway = (float) Math.sin(age * 0.05f) * 0.08f;
        headC.rotateAngleY = sway;
        core.showModel = stage >= 5;
        for (ModelRenderer sp : spikes) sp.showModel = stage >= 4;
        headXL.showModel = headXR.showModel = stage >= 3;
        headFL.showModel = headFR.showModel = stage >= 6;

        torso.render(S);
        core.render(S);
        for (ModelRenderer sp : spikes) sp.render(S);
        headC.render(S);
        headL.render(S);
        headR.render(S);
        headXL.render(S);
        headXR.render(S);
        headFL.render(S);
        headFR.render(S);
        for (int i = 0; i < tc; i++) tent[i][0].render(S);
    }
}
