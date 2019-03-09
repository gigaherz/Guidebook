package gigaherz.guidebook.guidebook;

import gigaherz.guidebook.guidebook.util.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

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

    int addString(int left, int top, String s, int color, float scale);

    boolean mouseClicked(int mouseButton);

    boolean mouseHover(int mouseX, int mouseY);

    void drawCurrentPages();

    BookDocument getBook();

    void drawItemStack(int left, int top, int z, ItemStack stack, int color, float scale);

    void drawImage(ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale);

    void drawTooltip(ItemStack stack, int x, int y);

    Object owner();

    Size measure(String text);

    List<VisualElement> measure(String text, int width, int firstLineWidth, float scale, int position, float baseline, int verticalAlignment);

    int getActualBookHeight();

    int getActualBookWidth();

    void resetRendering(boolean contentsChanged);
}
