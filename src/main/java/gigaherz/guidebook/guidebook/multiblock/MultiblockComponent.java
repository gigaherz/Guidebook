package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * @author joazlazer
 *
 * Base class for a renderable object within a multiblock structure
 */
public abstract class MultiblockComponent {
    /**
     * The texture location of the hover overlay texture
     */
    protected static final ResourceLocation HOVER_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_hover.png");

    /**
     * Renders the component at the specific position and at the specific scale
     * Note: Implementations are responsible for the matrix transformations specified via the parameters (in order to support flexibility)
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     * @param scale Current scale to render at (to support expanding/collapsing)
     * @return A bounding box for mouse ray collision for tooltip rendering
     */
    public abstract AxisAlignedBB render(float x, float y, float z, float scale);

    /**
     * Renders the highlight for the component at the specific position and at the specific scale
     * Note: Implementations are responsible for the matrix transformations specified via the parameters (in order to support flexibility)
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     */
    public abstract void renderHighlight(float x, float y, float z, float scale);

    /**
     * Gets the tooltip of the component to draw when hovered over
     * @return A formatted String to render when hovered
     */
    public abstract String getTooltip();
}
