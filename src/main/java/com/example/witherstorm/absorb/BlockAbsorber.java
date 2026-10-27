package com.example.witherstorm.absorb;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityWitherStorm;
import com.example.witherstorm.evolution.EvolutionFormulas;
import com.example.witherstorm.network.NetworkHandler;
import com.example.witherstorm.network.PacketAbsorb;

/**
 * Absorcao de blocos em lotes: amostragem + fila BFS (comer estruturas conectadas),
 * limite duro por tick e cache de posicoes rejeitadas. Roda apenas no servidor.
 */
public class BlockAbsorber {
    private final EntityWitherStorm storm;
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
    private final HashSet<Long> queued = new HashSet<Long>();
    private final HashSet<Long> rejected = new HashSet<Long>();
    private final Random rand = new Random();
    private BlockPos focus;
    private int scanCooldown;

    public BlockAbsorber(EntityWitherStorm storm) {
        this.storm = storm;
    }

    public BlockPos getFocus() {
        return focus;
    }

    public void clearFocus() {
        focus = null;
    }

    public boolean hasWork() {
        return !queue.isEmpty();
    }

    /** World.isValid e privado no 1.8.9: verifica altura e limites do mundo manualmente. */
    private static boolean valid(BlockPos p) {
        return p.getY() >= 0 && p.getY() < 256
                && Math.abs(p.getX()) < 30000000 && Math.abs(p.getZ()) < 30000000;
    }

    private boolean inReach(BlockPos p, Vec3 c, double reachSq) {
        double dx = p.getX() + 0.5 - c.xCoord, dy = p.getY() + 0.5 - c.yCoord, dz = p.getZ() + 0.5 - c.zCoord;
        return dx * dx + dy * dy + dz * dz <= reachSq;
    }

    private void enqueue(BlockPos p) {
        if (queue.size() >= StormConfig.queueCapacity) return;
        long k = p.toLong();
        if (queued.contains(k) || rejected.contains(k)) return;
        queue.add(p);
        queued.add(k);
    }

    private void enqueueFirst(BlockPos p) {
        long k = p.toLong();
        if (queued.contains(k) || rejected.contains(k)) return;
        if (queue.size() >= StormConfig.queueCapacity) queue.pollLast();
        queue.addFirst(p);
        queued.add(k);
    }

