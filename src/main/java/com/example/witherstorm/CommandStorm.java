package com.example.witherstorm;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.example.witherstorm.entity.EntityWitherStorm;

/** Comando APENAS para testes (op). A evolucao normal e 100% automatica. */
public class CommandStorm extends CommandBase {
    @Override
    public String getCommandName() {
        return "witherstorm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/witherstorm <spawn | info | addmass <n> | addprogress <n>>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static void say(ICommandSender s, String m) {
        s.addChatMessage(new ChatComponentText(m));
    }

    private EntityWitherStorm nearest(World w, BlockPos p) {
        AxisAlignedBB box = new AxisAlignedBB(p.getX() - 300, 0, p.getZ() - 300, p.getX() + 300, 256, p.getZ() + 300);
        List<EntityWitherStorm> l = w.getEntitiesWithinAABB(EntityWitherStorm.class, box);
        EntityWitherStorm best = null;
        double bd = Double.MAX_VALUE;
        for (EntityWitherStorm e : l) {
            double d = e.getDistanceSq(p.getX(), p.getY(), p.getZ());
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new WrongUsageException(getCommandUsage(sender));
        World w = sender.getEntityWorld();
        BlockPos p = sender.getPosition();
        String sub = args[0];
        if (sub.equals("spawn")) {
            EntityWitherStorm s = new EntityWitherStorm(w);
            s.setLocationAndAngles(p.getX() + 0.5, p.getY() + 10, p.getZ() + 0.5, 0, 0);
            w.spawnEntityInWorld(s);
            say(sender, "Wither Storm criado.");
            return;
        }
        EntityWitherStorm s = nearest(w, p);
        if (s == null) {
            say(sender, "Nenhum Wither Storm num raio de 300 blocos.");
            return;
        }
        if (sub.equals("info")) {
            say(sender, String.format("massa=%.1f progresso=%.1f blocos=%d mortes=%d", s.evo.mass, s.evo.progress, s.evo.blocksAbsorbed, s.evo.kills));
            say(sender, String.format("escala=%.2f estagio=%d poder=%.2f vidaEfetiva=%.0f alcance=%.0f tentaculos=%.0f", s.stats.scale, s.stats.stage,
                    s.stats.power, s.stats.effHp, s.stats.attackRange, s.stats.tentacleReach));
            return;
        }
        if ((sub.equals("addmass") || sub.equals("addprogress")) && args.length >= 2) {
            double v;
            try {
                v = Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) throw new WrongUsageException(getCommandUsage(sender));
            if (sub.equals("addmass")) s.evo.addMass(v);
            else s.evo.progress += v;
            s.evo.dirty = true;
            s.refreshDerived(false);
            say(sender, "ok");
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }
}
