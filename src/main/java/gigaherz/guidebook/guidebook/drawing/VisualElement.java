package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.item.ItemStack;

public abstract class VisualElement
{
    public Point position;
    public Size size;

    public VisualElement(Size size)
    {
        this(new Point(), size);
    }

    public VisualElement(Point point, Size size)
    {
        this.position = point;
        this.size=size;
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
