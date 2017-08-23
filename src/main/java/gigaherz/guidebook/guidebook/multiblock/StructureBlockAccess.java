package gigaherz.guidebook.guidebook.multiblock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;

@SuppressWarnings({"NullableProblems", "WeakerAccess"})
public class StructureBlockAccess implements IBlockAccess
{
    private BlockComponent[][][] blockComponents;

    public StructureBlockAccess(BlockPos bounds)
    {
        blockComponents = new BlockComponent[bounds.getX()][bounds.getY()][bounds.getZ()];
    }

    public BlockComponent[][][] getBlockComponents()
    {
        return blockComponents;
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos)
    {
        return null; // Do not store tile entities
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue)
    {
        // full brightness always
        return 15 << 20 | 15 << 4;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos)
    {
        if (pos.getX() < blockComponents.length && pos.getX() > -1)
        {
            if (pos.getY() < blockComponents[pos.getX()].length && pos.getY() > -1)
            {
                if (pos.getZ() < blockComponents[pos.getX()][pos.getY()].length && pos.getZ() > -1)
                {
                    return blockComponents[pos.getX()][pos.getY()][pos.getZ()] != null ? blockComponents[pos.getX()][pos.getY()][pos.getZ()].blockState : Blocks.AIR.getDefaultState();
                }
            }
        }
        return Blocks.AIR.getDefaultState(); // Return air by default
    }

    @Override
    public boolean isAirBlock(BlockPos pos)
    {
        return getBlockState(pos).getBlock() == Blocks.AIR;
    }

    @Override
    public Biome getBiome(BlockPos pos)
    {
        return null; // Do not accept biome storage
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction)
    {
        return 0; // Disregard redstone
    }

    @Override
    public WorldType getWorldType()
    {
        return null; // Not a real world
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default)
    {
        return false;
    }
}
