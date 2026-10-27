package com.example.witherstorm.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.example.witherstorm.config.StormConfig;

/** Projetil do Wither Storm: guarda o dono para atribuir mortes de forma confiavel. */
public class EntityStormSkull extends Entity {
    private EntityWitherStorm shooter;
    private float damage = 5.0F, explosion = 1.0F;
    private int maxAge = 200, age;

    public EntityStormSkull(World world) {
        super(world);
        this.setSize(0.35F, 0.35F);
        this.renderDistanceWeight = 10.0D;
    }

    public EntityStormSkull(World world, EntityWitherStorm shooter) {
        this(world);
        this.shooter = shooter;
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(10, Float.valueOf(1.0F));
    }

    public void configure(float dmg, float expl, float size, int maxAge) {
        this.damage = (float) Math.max(0, Math.min(dmg, StormConfig.maxDamage));
        this.explosion = (float) Math.max(0, Math.min(expl, StormConfig.maxExplosion * 1.5));
        this.maxAge = Math.max(20, Math.min(maxAge, 1200));
        this.dataWatcher.updateObject(10, Float.valueOf(Math.max(0.5F, Math.min(size, 4.0F))));
    }

    public float getSkullSize() {
        return this.dataWatcher.getWatchableObjectFloat(10);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean attackEntityFrom(net.minecraft.util.DamageSource src, float amount) {
        return false;
    }

    @Override
    public void setDead() {
        if (!this.isDead && shooter != null) shooter.skullDied();
        super.setDead();
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false; // projeteis nao sao salvos
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote) {
            if (shooter == null || shooter.isDead || ++age > maxAge) {
                setDead();
                return;
            }
            Vec3 a = new Vec3(posX, posY, posZ);
            Vec3 b = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            MovingObjectPosition mop = worldObj.rayTraceBlocks(a, b);
            if (mop != null) b = new Vec3(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);

            Entity hit = null;
            double best = 0.0D;
            AxisAlignedBB box = getEntityBoundingBox().addCoord(motionX, motionY, motionZ).expand(1.0D, 1.0D, 1.0D);
            List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, box);
            for (Entity e : list) {
                if (!e.canBeCollidedWith() || e == shooter || e instanceof EntityWitherStorm || e instanceof EntityStormSkull) continue;
                if (e instanceof EntityPlayer) {
                    EntityPlayer p = (EntityPlayer) e;
                    if (p.capabilities.disableDamage || p.isSpectator()) continue;
                }
                AxisAlignedBB bb = e.getEntityBoundingBox().expand(0.3D, 0.3D, 0.3D);
                MovingObjectPosition m = bb.calculateIntercept(a, b);
                if (m != null) {
                    double d = a.squareDistanceTo(m.hitVec);
                    if (hit == null || d < best) {
                        hit = e;
                        best = d;
                    }
                }
            }
            if (hit != null) mop = new MovingObjectPosition(hit);
            if (mop != null) {
                onImpact(mop);
                if (isDead) return;
            }
        } else if (rand.nextInt(2) == 0) {
            worldObj.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY, posZ, 0.0, 0.0, 0.0);
        }
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(posX, posY, posZ);
    }

    private void onImpact(MovingObjectPosition mop) {
        if (mop.hitVec != null) setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        if (mop.entityHit != null) {
            Entity hit = mop.entityHit;
            hit.attackEntityFrom(new EntityDamageSourceIndirect("witherSkull", this, shooter), damage);
            if (hit instanceof EntityLivingBase) {
                ((EntityLivingBase) hit).addPotionEffect(new PotionEffect(Potion.wither.id, 200, 1));
            }
        }
        shooter.markSkullBlast();
        if (explosion > 0.0F && shooter.reserveExplosion()) {
            boolean grief = StormConfig.explosionsDestroyBlocks && worldObj.getGameRules().getBoolean("mobGriefing");
            worldObj.newExplosion(shooter, posX, posY, posZ, explosion, false, grief);
        }
        setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }
}
