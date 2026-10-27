package com.example.witherstorm;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.witherstorm.config.StormConfig;
import com.example.witherstorm.entity.EntityStormSkull;
import com.example.witherstorm.entity.EntityWitherStorm;
import com.example.witherstorm.network.NetworkHandler;

@Mod(modid = WitherStormMod.MODID, name = WitherStormMod.NAME, version = WitherStormMod.VERSION, acceptedMinecraftVersions = "[1.8.9]")
public class WitherStormMod {
    public static final String MODID = "witherstorm";
    public static final String NAME = "Evolving Wither Storm";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static WitherStormMod instance;

    @SidedProxy(clientSide = "com.example.witherstorm.client.ClientProxy", serverSide = "com.example.witherstorm.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        StormConfig.load(e.getSuggestedConfigurationFile());
        EntityRegistry.registerModEntity(EntityWitherStorm.class, "WitherStorm", 0, this, 256, 2, true);
        EntityRegistry.registerModEntity(EntityStormSkull.class, "StormSkull", 1, this, 160, 1, true);
        NetworkHandler.init();
        MinecraftForge.EVENT_BUS.register(new StormEvents());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandStorm());
    }
}
