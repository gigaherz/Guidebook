package gigaherz.guidebook.guidebook.multiblock;

import net.minecraft.util.math.AxisAlignedBB;
import org.w3c.dom.Node;

public class ItemComponent extends ParsableMultiblockComponent
{
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
        // TODO Everything related to this
        return null;
    }

    /**
     * Renders the highlight for the component at the specific position and at the specific scale
     * Note: Implementations are responsible for performing the matrix transformations specified via the parameters (in order to support flexibility)
     *
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     */
    @Override
    public void renderHighlight(float x, float y, float z, float scale)
    {

    }

    /**
     * Gets the tooltip of the component to draw when hovered over
     *
     * @return A formatted String to render when hovered
     */
    @Override
    public String getTooltip()
    {
        return null;
    }

    /**
     * Whether the current multiblock component should be ordered and rendered in another pass
     *
     * @return If the component has translucent features (and needs to support alpha blending)
     */
    @Override
    public boolean isTranslucent()
    {
        return false;
    }

    public static class Parser extends ParsableMultiblockComponent.Parser
    {
        /**
         * Gets the tag mapping for the current parsable multiblock component
         *
         * @return A string to compare to the multiblock's sub-nodes
         */
        @Override
        public String getBaseNodeName()
        {
            return "stack";
        }

        /**
         * Parses the node and creates a new multiblock component from the specified XML parameters
         *
         * @param baseNode The XML node at the base of this component
         * @return A new instance of a ParsableMultiblockComponent
         */
        @Override
        public ParsableMultiblockComponent parse(Node baseNode)
        {
            return null;
        }
    }
}
