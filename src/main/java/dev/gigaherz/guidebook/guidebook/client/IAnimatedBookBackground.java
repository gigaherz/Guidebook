package dev.gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public interface IAnimatedBookBackground
{
    void startClosing();

    boolean isFullyOpen();

    boolean update();

    void draw(GuiGraphics graphics, float partialTicks, int bookHeight, float backgroundScale);
}
