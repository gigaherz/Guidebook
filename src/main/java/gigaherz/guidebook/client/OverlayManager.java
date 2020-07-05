package gigaherz.guidebook.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public class OverlayManager
{
    @FunctionalInterface
    public interface IDrawable
    {
        void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, float partialTicks);
    }

    public interface IOverlayRenderer extends IDrawable
    {
        /**
         * Bigger draws on top
         */
        int priority();
    }

    public class BasicOverlay implements IOverlayRenderer
    {

        private final int priority;
        private final IDrawable drawable;

        public BasicOverlay(int priority, IDrawable drawable)
        {
            this.priority = priority;
            this.drawable = drawable;
        }

        @Override
        public int priority()
        {
            return priority;
        }

        @Override
        public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, float partialTicks)
        {
            drawable.render(matrixStack, buffer, partialTicks);
        }
    }

}
