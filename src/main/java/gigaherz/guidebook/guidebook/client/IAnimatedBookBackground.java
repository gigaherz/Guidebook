package gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.matrix.MatrixStack;

public interface IAnimatedBookBackground
{
    void startClosing();

    boolean isFullyOpen();

    boolean update();

    void draw(MatrixStack matrixStack, float partialTicks, int bookHeight, float backgroundScale);
}
