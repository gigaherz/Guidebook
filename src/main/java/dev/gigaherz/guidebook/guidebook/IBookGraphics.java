package dev.gigaherz.guidebook.guidebook;

import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public interface IBookGraphics
{
    boolean refreshScalingFactor();

    double getScalingFactor();

    boolean canGoBack();

    boolean canGoNextPage();

    boolean canGoPrevPage();

    boolean canGoNextChapter();

    boolean canGoPrevChapter();

    void navigateTo(SectionRef target);

    void nextPage();

    void prevPage();

    void nextChapter();

    void prevChapter();

    void navigateHome();

    void navigateBack();

    int addString(GuiGraphics graphics, int left, int top, Component s, int color, float scale);

    boolean mouseClicked(int mouseX, int mouseY, int mouseButton);

    boolean mouseHover(GuiGraphics graphics, int mouseX, int mouseY);

    void drawCurrentPages(GuiGraphics graphics);

    BookDocument getBook();

    void drawItemStack(GuiGraphics graphics, int left, int top, int z, ItemStack stack, int color, float scale);

    void drawImage(GuiGraphics graphics, ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale);

    Font getFont();

    Object owner();

    Size measure(FormattedText text);

    List<VisualElement> measure(FormattedText text, int width, int firstLineWidth, float scale, int position, float baseline, int verticalAlignment);

    int getActualBookHeight();

    int getActualBookWidth();

    void resetRendering(boolean contentsChanged);

    Level getWorld();
}
