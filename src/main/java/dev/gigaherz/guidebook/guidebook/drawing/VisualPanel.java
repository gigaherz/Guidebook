package dev.gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VisualPanel extends VisualElement
{
    public final List<VisualElement> children = Lists.newArrayList();

    public VisualPanel(Size size, int positionMode, float baseline, int verticalAlign)
    {
        super(size, positionMode, baseline, verticalAlign);
    }

    @Override
    public void move(int offsetX, int offsetY)
    {
        super.move(offsetX, offsetY);
        children.forEach(e -> e.move(offsetX, offsetY));
    }

    @Override
    public void draw(IBookGraphics nav, GuiGraphics graphics)
    {
        super.draw(nav, graphics);
        children.forEach(e -> e.draw(nav, graphics));
    }

    private VisualElement lastMouseOver = null;

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, GuiGraphics graphics)
    {
        double x = hoverContext.mouseScaledX;
        double y = hoverContext.mouseScaledY;
        VisualElement newOver = null;
        for (VisualElement child : children)
        {
            if (child.wantsHover()
                    && x >= child.position.x()
                    && y >= child.position.y()
                    && (x - child.position.x()) < child.size.width()
                    && (y - child.position.y()) < child.size.height())
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
            newOver.mouseOver(nav, hoverContext, graphics);
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

    @Override
    public void updateDebugIndices(int index, int indent)
    {
        super.updateDebugIndices(index, indent);

        for(int i=0;i<children.size();i++)
        {
            children.get(i).updateDebugIndices(i,indent + 20);
        }
    }
}
