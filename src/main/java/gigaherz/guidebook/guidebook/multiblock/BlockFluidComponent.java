package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import javax.vecmath.Vector3f;
import java.awt.Color;

/**
 * @author joazlazer
 * <p>
 * Extends BlockComponent and supports the rendering of both vanilla and Forge fluids within a GUI multiblock structure. Gets rendered (if transparent) after every other block to avoid render pass artifacts.
 */
public class BlockFluidComponent extends BlockComponent
{
    private Fluid fluid;

    BlockFluidComponent(IBlockState stateIn, IBlockAccess blockAccess, BlockPos position)
    {
        super(stateIn, blockAccess, position);
        if (stateIn.getBlock() instanceof BlockLiquid)
        {
            if (stateIn.getBlock() == Blocks.WATER || stateIn.getBlock() == Blocks.FLOWING_WATER)
            {
                fluid = FluidRegistry.WATER;
            }
            else
            {
                fluid = FluidRegistry.LAVA;
            }
        }
        else if (stateIn.getBlock() instanceof BlockFluidBase)
        {
            fluid = ((BlockFluidBase) stateIn.getBlock()).getFluid();
        }
        else
        {
            GuidebookMod.logger.warn(String.format("Invalid block fluid component part of multiblock structure: '%s' is not an instanceof BlockLiquid or BlockFluidBase", stateIn.getBlock().getRegistryName()));
        }
    }

    /**
     * Gets the bounding box of the specified block state
     *
     * @param stateIn     The IBlockState of the object
     * @param blockAccess Access to the block's surrounding blocks
     * @param position    The block's position in the 'world'
     * @return A cached bounding box
     */
    @Override
    protected AxisAlignedBB getBounds(IBlockState stateIn, IBlockAccess blockAccess, BlockPos position)
    {
        return new AxisAlignedBB(0d, 0d, 0d, 1d, 0.8125d, 1d);
    }

    /**
     * Renders the component at the specific position and at the specific scale
     * Note: Implementations are responsible for performing the matrix transformations specified via the parameters (in order to support flexibility)
     *
     * @param x     X location in the structure
     * @param y     Y location in the structure
     * @param z     Z location in the structure
     * @param scale Current scale to render at (to support expanding/collapsing)
     * @return A bounding box for mouse ray collision for tooltip rendering
     */
    @Override
    public AxisAlignedBB render(float x, float y, float z, float scale)
    {
        GlStateManager.pushMatrix();
        {
            GlStateManager.disableLighting();
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);

            final float offset = 0.75f * (1f - MathHelper.clamp(scale, 0f, 1f));
            GlStateManager.translate(offset, 0f, offset);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBuffer();
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            TextureAtlasSprite still = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(fluid.getStill().toString());
            TextureAtlasSprite flowing = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(fluid.getFlowing().toString());
            Color color = new Color(fluid.getColor());

            Vector3f location = new Vector3f(0f, 0f, 0f);
            Vector3f dimensions = new Vector3f(1f, 0.8125f, 1f);  // Fluid block height
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, still, EnumFacing.DOWN, color, false);
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, flowing, EnumFacing.NORTH, color, true);
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, flowing, EnumFacing.EAST, color, true);
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, flowing, EnumFacing.SOUTH, color, true);
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, flowing, EnumFacing.WEST, color, true);
            MultiblockStructure.drawTexturedQuad(location, dimensions, renderer, still, EnumFacing.UP, color, false);
            GlStateManager.enableLighting();
        }
        GlStateManager.popMatrix();
        return cachedBounds;
    }

    /**
     * Gets the tooltip of the component to draw when hovered over
     *
     * @return A formatted String to render when hovered
     */
    @Override
    public String getTooltip()
    {
        return String.format("Fluid: %s", fluid.getName());
    }

    /**
     * A factory to produce BlockFluidComponents for blocks that extend either BlockLiquid or BlockFluidBase
     */
    public static class Factory extends BlockComponent.Factory
    {
        Factory()
        {
            this.setRegistryName(GuidebookMod.MODID, "blockFluid");
        }

        /**
         * Gets the class mappings that the factory will be used with
         *
         * @return An array of Class objects that each should extend Block
         */
        @Override
        public Class<?>[] getMappings()
        {
            return new Class<?>[]{BlockLiquid.class, BlockFluidBase.class};
        }

        /**
         * Initializes a new instance of the factor's target type
         *
         * @param blockState  The block's block state
         * @param blockAccess Access to the block's neighbors
         * @param position    The block's position in the multiblock
         * @return The custom BlockComponent implementation
         */
        @Override
        public BlockFluidComponent create(IBlockState blockState, IBlockAccess blockAccess, BlockPos position)
        {
            return new BlockFluidComponent(blockState, blockAccess, position);
        }
    }
}