package dev.gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.util.Size;

import java.util.List;

public class VisualPanel extends VisualElement
{
    public final List<VisualElement> children = Lists.newArrayList();

    public VisualPanel(Size size, Element.Position positionMode, float baseline, Element.VerticalAlignment verticalAlign)
    {
        super(size, positionMode, baseline, verticalAlign);
    }

    @Override
    public void draw(IBookGraphics nav, PoseStack matrixStack)
    {
        super.draw(nav, matrixStack);
        children.forEach(e -> e.draw(nav, matrixStack));
    }

    private VisualElement lastMouseOver = null;

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, PoseStack matrixStack)
    {
        double x = hoverContext.mouseScaledX;
        double y = hoverContext.mouseScaledY;
        VisualElement newOver = null;
        for (VisualElement child : children)
        {
            if (child.wantsHover() && child.contains(x, y))
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
            newOver.mouseOver(nav, hoverContext, matrixStack);
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
        return children.stream().anyMatch(VisualElement::wantsHover);
    }
}
