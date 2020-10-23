package gigaherz.guidebook.guidebook;

import com.mojang.blaze3d.matrix.MatrixStack;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.world.World;

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

    int addString(MatrixStack matrixStack, int left, int top, ITextComponent s, int color, float scale);

    boolean mouseClicked(int mouseX, int mouseY, int mouseButton);

    boolean mouseHover(MatrixStack matrixStack, int mouseX, int mouseY);

    void drawCurrentPages(MatrixStack matrixStack);

    BookDocument getBook();

    void drawItemStack(MatrixStack matrixStack, int left, int top, int z, ItemStack stack, int color, float scale);

    void drawImage(MatrixStack matrixStack, ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale);

    void drawTooltip(MatrixStack matrixStack, ItemStack stack, int x, int y);

    Object owner();

    Size measure(ITextProperties text);

    List<VisualElement> measure(ITextProperties text, int width, int firstLineWidth, float scale, int position, float baseline, int verticalAlignment);

    int getActualBookHeight();

    int getActualBookWidth();

    void resetRendering(boolean contentsChanged);

    World getWorld();
}
