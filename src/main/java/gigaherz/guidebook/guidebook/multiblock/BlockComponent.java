package gigaherz.guidebook.guidebook.multiblock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import java.util.List;

/**
 * @author joazlazer
 *
 * MultiblockComponent that supports the rendering of the baked model corresponding to the given blockstate
 */
public class BlockComponent extends MultiblockComponent {
    /** The type of block in this particular spot in the structure. */
    private final IBlockState blockState;
    private final IBakedModel bakedModel;

    BlockComponent(IBlockState stateIn) {
        this.blockState = stateIn;
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        bakedModel = dispatcher.getModelForState(this.blockState);
    }

    IBlockState getBlockState() {
        return blockState;
    }

    @Override
    public void render(float x, float y, float z, float scale) {
        GlStateManager.pushMatrix(); {
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(7, DefaultVertexFormats.ITEM);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, enumfacing, 0L));
            }

            this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, null, 0L));
            tessellator.draw();
        } GlStateManager.popMatrix();
    }

    private void renderQuads(BufferBuilder bufferBuilder, List<BakedQuad> quads) {
        for (BakedQuad bakedquad : quads) {
            bufferBuilder.addVertexData(bakedquad.getVertexData());
        }
    }
}