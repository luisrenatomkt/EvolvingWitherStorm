package com.example.witherstorm.ability;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityWitherStorm;
import com.example.witherstorm.evolution.EvolutionFormulas;

/** Habilidades desbloqueadas pelo poder evolutivo, com recargas e limites por tick. Servidor apenas. */
public class AbilityManager {
    public enum Type {
        BASIC, BARRAGE, BOMBARD, TENTACLE, FAST_ABSORB, PULSE, LONG_RANGE;

        public double unlock() {
            switch (this) {
                case BARRAGE: return StormConfig.unlockBarrage;
                case BOMBARD: return StormConfig.unlockBombard;
                case TENTACLE: return StormConfig.unlockTentacle;
                case FAST_ABSORB: return StormConfig.unlockFastAbsorb;
                case PULSE: return StormConfig.unlockPulse;
                case LONG_RANGE: return StormConfig.unlockLongRange;
                default: return 0.0;
            }
        }

        public int cooldown() {
            switch (this) {
                case BARRAGE: return StormConfig.cdBarrage;
                case BOMBARD: return StormConfig.cdBombard;
                case TENTACLE: return StormConfig.cdTentacle;
                case FAST_ABSORB: return StormConfig.cdFastAbsorb;
                case PULSE: return StormConfig.cdPulse;
                case LONG_RANGE: return StormConfig.cdLongRange;
                default: return StormConfig.cdBasic;
            }
        }
    }

    private final EntityWitherStorm s;
    private final Random rand = new Random();
    private final int[] cd = new int[Type.values().length];
    private int fastTicks, pulseVisual;
    private int barrageLeft, bombLeft, blastsLeft, queueTimeout;
    private EntityLivingBase queuedTarget;
    private double bx, by, bz, pulseR;

    public AbilityManager(EntityWitherStorm s) {
        this.s = s;
    }

    public boolean isUnlocked(Type t) {
        return s.stats.power >= t.unlock();
    }

    public boolean isFastAbsorbing() {
        return fastTicks > 0;
    }

    public int pulseVisualTicks() {
        return pulseVisual;
    }

    public double effectiveRange() {
        return isUnlocked(Type.LONG_RANGE) ? s.stats.attackRange : Math.min(s.stats.attackRange, StormConfig.normalRange);
    }

    private boolean ready(Type t) {
        return isUnlocked(t) && cd[t.ordinal()] <= 0;
    }

    private void start(Type t) {
        cd[t.ordinal()] = Math.max(5, (int) (t.cooldown() / (1.0 + 0.03 * s.stats.power)));
    }

    private double pulseRadius() {
        return EvolutionFormulas.clamp(14 + 3 * s.stats.power + 1.5 * s.stats.scale, 8, StormConfig.pulseMaxRadius);
    }

    private double strikeRange() {
        return Math.min(s.stats.tentacleReach, 10 + 3 * Math.pow(s.stats.scale, 0.8));
    }

