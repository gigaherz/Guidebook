package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import javax.vecmath.Point2d;
import javax.vecmath.Vector3f;
import java.awt.*;

/**
 * @author joazlazer
 * <p>
 * Base class for a renderable object within a multiblock structure
 */
public abstract class MultiblockComponent
{
    /**
     * The texture location of the hover overlay texture
     */
    @SuppressWarnings("WeakerAccess")
    protected static final ResourceLocation HOVER_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_hover.png");

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
    public abstract AxisAlignedBB render(float x, float y, float z, float scale);

    /**
     * Renders the highlight for the component at the specific position and at the specific scale
     * Note: Implementations are responsible for performing the matrix transformations specified via the parameters (in order to support flexibility)
     *
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     */
    public abstract void renderHighlight(float x, float y, float z, float scale);

    /**
     * Gets the tooltip of the component to draw when hovered over
     *
     * @return A formatted String to render when hovered
     */
    public abstract String getTooltip();

    /**
     * Whether the current multiblock component should be ordered and rendered in another pass
     *
     * @return If the component has translucent features (and needs to support alpha blending)
     */
    public abstract boolean isTranslucent();

    /**
     * A utility method to draw a highlight box around the component
     * Note: Implementations are responsible for performing the matrix transformations to render the box at (in order to support flexibility)
     *
     * @param box The bounding box to render each quad around
     */
    @SuppressWarnings("WeakerAccess")
    protected void renderHighlightBox(AxisAlignedBB box)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        Vector3f highlightLocation = new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ);
        Vector3f highlightDimensions = new Vector3f((float) (box.maxX - box.minX), (float) (box.maxY - box.minY), (float) (box.maxZ - box.minZ));
        Point2d UV = new Point2d(0d, 0d);
        Point2d WH = new Point2d(1d, 1d);
        Color color = new Color(1.0f, 1.0f, 1.0f, 0.15f);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.DOWN, color);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.NORTH, color);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.EAST, color);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.SOUTH, color);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.WEST, color);
        MultiblockStructure.drawTexturedQuad(highlightLocation, highlightDimensions, renderer, HOVER_TEXTURE, UV, WH, EnumFacing.UP, color);
        GlStateManager.enableLighting();
    }
}
