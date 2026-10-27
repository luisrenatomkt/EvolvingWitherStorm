package com.example.witherstorm.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.example.witherstorm.WitherStormMod;
import com.example.witherstorm.ability.AbilityManager;
import com.example.witherstorm.absorb.BlockAbsorber;
import com.example.witherstorm.absorb.StormChunkLoader;
import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.evolution.EvolutionData;
import com.example.witherstorm.evolution.EvolutionFormulas;
import com.example.witherstorm.network.NetworkHandler;
import com.example.witherstorm.network.PacketGhost;

public class EntityWitherStorm extends EntityLiving implements IMob, IBossDisplayData {
    /** 1 pixel do modelo = 0.1 bloco (na escala 1). */
    public static final double PX = 0.1D;
    public static final float BASE_W = 2.4F, BASE_H = 5.2F;
    public static final int DW_SCALE = 20, DW_STAGE = 21, DW_FLAGS = 22;
    public static final int FLAG_ATTACK = 1, FLAG_FAST = 2, FLAG_PULSE = 4;

    private static final double[] HEAD_X = { 0, -9, 9, -5, 5, -14, 14 };
    private static final double[] HEAD_UP = { 9, 6, 6, 13, 13, 2, 2 };

    /** Bloco em voo (apenas cliente, visual). */
    public static final class FlyingBlock {
        public final IBlockState state;
        public final double sx, sy, sz;
        public int age;
        public final int life;

        public FlyingBlock(IBlockState state, double sx, double sy, double sz, int life) {
            this.state = state;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.life = life;
        }
    }

    public final EvolutionData evo = new EvolutionData();
    public EvolutionFormulas.Stats stats;
    public final List<FlyingBlock> flying = new ArrayList<FlyingBlock>();

    private final BlockAbsorber absorber;
    private final AbilityManager abilities;
    private final StormChunkLoader chunkLoader = new StormChunkLoader();
    private EntityLivingBase target;
    private Vec3 roamDest;
    private int roamUntil;
    private int activeSkulls, skullsThisTick, explosionsThisTick;
    private long lastSkullBlast = -100;
    private float lastHitScale = -1.0F;
    private double lastX, lastY, lastZ;
    private int stuckCount;
    private float smoothScale = 1.0F, prevSmoothScale = 1.0F;

    public EntityWitherStorm(World world) {
        super(world);
        this.isImmuneToFire = true;
        this.noClip = StormConfig.passThroughBlocks; // por padrao COLIDE com blocos e abre caminho absorvendo-os
        this.ignoreFrustumCheck = true;
        this.experienceValue = 500;
        this.renderDistanceWeight = 50.0D;
        this.stats = EvolutionFormulas.compute(evo);
        this.absorber = new BlockAbsorber(this);
        this.abilities = new AbilityManager(this);
        refreshDerived(true);
        this.smoothScale = this.prevSmoothScale = getRenderScale();
    }

