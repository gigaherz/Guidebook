package dev.gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.client.BookRendering;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;

public abstract class VisualElement extends Rect
{
    public static final VisualElement EMPTY = new VisualElement(new Size(), 0, 0, 0)
    {
    };
    ;

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

    public void draw(IBookGraphics nav, PoseStack matrixStack)
    {
        if (BookRendering.DEBUG_DRAW_BOUNDS)
        {
            GuiComponent.fill(matrixStack, this.position.x, this.position.y, this.position.x + this.size.width, this.position.y + this.size.height, 0x3f000000);
        }
    }

    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, PoseStack matrixStack)
    {
    }

    public void mouseOut(IBookGraphics nav, HoverContext hoverContext)
    {
    }

    public void click(IBookGraphics nav)
    {
    }

    public boolean wantsHover()
    {
        return false;
    }

    public FormattedText getText()
    {
        return Component.literal("");
    }
}
