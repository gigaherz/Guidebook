package gigaherz.guidebook.guidebook.multiblock;

/**
 * @author joazlazer
 *
 * Base class for a renderable object within a multiblock structure
 */
public abstract class MultiblockComponent {
    /**
     * Renders the component at the specific position and at the specific scale
     * Note: Implementations are responsible for the glTransform and glScale that is specified via the parameters (in order to support flexibility)
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     * @param scale Current scale to render at (to support expanding/collapsing)
     */
    public abstract void render(float x, float y, float z, float scale);
}
