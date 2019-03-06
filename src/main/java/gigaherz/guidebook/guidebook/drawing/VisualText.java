package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.LinkContext;
import gigaherz.guidebook.guidebook.util.LinkHelper;
import gigaherz.guidebook.guidebook.util.Size;

public class VisualText extends VisualElement implements LinkHelper.ILinkable
{
    public String text;
    public int color;
    public float scale;

    public LinkContext linkContext = null;

    public VisualText(String text, Size size, int positionMode, float baseline, int verticalAlign, float scale)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.text = text;
        this.scale = scale;
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        if (linkContext != null)
            nav.addString(position.x, position.y, text, linkContext.isHovering ? linkContext.colorHover : color, scale);
        else
            nav.addString(position.x, position.y, text, color, scale);
    }

    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public boolean wantsHover()
    {
        return linkContext != null;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        linkContext.isHovering = true;
    }

    @Override
    public void mouseOut(IBookGraphics nav, int x, int y)
    {
        linkContext.isHovering = false;
    }

    @Override
    public void click(IBookGraphics nav)
    {
        LinkHelper.click(nav, linkContext);
    }

    @Override
    public void setLinkContext(LinkContext ctx)
    {
        linkContext = ctx;
    }
}
