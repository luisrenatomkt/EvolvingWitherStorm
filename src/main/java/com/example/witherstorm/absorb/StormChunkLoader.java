package com.example.witherstorm.absorb;

import java.util.ArrayList;

import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;

import com.example.witherstorm.WitherStormMod;
import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityWitherStorm;

/** Mantem os chunks ao redor do Storm carregados no servidor (ticket de entidade do Forge). */
public class StormChunkLoader {
    private ForgeChunkManager.Ticket ticket;
    private int lastCX = Integer.MIN_VALUE, lastCZ = Integer.MIN_VALUE;

    public void update(EntityWitherStorm e) {
        if (!StormConfig.keepChunksLoaded) return;
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(WitherStormMod.instance, e.worldObj, ForgeChunkManager.Type.ENTITY);
            if (ticket == null) return; // limite de tickets atingido
            ticket.bindEntity(e);
            ticket.setChunkListDepth(Math.min(25, ticket.getMaxChunkListDepth()));
            lastCX = lastCZ = Integer.MIN_VALUE;
        }
        int cx = MathHelper.floor_double(e.posX) >> 4;
        int cz = MathHelper.floor_double(e.posZ) >> 4;
        if (cx == lastCX && cz == lastCZ) return;
        int r = StormConfig.keptChunkRadius;
        for (ChunkCoordIntPair p : new ArrayList<ChunkCoordIntPair>(ticket.getChunkList())) {
            if (Math.abs(p.chunkXPos - cx) > r || Math.abs(p.chunkZPos - cz) > r) ForgeChunkManager.unforceChunk(ticket, p);
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(cx + dx, cz + dz));
        }
        lastCX = cx;
        lastCZ = cz;
    }

    public void release() {
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
            ticket = null;
        }
    }
}
