package dev.gigaherz.guidebook.guidebook.drawing;

import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class VisualDebugArea extends VisualElement
{
    public static final boolean INJECT_DEBUG = false;
    public static final boolean SHOW_DEBUG = false;

    private final Component text;

    public VisualDebugArea(Size size, int positionMode, float baseline, int verticalAlign, Component text)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.text = text;
    }

    @Override
    public void draw(IBookGraphics nav, GuiGraphics graphics)
    {
        super.draw(nav, graphics);
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, GuiGraphics graphics)
    {
        if (SHOW_DEBUG)
        {
            graphics.renderTooltip(nav.getFont(), text, hoverContext.mouseX, hoverContext.mouseY);
        }
    }

    @Override
    public void mouseOut(IBookGraphics nav, HoverContext hoverContext)
    {
    }

    @Override
    public boolean wantsHover()
    {
        return SHOW_DEBUG;
    }
}
