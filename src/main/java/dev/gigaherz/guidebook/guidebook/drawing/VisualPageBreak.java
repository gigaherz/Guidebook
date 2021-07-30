package dev.gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.util.Size;

public class VisualPageBreak extends VisualElement
{
    public VisualPageBreak(Size size)
    {
        super(size, 0, 0, 0);
    }

    @Override
    public void draw(IBookGraphics nav, PoseStack matrixStack)
    {
        // not a drawableelement
    }
}
