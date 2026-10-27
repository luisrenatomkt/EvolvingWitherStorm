package com.example.witherstorm.evolution;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityStormSkull;
import com.example.witherstorm.entity.EntityWitherStorm;

/** Atribui mortes ao Storm (uma unica vez por vitima). Roda apenas no servidor. */
public final class KillTracker {
    private KillTracker() {}

    public static void handle(EntityLivingBase victim, DamageSource src) {
        if (src == null || victim == null || victim instanceof EntityWitherStorm) return;
        Entity attacker = src.getEntity();
        if (!(attacker instanceof EntityWitherStorm)) return;
        EntityWitherStorm storm = (EntityWitherStorm) attacker;
        boolean viaSkull = src.getSourceOfDamage() instanceof EntityStormSkull
                || (src.isExplosion() && storm.recentSkullBlast());
        if (!viaSkull && !StormConfig.creditAllStormDamage) return;
        storm.onKill(victim, pointsFor(victim));
    }

    public static double pointsFor(EntityLivingBase victim) {
        double pts;
        String name = EntityList.getEntityString(victim);
        Double table = name == null ? null : StormConfig.killTable.get(name);
        if (table != null) {
            pts = table;
        } else {
            pts = Math.max(1.0, victim.getMaxHealth()) * StormConfig.killProgressPerHealth;
        }
        if (victim instanceof EntityPlayer) pts *= StormConfig.playerKillMultiplier;
        return EvolutionFormulas.clamp(pts, 0, StormConfig.maxProgressPerKill);
    }
}
