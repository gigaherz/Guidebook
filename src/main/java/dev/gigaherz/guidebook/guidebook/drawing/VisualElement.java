package dev.gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.client.BookRendering;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.concurrent.ThreadLocalRandom;

public abstract class VisualElement extends Rect
{
    // Only position modes 1 and 2 are valid here, mode 0 will have been handled by reflow
    public int positionMode;
    public int verticalAlign;
    public float baseline;

    private final int debugColor = 0x3f000000 | ThreadLocalRandom.current().nextInt(0xFFFFFF);
    public int debugIndex = 0;
    public int debugIndexIndent = 0;

    private void renderDebug(GuiGraphics graphics)
    {
        graphics.fill(this.position.x() + debugIndexIndent, this.position.y(), this.position.x() + this.size.width() + debugIndexIndent, this.position.y() + this.size.height(), debugColor);
        graphics.drawString(Minecraft.getInstance().font, Integer.toString(debugIndex),
                this.position.x() + debugIndexIndent, this.position.y(), 0xff000000 | debugColor);
        graphics.drawString(Minecraft.getInstance().font, Integer.toString(debugIndex),
                this.position.x() + this.size.width() + debugIndexIndent, this.position.y() + this.size.height(), 0xff000000 | debugColor);
        /*line(poseStack,
                this.position.x() + this.size.width() + debugIndexIndent, this.position.y() + this.size.height(),
                this.position.x() + this.size.width(), this.position.y() + this.size.height(),
                debugColor);*/
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color)
    {
        var pose = graphics.pose().last();
        var matrix = pose.pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth((float) Minecraft.getInstance().getWindow().getGuiScale());
        builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        builder.vertex(matrix, x0, y0, 0).color(color).normal(pose, 0,0,-1).endVertex();
        builder.vertex(matrix, x1, y1, 0).color(color).normal(pose, 0,0,-1).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1);
    }

    public VisualElement(Size size, int positionMode, float baseline, int verticalAlign)
    {
        this.size = size;
        this.positionMode = positionMode;
        this.baseline = baseline;
        this.verticalAlign = verticalAlign;
    }

    public void draw(IBookGraphics nav, GuiGraphics graphics)
    {
        if (BookRendering.DEBUG_DRAW_BOUNDS)
        {
            renderDebug(graphics);
        }
    }

    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, GuiGraphics graphics)
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

    public void move(int offsetX, int offsetY)
    {
        position = new Point(
                position.x() + offsetX,
                position.y() + offsetY);
    }

    public void updateDebugIndices(int index, int indent)
    {
        this.debugIndex = index;
        this.debugIndexIndent = indent;
    }
}
