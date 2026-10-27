package com.example.witherstorm.absorb;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.example.witherstorm.config.StormConfig;

/** Decide se um bloco pode ser absorvido e quanta massa ele vale. */
public final class BlockMassRegistry {
    private BlockMassRegistry() {}

    /** Retorna a massa (> 0) ou -1 se o bloco NAO pode ser absorvido. */
    public static double massOf(World w, BlockPos pos, IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.air) return -1;
        ResourceLocation rl = Block.blockRegistry.getNameForObject(b);
        String id = rl == null ? "" : rl.toString();
        if (StormConfig.protectedBlocks.contains(id)) return -1;
        if (!StormConfig.blockWhitelist.isEmpty() && !StormConfig.blockWhitelist.contains(id)) return -1;
        if (StormConfig.blockBlacklist.contains(id)) return -1;
        if (b.getMaterial().isLiquid() && !StormConfig.absorbLiquids) return -1;
        float h = b.getBlockHardness(w, pos);
        if (h < 0) return -1; // inquebravel
        if (!StormConfig.absorbTileEntities && (b instanceof ITileEntityProvider || b.hasTileEntity(state))) return -1;
        Double o = StormConfig.massOverrides.get(id);
        if (o != null) return o > 0 ? o : -1;
        return Math.max(0.05, StormConfig.baseBlockMass * (0.5 + h));
    }
}
