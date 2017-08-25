package gigaherz.guidebook.guidebook;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Rectangle;

import java.util.List;

public interface IBookGraphics
{
    void setScalingFactor();

    float getScalingFactor();

    boolean canGoBack();

    boolean canGoNextPage();

    boolean canGoPrevPage();

    boolean canGoNextChapter();

    boolean canGoPrevChapter();

    void navigateTo(PageRef target);

    void nextPage();

    void prevPage();

    void nextChapter();

    void prevChapter();

    void navigateBack();

    int addStringWrapping(int left, int top, String s, int color, int align);

    boolean mouseClicked(int mouseButton);

    void mouseHover(int mouseX, int mouseY);

    void drawCurrentPages(float partialTicks);

    BookDocument getBook();

    int getPageWidth();

    int getPageHeight();

    void drawItemStack(int left, int top, int z, ItemStack stack, int color, float scale);

    void drawHoverText(int left, int top, List<String> text);

    void drawImage(ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale);

    Rectangle getStringBounds(String text, int left, int top);

    void drawTooltip(ItemStack stack, int x, int y);

    Object owner();

    int getMouseX();

    int getMouseY();

    void updateCurrentPageElements();

    float getPartialTicks();
}
