package com.example.witherstorm.client;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.example.witherstorm.entity.EntityWitherStorm;

/**
 * Renderiza todos os Storms por conta propria (RenderWorldLastEvent), ignorando as checagens do render de entidades
 * (chunk do centro carregado, frustum, distancia) e estendendo o plano distante da camera. Alem das entidades reais,
 * desenha "fantasmas" alimentados por PacketGhost quando a entidade real nao existe no cliente.
 */
public class ClientStormRenderer {
    public static final ClientStormRenderer INSTANCE = new ClientStormRenderer();

    private static final float FAR = 8000.0F;
    private static final double MAX_DIST = 6000.0D;

    private static final class Ghost {
        EntityWitherStorm dummy;
        double px, py, pz, cx, cy, cz;
        long lastUpdate;
        int interval = 5;
    }

    private final Map<Integer, Ghost> ghosts = new HashMap<Integer, Ghost>();
    private World lastWorld;

    /** Chamado na thread principal do cliente. */
    public void onGhost(int id, double x, double y, double z, float yaw, float scale, int stage, int flags) {
        Minecraft mc = Minecraft.getMinecraft();
        World w = mc.theWorld;
        if (w == null) return;
        if (w != lastWorld) {
            ghosts.clear();
            lastWorld = w;
        }
        if (w.getEntityByID(id) instanceof EntityWitherStorm) {
            ghosts.remove(id);
            return;
        }
        long now = w.getTotalWorldTime();
        Ghost g = ghosts.get(id);
        if (g == null) {
            g = new Ghost();
            g.dummy = new EntityWitherStorm(w);
            g.px = x;
            g.py = y;
            g.pz = z;
            ghosts.put(id, g);
        } else {
            g.px = g.cx;
            g.py = g.cy;
            g.pz = g.cz;
            g.interval = (int) Math.max(1, Math.min(20, now - g.lastUpdate));
        }
        g.cx = x;
        g.cy = y;
        g.cz = z;
        g.lastUpdate = now;
        g.dummy.ghostSync(scale, stage, flags, yaw);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent ev) {
        Minecraft mc = Minecraft.getMinecraft();
        World w = mc.theWorld;
        Entity view = mc.getRenderViewEntity();
        if (w == null || view == null) return;
        if (w != lastWorld) {
            ghosts.clear();
            lastWorld = w;
        }
        float pt = ev.partialTicks;
        long now = w.getTotalWorldTime();
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        List<EntityWitherStorm> real = new ArrayList<EntityWitherStorm>();
        for (Entity e : w.loadedEntityList) {
            if (e instanceof EntityWitherStorm && !e.isDead) real.add((EntityWitherStorm) e);
        }
        Iterator<Map.Entry<Integer, Ghost>> it = ghosts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Ghost> en = it.next();
            if (now - en.getValue().lastUpdate > 60 || w.getEntityByID(en.getKey()) instanceof EntityWitherStorm) it.remove();
        }
        if (real.isEmpty() && ghosts.isEmpty()) return;

        // ---- estado GL: plano distante estendido, sem neblina, brilho total
        boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        buf.rewind();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf);
        float m10 = buf.get(10), m14 = buf.get(14);
        float near = m14 / (m10 - 1.0F);
        if (near > 0.0F && near < 5.0F) {
            buf.put(10, -(FAR + near) / (FAR - near));
            buf.put(14, -2.0F * FAR * near / (FAR - near));
            buf.rewind();
            GL11.glLoadMatrix(buf);
        }
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GlStateManager.disableFog();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        try {
            for (EntityWitherStorm e : real) {
                double ex = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt;
                double ey = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt;
                double ez = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt;
                double dx = ex - vx, dy = ey - vy, dz = ez - vz;
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) continue;
                if (dx * dx + dy * dy + dz * dz < 256.0 * 256.0) BossStatus.setBossStatus(e, true);
                StormRenderer.draw(e, dx, dy, dz, pt, e.ticksExisted + pt, ex, ey, ez, true);
            }
            for (Ghost g : ghosts.values()) {
                double el = Math.min(g.interval * 1.5, (now - g.lastUpdate) + pt);
                double k = el / g.interval;
                double ex = g.cx + (g.cx - g.px) * k, ey = g.cy + (g.cy - g.py) * k, ez = g.cz + (g.cz - g.pz) * k;
                double dx = ex - vx, dy = ey - vy, dz = ez - vz;
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) continue;
                g.dummy.setPosition(ex, ey, ez);
                StormRenderer.draw(g.dummy, dx, dy, dz, pt, now + pt, ex, ey, ez, false);
            }
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (fog) GlStateManager.enableFog();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }
}
