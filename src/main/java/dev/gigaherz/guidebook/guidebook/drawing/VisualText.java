package dev.gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.elements.LinkContext;
import dev.gigaherz.guidebook.guidebook.util.Color;
import dev.gigaherz.guidebook.guidebook.util.LinkHelper;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class VisualText extends VisualElement implements LinkHelper.ILinkable
{
    public Component text;
    public Color color;
    public float scale;

    public LinkContext linkContext = null;

    public VisualText(Component text, Size size, Element.Position positionMode, float baseline, Element.VerticalAlignment verticalAlign, float scale)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.text = text;
        this.scale = scale;
    }

    @Override
    public void draw(IBookGraphics nav, PoseStack matrixStack)
    {
        super.draw(nav, matrixStack);
        if (linkContext != null)
            nav.addString(matrixStack, position.x(), position.y(), text, linkContext.isHovering ? linkContext.colorHover : color.argb(), scale);
        else
            nav.addString(matrixStack, position.x(), position.y(), text, color.argb(), scale);
    }

    @Override
    public FormattedText getText()
    {
        return text;
    }

    @Override
    public boolean wantsHover()
    {
        return linkContext != null;
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, PoseStack matrixStack)
    {
        linkContext.isHovering = true;
    }

    @Override
    public void mouseOut(IBookGraphics nav, HoverContext hoverContext)
    {
        linkContext.isHovering = false;
    }

    @Override
    public void click(IBookGraphics nav)
    {
        if (linkContext != null)
            LinkHelper.click(nav, linkContext);
    }

    @Override
    public void setLinkContext(LinkContext ctx)
    {
        linkContext = ctx;
    }
}
