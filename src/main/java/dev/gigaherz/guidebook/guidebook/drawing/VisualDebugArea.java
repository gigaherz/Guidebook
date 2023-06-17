package dev.gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.client.BookRendering;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    public void draw(IBookGraphics nav, PoseStack matrixStack)
    {
        super.draw(nav, matrixStack);
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, PoseStack matrixStack)
    {
        if (SHOW_DEBUG)
        {
            nav.drawTooltip(matrixStack, text, hoverContext.mouseX, hoverContext.mouseY);
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
