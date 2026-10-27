package com.example.witherstorm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/** Texturas geradas por codigo (nao ha arquivos .png a distribuir). Linhas 56-63 = regiao brilhante (olhos/nucleo). */
public final class StormTextures {
    private StormTextures() {}

    private static ResourceLocation body, glow;

    public static ResourceLocation body() {
        if (body == null) init();
        return body;
    }

    public static ResourceLocation glow() {
        if (glow == null) init();
        return glow;
    }

    private static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    private static void init() {
        DynamicTexture b = new DynamicTexture(64, 64);
        DynamicTexture g = new DynamicTexture(64, 64);
        int[] bd = b.getTextureData(), gd = g.getTextureData();
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int n = hash(x, y);
                int i = y * 64 + x;
                if (y >= 56) {
                    int r = 225 + n % 30, gr = 160 + (n >> 4) % 50, bl = 250;
                    bd[i] = 0xFF000000 | (r << 16) | (gr << 8) | bl;
                    gd[i] = bd[i];
                } else {
                    int r = 20 + n % 14, gr = 14 + (n >> 3) % 10, bl = 28 + (n >> 6) % 18;
                    if ((n >> 9) % 100 < 7) {
                        r = 70 + n % 40;
                        gr = 20;
                        bl = 110 + (n >> 5) % 40;
                    }
                    bd[i] = 0xFF000000 | (r << 16) | (gr << 8) | bl;
                    gd[i] = 0x00000000;
                }
            }
        }
        b.updateDynamicTexture();
        g.updateDynamicTexture();
        body = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("witherstorm_body", b);
        glow = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("witherstorm_glow", g);
    }
}