    /** Blocos que estao no caminho (a frente de cada eixo em movimento) ganham prioridade: o Storm abre caminho comendo. */
    private void sampleFront(World w) {
        int n = StormConfig.frontSamplesPerTick;
        if (n <= 0) return;
        AxisAlignedBB bb = storm.getEntityBoundingBox();
        double[] m = { storm.motionX, storm.motionY, storm.motionZ };
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(m[axis]) < 0.02) continue;
            double dir = m[axis] > 0 ? 1 : -1;
            for (int i = 0; i < n; i++) {
                double x = bb.minX + rand.nextDouble() * (bb.maxX - bb.minX);
                double y = bb.minY + rand.nextDouble() * (bb.maxY - bb.minY);
                double z = bb.minZ + rand.nextDouble() * (bb.maxZ - bb.minZ);
                double off = 0.3 + rand.nextDouble() * 0.9;
                if (axis == 0) x = dir > 0 ? bb.maxX + off : bb.minX - off;
                else if (axis == 1) y = dir > 0 ? bb.maxY + off : bb.minY - off;
                else z = dir > 0 ? bb.maxZ + off : bb.minZ - off;
                BlockPos p = new BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
                if (valid(p) && w.isBlockLoaded(p) && !w.isAirBlock(p)) enqueueFirst(p);
            }
        }
    }

    public void tick(boolean boost) {
        World w = storm.worldObj;
        if (!StormConfig.absorptionEnabled || storm.evo.isFull()) return;
        if (StormConfig.respectMobGriefing && !w.getGameRules().getBoolean("mobGriefing")) return;

        Vec3 c = storm.getBodyCenter();
        double reach = storm.stats.tentacleReach;
        double reachSq = reach * reach;

        if (storm.ticksExisted % 600 == 0 || rejected.size() > StormConfig.invalidCacheSize) rejected.clear();
        if (--scanCooldown <= 0) {
            scanCooldown = StormConfig.scanInterval;
            scanFocus(w, c);
        }
        if (focus != null && inReach(focus, c, reachSq)) enqueue(focus);

        sample(w, c, reach);
        sampleFront(w);

        int budget = (int) Math.min(StormConfig.maxBlocksPerTick,
                Math.ceil(storm.stats.blocksPerTick * (boost ? StormConfig.fastAbsorbMultiplier : 1.0)));
        int attempts = budget * 3, absorbed = 0, packets = 0;
        while (absorbed < budget && attempts-- > 0 && !queue.isEmpty()) {
            BlockPos p = queue.pollFirst();
            queued.remove(p.toLong());
            if (!valid(p) || !w.isBlockLoaded(p) || !inReach(p, c, reachSq)) continue;
            IBlockState s = w.getBlockState(p);
            double mass = BlockMassRegistry.massOf(w, p, s);
            if (mass <= 0) {
                rejected.add(p.toLong());
                continue;
            }
            int stateId = Block.getStateId(s);
            // remocao e contabilizacao atomicas: so conta se o bloco realmente foi removido agora
            if (w.setBlockToAir(p)) {
                storm.evo.addMass(mass);
                absorbed++;
                if (packets < StormConfig.maxVisualBlocksPerTick) {
                    packets++;
                    NetworkHandler.CHANNEL.sendToAllAround(
                            new PacketAbsorb(storm.getEntityId(), p.getX(), p.getY(), p.getZ(), stateId),
                            new NetworkRegistry.TargetPoint(w.provider.getDimensionId(), storm.posX, storm.posY, storm.posZ, 192.0D));
                }
                for (EnumFacing f : EnumFacing.values()) {
                    BlockPos n = p.offset(f);
                    if (valid(n) && w.isBlockLoaded(n) && !w.isAirBlock(n) && inReach(n, c, reachSq)) enqueue(n);
                }
            }
        }
    }

    private void sample(World w, Vec3 c, double reach) {
        int n = StormConfig.samplesPerTick;
        for (int i = 0; i < n && queue.size() < StormConfig.queueCapacity; i++) {
            BlockPos p;
            double r = rand.nextDouble() * reach;
            double theta = rand.nextDouble() * Math.PI * 2.0;
            if ((i & 1) == 0) { // amostra a superficie do terreno
                int x = MathHelper.floor_double(c.xCoord + Math.cos(theta) * r);
                int z = MathHelper.floor_double(c.zCoord + Math.sin(theta) * r);
                BlockPos probe = new BlockPos(x, 64, z);
                if (!w.isBlockLoaded(probe)) continue;
                p = w.getHeight(probe).down();
            } else { // amostra esferica
                double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
                p = new BlockPos(MathHelper.floor_double(c.xCoord + r * Math.sin(phi) * Math.cos(theta)),
                        MathHelper.floor_double(c.yCoord + r * Math.cos(phi)),
                        MathHelper.floor_double(c.zCoord + r * Math.sin(phi) * Math.sin(theta)));
            }
            if (!valid(p) || !w.isBlockLoaded(p) || w.isAirBlock(p)) continue;
            enqueue(p);
        }
        // destroi obstaculos dentro do proprio corpo
        AxisAlignedBB bb = storm.getEntityBoundingBox();
        for (int i = 0; i < 12; i++) {
            BlockPos p = new BlockPos(
                    MathHelper.floor_double(bb.minX + rand.nextDouble() * (bb.maxX - bb.minX)),
                    MathHelper.floor_double(bb.minY + rand.nextDouble() * (bb.maxY - bb.minY)),
                    MathHelper.floor_double(bb.minZ + rand.nextDouble() * (bb.maxZ - bb.minZ)));
            if (valid(p) && w.isBlockLoaded(p) && !w.isAirBlock(p)) enqueueFirst(p);
        }
    }

    /** Procura o bloco absorvivel mais proximo na superficie (busca amostrada e limitada). */
    private void scanFocus(World w, Vec3 c) {
        double radius = storm.stats.searchRadius;
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < StormConfig.scanSamples; i++) {
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * Math.sqrt(rand.nextDouble());
            int x = MathHelper.floor_double(c.xCoord + Math.cos(ang) * r);
            int z = MathHelper.floor_double(c.zCoord + Math.sin(ang) * r);
            BlockPos probe = new BlockPos(x, 64, z);
            if (!w.isBlockLoaded(probe)) continue;
            BlockPos top = w.getHeight(probe).down();
            if (top.getY() < 0 || rejected.contains(top.toLong())) continue;
            IBlockState s = w.getBlockState(top);
            if (BlockMassRegistry.massOf(w, top, s) <= 0) {
                rejected.add(top.toLong());
                continue;
            }
            double dx = top.getX() + 0.5 - c.xCoord, dy = top.getY() - c.yCoord, dz = top.getZ() + 0.5 - c.zCoord;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bd) {
                bd = d;
                best = top;
            }
        }
        focus = best;
    }
}