    // ---------------------------------------------------------------- atributos / dados

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // 1024 e o teto da barra de vida no 1.8.9: a vida real evolui via reducao de dano (ver attackEntityFrom).
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1024.0D);
        this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(256.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(DW_SCALE, Float.valueOf(1.0F));
        this.dataWatcher.addObject(DW_STAGE, Integer.valueOf(0));
        this.dataWatcher.addObject(DW_FLAGS, Byte.valueOf((byte) 0));
    }

    public float getRenderScale() {
        return this.dataWatcher.getWatchableObjectFloat(DW_SCALE);
    }

    public float getSmoothScale(float partial) {
        return prevSmoothScale + (smoothScale - prevSmoothScale) * partial;
    }

    public int getStage() {
        return this.dataWatcher.getWatchableObjectInt(DW_STAGE);
    }

    public int getFlags() {
        return this.dataWatcher.getWatchableObjectByte(DW_FLAGS);
    }

    public int getTentacleCount() {
        return Math.min(24, 4 + getStage() * 2);
    }

    public int getHeadCount() {
        int st = getStage();
        return st < 3 ? 3 : (st < 6 ? 5 : 7);
    }

    public BlockAbsorber getAbsorber() {
        return absorber;
    }

    public AbilityManager getAbilities() {
        return abilities;
    }

    public EntityLivingBase getTarget() {
        return target;
    }

    /** Recalcula os valores derivados da evolucao. Nao altera a vida atual (sem cura indevida). */
    public void refreshDerived(boolean force) {
        stats = EvolutionFormulas.compute(evo);
        float rs = (float) Math.min(stats.scale, StormConfig.maxRenderScale);
        if (!worldObj.isRemote) {
            if (Math.abs(getRenderScale() - rs) > 1.0E-4F) this.dataWatcher.updateObject(DW_SCALE, Float.valueOf(rs));
            if (getStage() != stats.stage) this.dataWatcher.updateObject(DW_STAGE, Integer.valueOf(stats.stage));
        }
        applyHitbox(rs, force);
        evo.dirty = false;
    }

    private void applyHitbox(float renderScale, boolean force) {
        float hs = (float) Math.max(1.0, Math.min(renderScale, StormConfig.maxHitboxScale));
        if (force || lastHitScale < 0 || Math.abs(hs - lastHitScale) > 0.01F * lastHitScale) {
            lastHitScale = hs;
            double x = posX, y = posY, z = posZ;
            this.setSize(BASE_W * hs, BASE_H * hs);
            this.setPosition(x, y, z);
        }
    }

    /** Centro do corpo (referencia para absorcao, cabecas e render). */
    public Vec3 getBodyCenter() {
        double hs = this.height / BASE_H;
        return new Vec3(posX, posY + this.height * 0.5D + 1.2D * hs, posZ);
    }

    public Vec3 getHeadPos(int i) {
        i = Math.max(0, Math.min(i, HEAD_X.length - 1));
        double s = getRenderScale();
        double yaw = Math.toRadians(rotationYaw);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = Math.cos(yaw), rz = Math.sin(yaw);
        double lat = HEAD_X[i] * PX * s, up = HEAD_UP[i] * PX * s, fwd = 4.0 * PX * s;
        Vec3 c = getBodyCenter();
        return new Vec3(c.xCoord + rx * lat + fx * fwd, c.yCoord + up, c.zCoord + rz * lat + fz * fwd);
    }

    // ---------------------------------------------------------------- dano / fisica

    @Override
    public boolean attackEntityFrom(DamageSource src, float amount) {
        if (isEntityInvulnerable(src)) return false;
        if (src == DamageSource.inWall || src == DamageSource.drown || src == DamageSource.fall) return false;
        if (src.getEntity() instanceof EntityWitherStorm) return false;
        float scaled;
        if (src.canHarmInCreative()) {
            scaled = amount; // /kill e void
        } else {
            scaled = (float) Math.max(0.0, amount * stats.damageTaken * stats.barPerEff);
        }
        if (Float.isNaN(scaled) || Float.isInfinite(scaled)) return false;
        return super.attackEntityFrom(src, scaled);
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (!worldObj.isRemote) {
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.98D;
            this.motionY *= 0.98D;
            this.motionZ *= 0.98D;
        }
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.85F;
    }

    @Override
    public String getName() {
        return "Wither Storm";
    }

    @Override
    protected String getLivingSound() {
        return "mob.wither.idle";
    }

    @Override
    protected String getHurtSound() {
        return "mob.wither.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.wither.death";
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        int n = Math.min(StormConfig.maxNetherStarDrops, 1 + stats.stage / 2);
        for (int i = 0; i < n; i++) this.dropItem(Items.nether_star, 1);
    }

    // ---------------------------------------------------------------- NBT

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagCompound e = new NBTTagCompound();
        evo.writeToNBT(e);
        e.setDouble("CachedScale", stats.scale);
        e.setInteger("CachedStage", stats.stage);
        e.setDouble("CachedPower", stats.power);
        tag.setTag("StormEvolution", e);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("StormEvolution", 10)) evo.readFromNBT(tag.getCompoundTag("StormEvolution"));
        refreshDerived(true);
        smoothScale = prevSmoothScale = getRenderScale();
    }

    // ---------------------------------------------------------------- tick servidor

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (worldObj.isRemote || isDead) return;
        explosionsThisTick = 0;
        skullsThisTick = 0;
        if (evo.dirty) refreshDerived(false);

        if (ticksExisted % 20 == 0 && getHealth() > 0 && getHealth() < getMaxHealth()) {
            this.heal((float) (stats.regenPerSec * stats.barPerEff));
        }
        if (ticksExisted % StormConfig.targetScanInterval == 0 || target == null) updateTarget();
        absorber.tick(abilities.isFastAbsorbing());
        abilities.tick(target);
        steer();
        syncFlags();
        chunkLoader.update(this);
        if (ticksExisted % StormConfig.ghostInterval == 0) sendGhosts();
    }

    /** Envia posicao/escala a jogadores distantes, para o Storm continuar visivel fora do alcance de rastreio. */
    private void sendGhosts() {
        double r = StormConfig.ghostRange;
        if (r <= 0) return;
        r *= r;
        PacketGhost pkt = new PacketGhost(getEntityId(), posX, posY, posZ, rotationYaw, getRenderScale(), getStage(), getFlags());
        for (EntityPlayer p : worldObj.playerEntities) {
            if (!(p instanceof EntityPlayerMP)) continue;
            double dx = p.posX - posX, dz = p.posZ - posZ;
            if (dx * dx + dz * dz <= r) NetworkHandler.CHANNEL.sendTo(pkt, (EntityPlayerMP) p);
        }
    }

    @Override
    public void setDead() {
        if (!worldObj.isRemote) chunkLoader.release();
        super.setDead();
    }

    /** Cliente: sincroniza uma copia "fantasma" (nao adicionada ao mundo) usada so para desenhar. */
    public void ghostSync(float scale, int stage, int flags, float yaw) {
        this.dataWatcher.updateObject(DW_SCALE, Float.valueOf(scale));
        this.dataWatcher.updateObject(DW_STAGE, Integer.valueOf(stage));
        this.dataWatcher.updateObject(DW_FLAGS, Byte.valueOf((byte) flags));
        applyHitbox(scale, true);
        smoothScale = prevSmoothScale = scale;
        rotationYaw = prevRotationYaw = yaw;
    }

    public boolean isValidTarget(EntityLivingBase e, double range) {
        if (e == null || e.isDead || e.getHealth() <= 0 || e == this || e instanceof EntityWitherStorm) return false;
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            if (!StormConfig.targetPlayers || p.capabilities.disableDamage || p.isSpectator()) return false;
        }
        String n = EntityList.getEntityString(e);
        if (n != null && StormConfig.ignoredEntities.contains(n)) return false;
        Vec3 c = getBodyCenter();
        double dx = e.posX - c.xCoord, dy = e.posY - c.yCoord, dz = e.posZ - c.zCoord;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    private void updateTarget() {
        double range = abilities.effectiveRange();
        if (target != null && isValidTarget(target, range * 1.1D) && ticksExisted % 100 != 0) return;
        Vec3 c = getBodyCenter();
        AxisAlignedBB box = new AxisAlignedBB(c.xCoord - range, c.yCoord - range * 0.6, c.zCoord - range,
                c.xCoord + range, c.yCoord + range * 0.6, c.zCoord + range);
        List<EntityLivingBase> list = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);
        EntityLivingBase best = null;
        double bd = Double.MAX_VALUE;
        for (EntityLivingBase e : list) {
            if (!isValidTarget(e, range)) continue;
            double dx = e.posX - c.xCoord, dy = e.posY - c.yCoord, dz = e.posZ - c.zCoord;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        target = best;
    }

    private void pickRoam() {
        for (int tries = 0; tries < 4; tries++) {
            double ang = rand.nextDouble() * Math.PI * 2.0;
            double r = Math.max(24.0, stats.searchRadius * 0.7);
            double x = posX + Math.cos(ang) * r, z = posZ + Math.sin(ang) * r;
            BlockPos probe = new BlockPos(MathHelper.floor_double(x), 64, MathHelper.floor_double(z));
            if (!worldObj.isBlockLoaded(probe)) continue;
            int gy = worldObj.getHeight(probe).getY();
            roamDest = new Vec3(x, EvolutionFormulas.clamp(gy + 6.0, 4, 240), z);
            roamUntil = ticksExisted + 200 + rand.nextInt(200);
            return;
        }
        roamDest = new Vec3(posX, posY + 4.0, posZ);
        roamUntil = ticksExisted + 60;
    }

    /** Movimento autonomo: perseguir alvo > ir ate area de alimentacao > vagar. */
    private void steer() {
        Vec3 c = getBodyCenter();
        Vec3 dest = null;
        double faceX = 0, faceZ = 0;
        boolean hasFace = false;
        double hover = Math.min(60.0, 6.0 + getRenderScale() * 1.5);

        if (target != null) {
            double dx = target.posX - c.xCoord, dz = target.posZ - c.zCoord;
            double hd = Math.sqrt(dx * dx + dz * dz);
            double keep = Math.max(14.0, Math.min(stats.attackRange * 0.5, 22.0 + stats.scale * 2.0));
            double nx = hd > 1.0E-3 ? dx / hd : 1.0, nz = hd > 1.0E-3 ? dz / hd : 0.0;
            double orbit = Math.sin(ticksExisted * 0.03) * keep * 0.4;
            dest = new Vec3(target.posX - nx * keep - nz * orbit, target.posY + hover, target.posZ - nz * keep + nx * orbit);
            faceX = dx;
            faceZ = dz;
            hasFace = true;
        } else if (absorber.getFocus() != null) {
            BlockPos f = absorber.getFocus();
            double dx = f.getX() + 0.5 - c.xCoord, dy = f.getY() - c.yCoord, dz = f.getZ() + 0.5 - c.zCoord;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > stats.tentacleReach * 0.5) {
                dest = new Vec3(f.getX() + 0.5, f.getY() + 1.5, f.getZ() + 0.5);
            }
        } else {
            if (roamDest == null || ticksExisted > roamUntil) pickRoam();
            dest = roamDest;
        }

        if (dest != null) {
            double dx = dest.xCoord - c.xCoord, dy = dest.yCoord - c.yCoord, dz = dest.zCoord - c.zCoord;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0.5) {
                double sp = Math.min(stats.chaseSpeed, dist * 0.12);
                motionX += (dx / dist * sp - motionX) * 0.12;
                motionY += (dy / dist * sp - motionY) * 0.12;
                motionZ += (dz / dist * sp - motionZ) * 0.12;
            } else {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            }
            if (!hasFace && dx * dx + dz * dz > 0.01) {
                faceX = dx;
                faceZ = dz;
                hasFace = true;
            }
        } else {
            motionX *= 0.9;
            motionY *= 0.9;
            motionZ *= 0.9;
        }
        if (Double.isNaN(motionX) || Double.isNaN(motionY) || Double.isNaN(motionZ)) {
            motionX = motionY = motionZ = 0;
        }
        if (hasFace) {
            float want = (float) (Math.atan2(-faceX, faceZ) * 180.0D / Math.PI);
            float diff = MathHelper.wrapAngleTo180_float(want - rotationYaw);
            rotationYaw += MathHelper.clamp_float(diff, -4.0F, 4.0F);
            renderYawOffset = rotationYaw;
            rotationYawHead = rotationYaw;
        }
        // evita ficar preso: se nao saiu do lugar, escolhe outra area
        if (ticksExisted % 20 == 0) {
            double mx = posX - lastX, my = posY - lastY, mz = posZ - lastZ;
            if (dest != null && target == null && mx * mx + my * my + mz * mz < 0.25) stuckCount++;
            else stuckCount = 0;
            if (stuckCount >= 5) {
                pickRoam();
                absorber.clearFocus();
                stuckCount = 0;
            }
            lastX = posX;
            lastY = posY;
            lastZ = posZ;
        }
    }

    private void syncFlags() {
        int f = 0;
        if (target != null) f |= FLAG_ATTACK;
        if (abilities.isFastAbsorbing()) f |= FLAG_FAST;
        if (abilities.pulseVisualTicks() > 0) f |= FLAG_PULSE;
        if (f != getFlags()) this.dataWatcher.updateObject(DW_FLAGS, Byte.valueOf((byte) f));
    }

    // ---------------------------------------------------------------- projeteis e mortes

    public boolean fireSkull(Vec3 from, Vec3 to, double speed, float dmg, float expl, float size, int maxAge) {
        if (worldObj.isRemote) return false;
        if (activeSkulls >= StormConfig.maxActiveSkulls || skullsThisTick >= StormConfig.maxSkullsPerTick) return false;
        double dx = to.xCoord - from.xCoord, dy = to.yCoord - from.yCoord, dz = to.zCoord - from.zCoord;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (Double.isNaN(d) || d < 1.0E-4 || Double.isNaN(speed)) return false;
        EntityStormSkull sk = new EntityStormSkull(worldObj, this);
        sk.setPosition(from.xCoord, from.yCoord, from.zCoord);
        sk.motionX = dx / d * speed;
        sk.motionY = dy / d * speed;
        sk.motionZ = dz / d * speed;
        sk.configure(dmg, expl, size, maxAge);
        if (!worldObj.spawnEntityInWorld(sk)) return false;
        activeSkulls++;
        skullsThisTick++;
        return true;
    }

    public void skullDied() {
        if (activeSkulls > 0) activeSkulls--;
    }

    public boolean reserveExplosion() {
        if (explosionsThisTick >= StormConfig.maxExplosionsPerTick) return false;
        explosionsThisTick++;
        return true;
    }

    public void markSkullBlast() {
        lastSkullBlast = worldObj.getTotalWorldTime();
    }

    public boolean recentSkullBlast() {
        return worldObj.getTotalWorldTime() - lastSkullBlast <= 2;
    }

    public void onKill(EntityLivingBase victim, double points) {
        if (worldObj.isRemote) return;
        if (evo.registerKill(victim.getUniqueID(), points)) refreshDerived(false);
    }

    // ---------------------------------------------------------------- cliente (visual)

    public void addFlyingBlock(int x, int y, int z, int stateId) {
        double dx = x + 0.5 - posX, dy = y + 0.5 - posY, dz = z + 0.5 - posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int life = MathHelper.clamp_int((int) (dist * 0.8) + 8, 10, 40);
        if (flying.size() >= 96) flying.remove(0);
        flying.add(new FlyingBlock(Block.getStateById(stateId), x + 0.5, y + 0.5, z + 0.5, life));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) clientTick();
    }

    private void clientTick() {
        prevSmoothScale = smoothScale;
        float target = getRenderScale();
        smoothScale += (target - smoothScale) * 0.08F;
        applyHitbox(target, false);

        for (int i = flying.size() - 1; i >= 0; i--) {
            FlyingBlock fb = flying.get(i);
            if (++fb.age >= fb.life) flying.remove(i);
        }
        Vec3 c = getBodyCenter();
        double s = Math.max(1.0, smoothScale);
        int n = Math.min(StormConfig.maxParticlesPerTick, 1 + getStage());
        for (int i = 0; i < n; i++) {
            double ox = (rand.nextDouble() - 0.5) * 2.0 * s, oy = (rand.nextDouble() - 0.5) * 2.0 * s, oz = (rand.nextDouble() - 0.5) * 2.0 * s;
            worldObj.spawnParticle(rand.nextBoolean() ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.PORTAL,
                    c.xCoord + ox, c.yCoord + oy, c.zCoord + oz, 0.0, 0.02, 0.0);
        }
        if ((getFlags() & FLAG_PULSE) != 0) {
            double r = ((ticksExisted % 20) / 20.0) * s * 16.0;
            for (int i = 0; i < Math.min(24, StormConfig.maxParticlesPerTick * 4); i++) {
                double a = rand.nextDouble() * Math.PI * 2.0;
                worldObj.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, c.xCoord + Math.cos(a) * r, c.yCoord - s, c.zCoord + Math.sin(a) * r, 0, 0.05, 0);
            }
        }
    }
}
