package gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.matrix.MatrixStack;
import gigaherz.guidebook.guidebook.HoverContext;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.LinkContext;
import gigaherz.guidebook.guidebook.util.LinkHelper;
import gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.util.text.ITextProperties;

public class VisualText extends VisualElement implements LinkHelper.ILinkable
{
    public ITextProperties text;
    public int color;
    public float scale;

    public LinkContext linkContext = null;

    public VisualText(ITextProperties text, Size size, int positionMode, float baseline, int verticalAlign, float scale)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.text = text;
        this.scale = scale;
    }

    @Override
    public void draw(IBookGraphics nav, MatrixStack matrixStack)
    {
        super.draw(nav, matrixStack);
        if (linkContext != null)
            nav.addString(matrixStack, position.x, position.y, text, linkContext.isHovering ? linkContext.colorHover : color, scale);
        else
            nav.addString(matrixStack, position.x, position.y, text, color, scale);
    }

    @Override
    public ITextProperties getText()
    {
        return text;
    }

    @Override
    public boolean wantsHover()
    {
        return linkContext != null;
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, MatrixStack matrixStack)
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
