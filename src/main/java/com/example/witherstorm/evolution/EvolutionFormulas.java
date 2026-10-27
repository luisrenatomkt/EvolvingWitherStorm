package com.example.witherstorm.evolution;

import com.example.witherstorm.config.StormConfig;

/** Formulas progressivas (sem nivel maximo fixo); todos os resultados sao limitados e livres de NaN. */
public final class EvolutionFormulas {
    private EvolutionFormulas() {}

    public static double clamp(double v, double lo, double hi) {
        if (hi < lo) hi = lo;
        if (Double.isNaN(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static double power(EvolutionData d) {
        double a = Math.log1p(Math.max(0, d.mass) / StormConfig.massUnit) * StormConfig.massPowerWeight;
        double b = Math.log1p(Math.max(0, d.progress) / StormConfig.killUnit) * StormConfig.killPowerWeight;
        return clamp(a + b, 0, 1e6);
    }

    public static double scale(double mass) {
        double v = 1.0 + StormConfig.scaleCoeff * Math.pow(Math.max(0, mass) / StormConfig.massUnit, StormConfig.scaleExp);
        return clamp(v, 1.0, StormConfig.maxLogicalScale);
    }

    public static int stage(double mass) {
        double v = Math.log1p(Math.max(0, mass) / StormConfig.stageMass) / Math.log(2.0);
        return (int) clamp(Math.floor(v), 0, 1000);
    }

    public static final class Stats {
        public double power, scale, effHp, barPerEff, damageTaken, damage, skullSpeed, attackRange, explosion,
                regenPerSec, blocksPerTick, tentacleReach, searchRadius, chaseSpeed;
        public int stage, barrageShots;
        public float skullSize;
    }

    public static Stats compute(EvolutionData d) {
        Stats s = new Stats();
        double P = power(d);
        s.power = P;
        s.scale = scale(d.mass);
        s.stage = stage(d.mass);
        s.effHp = clamp(StormConfig.hpBase * (1 + StormConfig.hpGrowth * Math.pow(P, StormConfig.hpExp)), 1, StormConfig.maxEffectiveHp);
        s.barPerEff = 1024.0 / s.effHp;
        s.damageTaken = clamp(1.0 / (1.0 + StormConfig.resGrowth * P), StormConfig.minDamageTaken, 1.0);
        s.damage = clamp(StormConfig.dmgBase * (1 + StormConfig.dmgGrowth * P), 0, StormConfig.maxDamage);
        s.skullSpeed = clamp(StormConfig.skullSpeedBase * (1 + StormConfig.skullSpeedGrowth * P), 0.1, StormConfig.maxSkullSpeed);
        s.attackRange = clamp(StormConfig.rangeBase * (1 + StormConfig.rangeGrowth * P), 8, StormConfig.maxRange);
        s.explosion = clamp(StormConfig.explBase * (1 + StormConfig.explGrowth * P), 0, StormConfig.maxExplosion);
        s.regenPerSec = clamp(StormConfig.regenBase * (1 + StormConfig.regenGrowth * Math.pow(P, 1.2)), 0, StormConfig.maxRegen);
        s.blocksPerTick = clamp(StormConfig.blocksBase * (1 + StormConfig.blocksGrowth * P), 1, StormConfig.maxBlocksPerTick);
        s.tentacleReach = clamp(StormConfig.reachBase + StormConfig.reachPerScale * Math.pow(s.scale, 0.9) + 1.5 * P, 4, StormConfig.maxReach);
        s.searchRadius = clamp(s.tentacleReach * 1.5 + 16, 16, StormConfig.maxSearchRadius);
        s.chaseSpeed = clamp(StormConfig.speedBase * (1 + StormConfig.speedGrowth * P) + 0.01 * Math.min(s.scale, 50), 0.05, StormConfig.maxSpeed);
        s.barrageShots = (int) clamp(4 + P * 1.5, 2, StormConfig.maxBarrageShots);
        s.skullSize = (float) (1.0 + Math.min(2.0, s.explosion * 0.1));
        return s;
    }
}
