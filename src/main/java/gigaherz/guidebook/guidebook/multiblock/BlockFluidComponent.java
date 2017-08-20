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
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import javax.vecmath.Vector3f;
import java.awt.Color;

/**
 * @author joazlazer
 *
 * Extends BlockComponent and supports the rendering of both vanilla and Forge fluids within a GUI multiblock structure. Gets rendered after every other block to avoid render pass artifacts.
 */
public class BlockFluidComponent extends BlockComponent {
    private Fluid fluid;
    BlockFluidComponent(IBlockState stateIn) {
        super(stateIn);
        if(stateIn.getBlock() instanceof BlockLiquid) {
            if(stateIn.getBlock() == Blocks.WATER || stateIn.getBlock() == Blocks.FLOWING_WATER) {
                fluid = FluidRegistry.WATER;
            } else {
                fluid = FluidRegistry.LAVA;
            }
        } else if(stateIn.getBlock() instanceof BlockFluidBase){
            fluid = ((BlockFluidBase)stateIn.getBlock()).getFluid();
        } else {
            GuidebookMod.logger.warn(String.format("Invalid block fluid component part of multiblock structure: '%s' is not an instanceof BlockLiquid or BlockFluidBase", stateIn.getBlock().getRegistryName()));
        }
    }

    @Override
    public AxisAlignedBB render(float x, float y, float z, float scale) {
        GlStateManager.pushMatrix(); {
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
        } GlStateManager.popMatrix();
        return new AxisAlignedBB(x, y, z, x + 1f, y + 0.8125f, z + 1f);
    }

    @Override
    public String getTooltip() {
        return String.format("Fluid:%s, BlockState:%s", fluid.toString(), blockState.toString());
    }

    public static class Factory extends BlockComponent.Factory {
        public Factory() {
            this.setRegistryName(GuidebookMod.MODID, "blockFluid");
        }

        @Override
        public Class<?>[] getMappings() {
            return new Class<?>[] { BlockLiquid.class, BlockFluidBase.class };
        }

        @Override
        public BlockFluidComponent create(IBlockState blockState) {
            return new BlockFluidComponent(blockState);
        }
    }
}