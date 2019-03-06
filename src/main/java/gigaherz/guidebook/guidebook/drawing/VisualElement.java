package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.util.Rect;
import gigaherz.guidebook.guidebook.util.Size;

public abstract class VisualElement extends Rect
{
    // Only position modes 1 and 2 are valid here, mode 0 will have been handled by reflow
    public int positionMode = 1;

    public int verticalAlign = 1;

    public float baseline = 0;

    public VisualElement(Size size, int positionMode, float baseline, int verticalAlign)
    {
        this.size = size;
        this.positionMode = positionMode;
        this.baseline = baseline;
        this.verticalAlign = verticalAlign;
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

    public String getText()
    {
        return "";
    }
}
