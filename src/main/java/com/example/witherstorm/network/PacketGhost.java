package com.example.witherstorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.example.witherstorm.WitherStormMod;

/** Servidor -> cliente: posicao e aparencia do Storm (usado quando a entidade real nao esta no cliente). */
public class PacketGhost implements IMessage {
    public int entityId, stage, flags;
    public double x, y, z;
    public float yaw, scale;

    public PacketGhost() {}

    public PacketGhost(int id, double x, double y, double z, float yaw, float scale, int stage, int flags) {
        this.entityId = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.scale = scale;
        this.stage = stage;
        this.flags = flags;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        entityId = b.readInt();
        x = b.readDouble();
        y = b.readDouble();
        z = b.readDouble();
        yaw = b.readFloat();
        scale = b.readFloat();
        stage = b.readInt();
        flags = b.readByte();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(entityId);
        b.writeDouble(x);
        b.writeDouble(y);
        b.writeDouble(z);
        b.writeFloat(yaw);
        b.writeFloat(scale);
        b.writeInt(stage);
        b.writeByte(flags);
    }

    public static class Handler implements IMessageHandler<PacketGhost, IMessage> {
        @Override
        public IMessage onMessage(PacketGhost m, MessageContext ctx) {
            WitherStormMod.proxy.handleGhost(m.entityId, m.x, m.y, m.z, m.yaw, m.scale, m.stage, m.flags);
            return null;
        }
    }
}
