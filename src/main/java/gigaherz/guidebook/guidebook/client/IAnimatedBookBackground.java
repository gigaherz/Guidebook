package gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.vertex.PoseStack;

public interface IAnimatedBookBackground
{
    void startClosing();

    boolean isFullyOpen();

    boolean update();

    void draw(PoseStack matrixStack, float partialTicks, int bookHeight, float backgroundScale);
}
