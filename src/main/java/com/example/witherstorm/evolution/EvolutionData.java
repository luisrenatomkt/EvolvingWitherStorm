package com.example.witherstorm.evolution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.example.witherstorm.WitherStormMod;
import com.example.witherstorm.config.StormConfig;

/** Estado evolutivo persistente: massa (blocos) e progresso (mortes). */
public class EvolutionData {
    public double mass;
    public double progress;
    public long blocksAbsorbed;
    public long kills;
    public boolean dirty = true;
    private boolean warned;

    /** Mortes ja contabilizadas (limitado, evita contagem dupla). */
    private final Map<UUID, Boolean> counted = new LinkedHashMap<UUID, Boolean>(16, 0.75f, false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
            return size() > 1024;
        }
    };

    public boolean isFull() {
        return mass >= StormConfig.maxMass;
    }

    /** Adiciona massa de UM bloco ja removido. Retorna quanto foi realmente somado. */
    public double addMass(double m) {
        if (Double.isNaN(m) || Double.isInfinite(m) || m <= 0) return 0;
        double room = StormConfig.maxMass - mass;
        if (room <= 0) {
            if (!warned) {
                warned = true;
                WitherStormMod.LOG.warn("Wither Storm atingiu o limite de seguranca de massa (maxMass).");
            }
            return 0;
        }
        double add = Math.min(m, room);
        mass += add;
        blocksAbsorbed++;
        dirty = true;
        return add;
    }

    /** Registra uma morte uma unica vez. */
    public boolean registerKill(UUID victim, double points) {
        if (victim == null || Double.isNaN(points) || Double.isInfinite(points) || points <= 0) return false;
        if (counted.containsKey(victim)) return false;
        counted.put(victim, Boolean.TRUE);
        progress = Math.min(StormConfig.maxMass, progress + points);
        kills++;
        dirty = true;
        return true;
    }

    public void writeToNBT(NBTTagCompound t) {
        t.setDouble("Mass", mass);
        t.setDouble("Progress", progress);
        t.setLong("Blocks", blocksAbsorbed);
        t.setLong("Kills", kills);
        t.setInteger("Version", 1);
    }

    public void readFromNBT(NBTTagCompound t) {
        mass = sane(t.getDouble("Mass"));
        progress = sane(t.getDouble("Progress"));
        blocksAbsorbed = Math.max(0, t.getLong("Blocks"));
        kills = Math.max(0, t.getLong("Kills"));
        dirty = true;
    }

    private static double sane(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) return 0;
        return Math.min(v, StormConfig.maxMass);
    }
}
