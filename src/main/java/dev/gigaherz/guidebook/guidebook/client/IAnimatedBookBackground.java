package dev.gigaherz.guidebook.guidebook.client;

import net.minecraft.client.gui.GuiGraphics;

public interface IAnimatedBookBackground
{
    void startClosing();

    boolean isFullyOpen();

    boolean update();

    void draw(GuiGraphics graphics, float partialTicks, int bookHeight, float backgroundScale);
}
