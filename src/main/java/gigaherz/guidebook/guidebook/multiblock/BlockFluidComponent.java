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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.lwjgl.opengl.GL11;
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
    public void render(float x, float y, float z, float scale) {
        GlStateManager.pushMatrix(); {
            GlStateManager.disableLighting();
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBuffer();
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            TextureAtlasSprite still = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(fluid.getStill().toString());
            TextureAtlasSprite flowing = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(fluid.getFlowing().toString());
            Color color = new Color(fluid.getColor());

            drawTexturedQuad(renderer, still, EnumFacing.DOWN, color, false);
            drawTexturedQuad(renderer, flowing, EnumFacing.NORTH, color, true);
            drawTexturedQuad(renderer, flowing, EnumFacing.EAST, color, true);
            drawTexturedQuad(renderer, flowing, EnumFacing.SOUTH, color, true);
            drawTexturedQuad(renderer, flowing, EnumFacing.WEST, color, true);
            drawTexturedQuad(renderer, still, EnumFacing.UP, color, false);

            GlStateManager.enableLighting();
        } GlStateManager.popMatrix();
    }

    private void drawTexturedQuad(BufferBuilder renderer, TextureAtlasSprite sprite, EnumFacing face, Color color, boolean flowing) {
        if(sprite == null) return;
        double minU, maxU, minV, maxV;
        double size = 16f;
        if(flowing) size = 8f;
        double x1 = 0d;
        double x2 = 1d;
        double y1 = 0d;
        double y2 = 0.8d;
        double z1 = 0d;
        double z2 = 1d;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        double xt1 = x1 % 1d;
        double xt2 = xt1 + 1d;
        while(xt2 > 1f) xt2 -= 1f;
        double yt1 = y1 % 1d;
        double yt2 = yt1 + 1d;
        while(yt2 > 1f) yt2 -= 1f;
        double zt1 = z1 % 1d;
        double zt2 = zt1 + 1d;
        while(zt2 > 1f) zt2 -= 1f;

        if(flowing) {
            double tmp = 1d - yt1;
            yt1 = 1d - yt2;
            yt2 = tmp;
        }

        switch(face) {
            case DOWN:
            case UP:
                minU = sprite.getInterpolatedU(xt1 * size);
                maxU = sprite.getInterpolatedU(xt2 * size);
                minV = sprite.getInterpolatedV(zt1 * size);
                maxV = sprite.getInterpolatedV(zt2 * size);
                break;
            case NORTH:
            case SOUTH:
                minU = sprite.getInterpolatedU(xt1 * size);
                maxU = sprite.getInterpolatedU(xt2 * size);
                minV = sprite.getInterpolatedV(yt1 * size);
                maxV = sprite.getInterpolatedV(yt2 * size);
                break;
            case WEST:
            case EAST:
                minU = sprite.getInterpolatedU(zt1 * size);
                maxU = sprite.getInterpolatedU(zt2 * size);
                minV = sprite.getInterpolatedV(yt1 * size);
                maxV = sprite.getInterpolatedV(yt2 * size);
                break;
            default:
                minU = sprite.getMinU();
                maxU = sprite.getMaxU();
                minV = sprite.getMinV();
                maxV = sprite.getMaxV();
        }

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        switch(face) {
            case DOWN:
                renderer.pos(x1, y1, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                break;
            case UP:
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case NORTH:
                renderer.pos(x1, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
            case SOUTH:
                renderer.pos(x1, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case WEST:
                renderer.pos(x1, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case EAST:
                renderer.pos(x2, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
        }
        Tessellator.getInstance().draw();
    }
}