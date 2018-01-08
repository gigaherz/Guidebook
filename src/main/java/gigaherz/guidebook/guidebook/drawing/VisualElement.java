package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.item.ItemStack;

public abstract class VisualElement extends Rect
{
    // Only position modes 1 and 2 are valid here, mode 0 will have been handled by reflow
    public int positionMode = 1;

    public VisualElement(Size size)
    {
        this.size = size;
    }

    public abstract void draw(IBookGraphics nav);

    public void mouseOver(IBookGraphics nav, int x, int y)
    {
    }

    public void mouseOut(IBookGraphics nav, int x, int y)
    {
    }

    public void click(IBookGraphics nav)
    {
    }

    public boolean wantsHover()
    {
        return false;
    }
}