    private double distTo(EntityLivingBase t) {
        Vec3 c = s.getBodyCenter();
        double dx = t.posX - c.xCoord, dy = t.posY - c.yCoord, dz = t.posZ - c.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void tick(EntityLivingBase target) {
        for (int i = 0; i < cd.length; i++) if (cd[i] > 0) cd[i]--;
        if (fastTicks > 0) fastTicks--;
        if (pulseVisual > 0) pulseVisual--;
        runQueues();

        if (ready(Type.FAST_ABSORB) && s.getAbsorber().hasWork()) {
            fastTicks = StormConfig.fastAbsorbDuration;
            start(Type.FAST_ABSORB);
        }
        if (target == null) return;

        double dist = distTo(target), range = effectiveRange();
        if (ready(Type.PULSE) && dist <= pulseRadius() * 0.9) {
            pulse();
            start(Type.PULSE);
        } else if (ready(Type.TENTACLE) && dist <= strikeRange()) {
            tentacleStrike();
            start(Type.TENTACLE);
        } else if (ready(Type.BOMBARD) && dist <= range) {
            bombLeft = (int) EvolutionFormulas.clamp(5 + 2 * s.stats.power, 3, StormConfig.bombardMax);
            bx = target.posX;
            by = target.posY;
            bz = target.posZ;
            queuedTarget = target;
            queueTimeout = 200;
            start(Type.BOMBARD);
        } else if (ready(Type.BARRAGE) && dist <= range) {
            barrageLeft = s.stats.barrageShots;
            queuedTarget = target;
            queueTimeout = 200;
            start(Type.BARRAGE);
        } else if (ready(Type.LONG_RANGE) && dist > StormConfig.normalRange * 0.6 && dist <= range) {
            fireAt(target, 0, 0.01, 2.0, 1.5, 1.5, 1.5);
            start(Type.LONG_RANGE);
        }
        if (ready(Type.BASIC) && dist <= range) {
            int n = Math.min(s.getHeadCount(), StormConfig.maxSkullsPerTick);
            for (int i = 0; i < n; i++) fireAt(target, i, 0.02, 1.0, 1.0, 1.0, 1.0);
            start(Type.BASIC);
        }
    }

    private boolean fireAt(EntityLivingBase t, int head, double spread, double speedMul, double dmgMul, double explMul, double sizeMul) {
        Vec3 from = s.getHeadPos(head);
        double dist = distTo(t);
        Vec3 to = new Vec3(t.posX + (rand.nextDouble() - 0.5) * spread * dist,
                t.posY + t.height * 0.6 + (rand.nextDouble() - 0.5) * spread * dist,
                t.posZ + (rand.nextDouble() - 0.5) * spread * dist);
        double speed = Math.min(s.stats.skullSpeed * speedMul, StormConfig.maxSkullSpeed * 2.0);
        int maxAge = (int) (s.stats.attackRange * 1.5 / Math.max(0.1, speed)) + 40;
        return s.fireSkull(from, to, speed, (float) (s.stats.damage * dmgMul), (float) (s.stats.explosion * explMul),
                (float) (s.stats.skullSize * sizeMul), maxAge);
    }

    private void runQueues() {
        if (barrageLeft > 0 || bombLeft > 0) {
            if (queuedTarget == null || queuedTarget.isDead || --queueTimeout <= 0) {
                barrageLeft = 0;
                bombLeft = 0;
            }
        }
        if (barrageLeft > 0) {
            for (int k = 0; k < Math.min(StormConfig.maxSkullsPerTick, 2) && barrageLeft > 0; k++) {
                if (fireAt(queuedTarget, rand.nextInt(s.getHeadCount()), 0.12, 1.0, 1.0, 1.0, 1.0)) barrageLeft--;
            }
        }
        if (bombLeft > 0) {
            if (queuedTarget != null) {
                bx = queuedTarget.posX;
                by = queuedTarget.posY;
                bz = queuedTarget.posZ;
            }
            double r = EvolutionFormulas.clamp(6 + s.stats.power * 0.5, 6, 24);
            for (int k = 0; k < 2 && bombLeft > 0; k++) {
                double ix = bx + (rand.nextDouble() - 0.5) * 2 * r, iz = bz + (rand.nextDouble() - 0.5) * 2 * r;
                Vec3 from = new Vec3(ix + (rand.nextDouble() - 0.5) * 12, by + 28 + rand.nextDouble() * 10, iz + (rand.nextDouble() - 0.5) * 12);
                Vec3 to = new Vec3(ix, by, iz);
                double speed = Math.min(s.stats.skullSpeed * 1.2, StormConfig.maxSkullSpeed);
                if (s.fireSkull(from, to, speed, (float) s.stats.damage, (float) s.stats.explosion, s.stats.skullSize, 120)) bombLeft--;
            }
        }
        if (blastsLeft > 0 && s.ticksExisted % 3 == 0) {
            World w = s.worldObj;
            double a = rand.nextDouble() * Math.PI * 2, r = pulseR * 0.8 * Math.sqrt(rand.nextDouble());
            double x = s.posX + Math.cos(a) * r, z = s.posZ + Math.sin(a) * r;
            double y = s.posY;
            BlockPos probe = new BlockPos((int) Math.floor(x), 64, (int) Math.floor(z));
            if (w.isBlockLoaded(probe)) y = w.getHeight(probe).getY();
            if (s.reserveExplosion()) {
                boolean grief = StormConfig.explosionsDestroyBlocks && w.getGameRules().getBoolean("mobGriefing");
                w.newExplosion(s, x, y, z, (float) Math.min(4.0, s.stats.explosion), false, grief);
            }
            blastsLeft--;
        }
    }

    private void tentacleStrike() {
        double r = strikeRange();
        Vec3 c = s.getBodyCenter();
        AxisAlignedBB box = new AxisAlignedBB(c.xCoord - r, c.yCoord - r, c.zCoord - r, c.xCoord + r, c.yCoord + r, c.zCoord + r);
        List<EntityLivingBase> list = s.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);
        int n = 0;
        for (EntityLivingBase e : list) {
            if (n >= StormConfig.maxAbilityTargets) break;
            if (!s.isValidTarget(e, r)) continue;
            e.attackEntityFrom(DamageSource.causeMobDamage(s), (float) (s.stats.damage * 0.6));
            n++;
        }
        s.worldObj.playSoundAtEntity(s, "mob.wither.shoot", 4.0F, 0.6F);
    }

    private void pulse() {
        pulseR = pulseRadius();
        pulseVisual = 20;
        Vec3 c = s.getBodyCenter();
        AxisAlignedBB box = new AxisAlignedBB(c.xCoord - pulseR, c.yCoord - pulseR, c.zCoord - pulseR,
                c.xCoord + pulseR, c.yCoord + pulseR, c.zCoord + pulseR);
        List<EntityLivingBase> list = s.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);
        int n = 0;
        for (EntityLivingBase e : list) {
            if (n >= StormConfig.maxAbilityTargets) break;
            if (!s.isValidTarget(e, pulseR)) continue;
            double falloff = 1.0 - 0.6 * Math.min(1.0, distTo(e) / pulseR);
            e.attackEntityFrom(DamageSource.causeMobDamage(s), (float) (s.stats.damage * 1.5 * falloff));
            n++;
        }
        blastsLeft = (int) EvolutionFormulas.clamp(2 + s.stats.power / 2, 2, 6);
        s.worldObj.playSoundAtEntity(s, "random.explode", 6.0F, 0.5F);
    }
}
