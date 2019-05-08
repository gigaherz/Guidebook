package gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.HoverContext;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.util.Size;

import java.util.List;

public class VisualPanel extends VisualElement
{
    public final List<VisualElement> children = Lists.newArrayList();

    public VisualPanel(Size size, int positionMode, float baseline, int verticalAlign)
    {
        super(size, positionMode, baseline, verticalAlign);
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        super.draw(nav);
        children.forEach(e -> e.draw(nav));
    }

    private VisualElement lastMouseOver = null;

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext)
    {
        double x = hoverContext.mouseScaledX;
        double y = hoverContext.mouseScaledY;
        VisualElement newOver = null;
        for(VisualElement child : children)
        {
            if (child.wantsHover()
                    && x >= child.position.x
                    && y >= child.position.y
                    && (x - child.position.x) < child.size.width
                    && (y - child.position.y) < child.size.height)
            {
                newOver = child;
                break;
            }
        }

        if (lastMouseOver != null && lastMouseOver != newOver)
        {
            lastMouseOver.mouseOut(nav, hoverContext);
        }

        if (newOver != null)
        {
            newOver.mouseOver(nav, hoverContext);
        }

        lastMouseOver = newOver;
    }

    @Override
    public void mouseOut(IBookGraphics nav, HoverContext hoverContext)
    {
        if (lastMouseOver != null)
        {
            lastMouseOver.mouseOut(nav, hoverContext);
            lastMouseOver = null;
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        super.click(nav);
        children.forEach(e -> e.click(nav));
    }

    @Override
    public boolean wantsHover()
    {
        return children.stream().anyMatch(e -> e.wantsHover());
    }
}
