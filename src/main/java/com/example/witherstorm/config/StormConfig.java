package com.example.witherstorm.config;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

/** Todas as configuracoes do mod (arquivo config/witherstorm.cfg). */
public final class StormConfig {
    private StormConfig() {}

    private static Configuration cfg;

    // ---- geral
    public static boolean convertVanillaWither, respectMobGriefing, absorptionEnabled, explosionsDestroyBlocks,
            targetPlayers, creditAllStormDamage, absorbTileEntities, absorbLiquids;
    public static int maxNetherStarDrops;

    // ---- crescimento
    public static double massUnit, scaleCoeff, scaleExp, stageMass, maxLogicalScale, maxHitboxScale, maxRenderScale, maxMass;

    // ---- poder evolutivo
    public static double massPowerWeight, killPowerWeight, killUnit;

    // ---- atributos
    public static double hpBase, hpGrowth, hpExp, maxEffectiveHp, dmgBase, dmgGrowth, maxDamage, resGrowth, minDamageTaken,
            skullSpeedBase, skullSpeedGrowth, maxSkullSpeed, rangeBase, rangeGrowth, maxRange, explBase, explGrowth,
            maxExplosion, regenBase, regenGrowth, maxRegen, blocksBase, blocksGrowth, reachBase, reachPerScale, maxReach,
            maxSearchRadius, speedBase, speedGrowth, maxSpeed;
    public static int maxBarrageShots;

    // ---- absorcao
    public static int maxBlocksPerTick, samplesPerTick, queueCapacity, scanInterval, scanSamples, invalidCacheSize,
            maxVisualBlocksPerTick, fastAbsorbDuration;
    public static double baseBlockMass, fastAbsorbMultiplier;
    public static Set<String> protectedBlocks = new HashSet<String>();
    public static Set<String> blockBlacklist = new HashSet<String>();
    public static Set<String> blockWhitelist = new HashSet<String>();
    public static Map<String, Double> massOverrides = new HashMap<String, Double>();

    // ---- mortes e alvos
    public static double killProgressPerHealth, playerKillMultiplier, maxProgressPerKill, normalRange;
    public static int targetScanInterval;
    public static Map<String, Double> killTable = new HashMap<String, Double>();
    public static Set<String> ignoredEntities = new HashSet<String>();

    // ---- habilidades
    public static double unlockBarrage, unlockTentacle, unlockFastAbsorb, unlockBombard, unlockLongRange, unlockPulse, pulseMaxRadius;
    public static int cdBasic, cdBarrage, cdBombard, cdTentacle, cdFastAbsorb, cdPulse, cdLongRange;
    public static int maxActiveSkulls, maxSkullsPerTick, maxExplosionsPerTick, maxAbilityTargets, bombardMax;

    // ---- cliente
    public static int maxParticlesPerTick;

    private static double d(String c, String k, double def, double min, double max, String comment) {
        return cfg.getFloat(k, c, (float) def, (float) min, (float) max, comment);
    }

    private static int i(String c, String k, int def, int min, int max, String comment) {
        return cfg.getInt(k, c, def, min, max, comment);
    }

    private static boolean b(String c, String k, boolean def, String comment) {
        return cfg.getBoolean(k, c, def, comment);
    }

    private static Set<String> set(String[] a) {
        Set<String> s = new HashSet<String>();
        for (String x : a) {
            if (x != null && x.trim().length() > 0) s.add(x.trim());
        }
        return s;
    }

    private static Map<String, Double> map(String[] a) {
        Map<String, Double> m = new HashMap<String, Double>();
        for (String x : a) {
            if (x == null) continue;
            int p = x.lastIndexOf('=');
            if (p <= 0) continue;
            try {
                double v = Double.parseDouble(x.substring(p + 1).trim());
                if (!Double.isNaN(v) && !Double.isInfinite(v)) m.put(x.substring(0, p).trim(), v);
            } catch (NumberFormatException ignored) {
            }
        }
        return m;
    }

