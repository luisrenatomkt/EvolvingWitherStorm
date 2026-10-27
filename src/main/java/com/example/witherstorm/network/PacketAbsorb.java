package com.example.witherstorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.example.witherstorm.WitherStormMod;

/** Servidor -> cliente: um bloco foi absorvido (apenas animacao; o servidor decide a massa). */
public class PacketAbsorb implements IMessage {
    public int entityId, x, y, z, stateId;

    public PacketAbsorb() {}

    public PacketAbsorb(int entityId, int x, int y, int z, int stateId) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.stateId = stateId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        stateId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(stateId);
    }

    public static class Handler implements IMessageHandler<PacketAbsorb, IMessage> {
        @Override
        public IMessage onMessage(PacketAbsorb m, MessageContext ctx) {
            WitherStormMod.proxy.handleAbsorb(m.entityId, m.x, m.y, m.z, m.stateId);
            return null;
        }
    }
}
