package com.example.witherstorm.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

/** Desenho simples sem textura (cubos e tentaculos em fita). */
public final class RenderUtil {
    private RenderUtil() {}

    private static void q(WorldRenderer w, float r, float g, float b, float a, float... v) {
        for (int i = 0; i < 12; i += 3) w.pos(v[i], v[i + 1], v[i + 2]).color(r, g, b, a).endVertex();
    }

    public static void cube(float h, float r, float g, float b, float a) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        q(w, r, g, b, a, -h, -h, -h, h, -h, -h, h, h, -h, -h, h, -h);
        q(w, r, g, b, a, -h, -h, h, -h, h, h, h, h, h, h, -h, h);
        q(w, r, g, b, a, -h, -h, -h, -h, h, -h, -h, h, h, -h, -h, h);
        q(w, r, g, b, a, h, -h, -h, h, -h, h, h, h, h, h, h, -h);
        q(w, r, g, b, a, -h, -h, -h, -h, -h, h, h, -h, h, h, -h, -h);
        q(w, r, g, b, a, -h, h, -h, h, h, -h, h, h, h, -h, h, h);
        t.draw();
    }

    /** Tentaculo em fita entre dois pontos (coordenadas relativas a camera). */
    public static void beam(double ax, double ay, double az, double bx, double by, double bz, float width, float time,
            float r, float g, float bl) {
        int n = 12;
        double len = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double px = ax, py = ay, pz = az, pw = width;
        for (int i = 1; i <= n; i++) {
            double f = i / (double) n;
            double sway = Math.sin(f * Math.PI) * len * 0.05;
            double nx = ax + (bx - ax) * f + Math.sin(time * 0.3 + f * 6) * sway;
            double ny = ay + (by - ay) * f + Math.cos(time * 0.25 + f * 5) * sway;
            double nz = az + (bz - az) * f + Math.sin(time * 0.2 + f * 4) * sway;
            double nw = width * (1.0 - 0.7 * f);
            w.pos(px - pw, py, pz).color(r, g, bl, 1f).endVertex();
            w.pos(px + pw, py, pz).color(r, g, bl, 1f).endVertex();
            w.pos(nx + nw, ny, nz).color(r, g, bl, 1f).endVertex();
            w.pos(nx - nw, ny, nz).color(r, g, bl, 1f).endVertex();
            w.pos(px, py - pw, pz).color(r, g, bl, 1f).endVertex();
            w.pos(px, py + pw, pz).color(r, g, bl, 1f).endVertex();
            w.pos(nx, ny + nw, nz).color(r, g, bl, 1f).endVertex();
            w.pos(nx, ny - nw, nz).color(r, g, bl, 1f).endVertex();
            px = nx;
            py = ny;
            pz = nz;
            pw = nw;
        }
        t.draw();
    }
}