    public static void load(File file) {
        cfg = new Configuration(file);
        cfg.load();

        String g = "general";
        convertVanillaWither = b(g, "convertVanillaWither", true, "Todo Wither vanilla que aparecer no mundo vira um Wither Storm evolutivo.");
        respectMobGriefing = b(g, "respectMobGriefing", true, "Se true, a absorcao de blocos so ocorre quando a gamerule mobGriefing esta ligada.");
        absorptionEnabled = b(g, "absorptionEnabled", true, "Liga/desliga a absorcao de blocos.");
        explosionsDestroyBlocks = b(g, "explosionsDestroyBlocks", true, "Explosoes dos projeteis destroem blocos (sem contar como massa).");
        targetPlayers = b(g, "targetPlayers", true, "O Storm ataca jogadores (survival/adventure).");
        creditAllStormDamage = b(g, "creditAllStormDamage", true, "Mortes por tentaculos e pulso tambem dao progresso (alem de projeteis).");
        maxNetherStarDrops = i(g, "maxNetherStarDrops", 4, 0, 64, "Maximo de estrelas do Nether dropadas ao morrer.");

        String c = "growth";
        massUnit = d(c, "massUnit", 100, 0.001, 1e12, "Unidade de massa usada nas formulas.");
        scaleCoeff = d(c, "scaleCoeff", 0.3, 0, 1000, "escala = 1 + scaleCoeff * (massa/massUnit)^scaleExp");
        scaleExp = d(c, "scaleExp", 0.5, 0.05, 2, "Expoente da formula de escala.");
        stageMass = d(c, "stageMass", 500, 1, 1e12, "Estagio visual = floor(log2(1 + massa/stageMass)). Estagios sao marcos, nao limites.");
        maxLogicalScale = d(c, "maxLogicalScale", 1e6, 1, 1e9, "Limite de seguranca numerica da escala logica.");
        maxHitboxScale = d(c, "maxHitboxScale", 20, 1, 200, "Limite da escala da caixa de colisao (acima disso so o logico/visual cresce).");
        maxRenderScale = d(c, "maxRenderScale", 40, 1, 400, "Limite da escala visual (render). Acima disso crescem tentaculos/alcance, nao o modelo.");
        maxMass = d(c, "maxMass", 1e18, 1e3, 1e30, "Limite de seguranca contra overflow da massa (avisa no log se atingido).");

        String p = "power";
        massPowerWeight = d(p, "massPowerWeight", 0.5, 0, 100, "Peso da massa no poder evolutivo (log).");
        killPowerWeight = d(p, "killPowerWeight", 1.0, 0, 100, "Peso do progresso por mortes no poder evolutivo (log).");
        killUnit = d(p, "killUnit", 50, 0.001, 1e12, "Unidade de progresso por mortes.");

        String a = "attributes";
        hpBase = d(a, "hpBase", 300, 1, 1e12, "Vida efetiva base.");
        hpGrowth = d(a, "hpGrowth", 0.6, 0, 1000, "");
        hpExp = d(a, "hpExp", 1.5, 0.1, 5, "");
        maxEffectiveHp = d(a, "maxEffectiveHp", 1e12, 1, 1e30, "Vida efetiva maxima. A barra de vida do jogo e limitada a 1024; o resto vira reducao de dano.");
        dmgBase = d(a, "damageBase", 5, 0, 1e6, "");
        dmgGrowth = d(a, "damageGrowth", 0.25, 0, 1000, "");
        maxDamage = d(a, "maxDamage", 1e5, 1, 1e9, "");
        resGrowth = d(a, "resistanceGrowth", 0.15, 0, 1000, "");
        minDamageTaken = d(a, "minDamageTakenFactor", 0.02, 0.0001, 1, "");
        skullSpeedBase = d(a, "skullSpeedBase", 0.6, 0.05, 10, "");
        skullSpeedGrowth = d(a, "skullSpeedGrowth", 0.05, 0, 100, "");
        maxSkullSpeed = d(a, "maxSkullSpeed", 3.0, 0.1, 6, "");
        rangeBase = d(a, "attackRangeBase", 28, 8, 512, "");
        rangeGrowth = d(a, "attackRangeGrowth", 0.12, 0, 100, "");
        maxRange = d(a, "maxAttackRange", 256, 16, 512, "");
        explBase = d(a, "explosionBase", 1.0, 0, 20, "");
        explGrowth = d(a, "explosionGrowth", 0.12, 0, 100, "");
        maxExplosion = d(a, "maxExplosion", 8, 0, 30, "Forca maxima de explosao de um projetil.");
        regenBase = d(a, "regenPerSecondBase", 2, 0, 1e9, "");
        regenGrowth = d(a, "regenGrowth", 0.5, 0, 1000, "");
        maxRegen = d(a, "maxRegenPerSecond", 1e9, 0, 1e15, "");
        blocksBase = d(a, "blocksPerTickBase", 2, 0.1, 1000, "");
        blocksGrowth = d(a, "blocksPerTickGrowth", 0.2, 0, 1000, "");
        reachBase = d(a, "reachBase", 8, 2, 200, "Alcance dos tentaculos.");
        reachPerScale = d(a, "reachPerScale", 5, 0, 100, "");
        maxReach = d(a, "maxReach", 96, 4, 256, "");
        maxSearchRadius = d(a, "maxSearchRadius", 96, 16, 256, "Raio de busca por recursos.");
        speedBase = d(a, "speedBase", 0.25, 0.01, 5, "Velocidade de voo (blocos/tick).");
        speedGrowth = d(a, "speedGrowth", 0.04, 0, 100, "");
        maxSpeed = d(a, "maxSpeed", 1.2, 0.05, 5, "");
        maxBarrageShots = i(a, "maxBarrageShots", 16, 1, 64, "");

        String s = "absorption";
        maxBlocksPerTick = i(s, "maxBlocksPerTick", 24, 1, 512, "Limite DURO de blocos absorvidos por tick, por Storm.");
        samplesPerTick = i(s, "samplesPerTick", 48, 1, 1024, "Posicoes amostradas por tick para achar blocos.");
        queueCapacity = i(s, "queueCapacity", 4096, 64, 100000, "Tamanho maximo da fila de blocos a absorver.");
        scanInterval = i(s, "scanIntervalTicks", 40, 5, 1200, "Intervalo da busca de novas areas para se alimentar.");
        scanSamples = i(s, "scanSamples", 200, 10, 5000, "Amostras por busca de area.");
        invalidCacheSize = i(s, "invalidCacheSize", 8192, 128, 1000000, "Cache de posicoes ja rejeitadas.");
        maxVisualBlocksPerTick = i(s, "maxVisualBlocksPerTick", 6, 0, 64, "Blocos animados (pacotes) por tick.");
        fastAbsorbDuration = i(s, "fastAbsorbDurationTicks", 200, 20, 6000, "");
        fastAbsorbMultiplier = d(s, "fastAbsorbMultiplier", 3.0, 1, 20, "");
        baseBlockMass = d(s, "baseBlockMass", 1.0, 0.001, 1e6, "massa = base * (0.5 + dureza do bloco), salvo override.");
        absorbTileEntities = b(s, "absorbTileEntities", false, "Absorver baus, fornalhas etc. (perderia os itens). Padrao: nao.");
        absorbLiquids = b(s, "absorbLiquids", false, "");
        protectedBlocks = set(cfg.getStringList("protectedBlocks", s, new String[] {
                "minecraft:bedrock", "minecraft:command_block", "minecraft:end_portal", "minecraft:end_portal_frame",
                "minecraft:portal", "minecraft:barrier", "minecraft:mob_spawner" },
                "Blocos que nunca sao absorvidos."));
        blockBlacklist = set(cfg.getStringList("blockBlacklist", s, new String[0], "Blocos extras proibidos."));
        blockWhitelist = set(cfg.getStringList("blockWhitelist", s, new String[0], "Se nao vazio, SOMENTE estes blocos podem ser absorvidos."));
        massOverrides = map(cfg.getStringList("massOverrides", s, new String[] {
                "minecraft:obsidian=60", "minecraft:diamond_block=40", "minecraft:iron_block=30", "minecraft:gold_block=30" },
                "Massa por bloco: id=massa"));

        String k = "kills";
        killProgressPerHealth = d(k, "progressPerMaxHealth", 1.0, 0, 1000, "Progresso padrao = vida maxima da vitima * este valor.");
        playerKillMultiplier = d(k, "playerMultiplier", 3.0, 0, 1000, "");
        maxProgressPerKill = d(k, "maxProgressPerKill", 5000, 1, 1e12, "");
        killTable = map(cfg.getStringList("killTable", k, new String[] {
                "Zombie=20", "Creeper=25", "Skeleton=20", "Villager=30", "EnderDragon=2000", "WitherBoss=1500" },
                "Progresso por criatura: NomeDaEntidade=progresso (sobrescreve o calculo por vida)."));
        ignoredEntities = set(cfg.getStringList("ignoredEntities", k, new String[] { "ArmorStand" }, "Entidades que o Storm nunca ataca."));
        targetScanInterval = i(k, "targetScanIntervalTicks", 20, 5, 200, "");
        normalRange = d(k, "rangeBeforeLongRange", 40, 8, 256, "Alcance maximo antes de desbloquear 'ataques de longo alcance'.");

        String h = "abilities";
        unlockBarrage = d(h, "unlockPowerBarrage", 1.5, 0, 1000, "Poder evolutivo minimo para desbloquear.");
        unlockTentacle = d(h, "unlockPowerTentacleStrike", 2.0, 0, 1000, "");
        unlockFastAbsorb = d(h, "unlockPowerFastAbsorb", 2.5, 0, 1000, "");
        unlockBombard = d(h, "unlockPowerBombard", 3.0, 0, 1000, "");
        unlockLongRange = d(h, "unlockPowerLongRange", 5.0, 0, 1000, "");
        unlockPulse = d(h, "unlockPowerPulse", 6.0, 0, 1000, "");
        pulseMaxRadius = d(h, "pulseMaxRadius", 96, 8, 256, "");
        cdBasic = i(h, "cooldownBasic", 25, 2, 6000, "Recargas em ticks.");
        cdBarrage = i(h, "cooldownBarrage", 160, 2, 6000, "");
        cdBombard = i(h, "cooldownBombard", 400, 2, 12000, "");
        cdTentacle = i(h, "cooldownTentacleStrike", 50, 2, 6000, "");
        cdFastAbsorb = i(h, "cooldownFastAbsorb", 800, 2, 12000, "");
        cdPulse = i(h, "cooldownPulse", 1200, 2, 24000, "");
        cdLongRange = i(h, "cooldownLongRange", 240, 2, 12000, "");
        maxActiveSkulls = i(h, "maxActiveSkulls", 40, 1, 500, "Projeteis simultaneos por Storm.");
        maxSkullsPerTick = i(h, "maxSkullsPerTick", 4, 1, 32, "");
        maxExplosionsPerTick = i(h, "maxExplosionsPerTick", 4, 1, 32, "");
        maxAbilityTargets = i(h, "maxAbilityTargets", 24, 1, 256, "Alvos maximos por habilidade de area.");
        bombardMax = i(h, "maxBombardSkulls", 24, 1, 128, "");

        maxParticlesPerTick = i("client", "maxParticlesPerTick", 6, 0, 64, "Particulas ambiente por tick por Storm.");

        if (cfg.hasChanged()) cfg.save();
    }
}
