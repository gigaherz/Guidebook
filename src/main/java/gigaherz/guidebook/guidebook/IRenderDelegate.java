package gigaherz.guidebook.guidebook;

/**
 * @author joazlazer
 * Functional interface to support delegation of additional rendering to custom framework implementations
 */
public interface IRenderDelegate
{
    /**
     * Called each render tick when the context's object is visible
     * @param nav Access to rendering methods for GUI rendering
     * @param left The screen-space x-position to start rendering at
     * @param top The screen-space y-position to start rendering at
     */
    void render(IBookGraphics nav, int left, int top);
}
