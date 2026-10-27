package com.example.witherstorm.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static SimpleNetworkWrapper CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("witherstorm");
        CHANNEL.registerMessage(PacketAbsorb.Handler.class, PacketAbsorb.class, 0, Side.CLIENT);
    }
}
