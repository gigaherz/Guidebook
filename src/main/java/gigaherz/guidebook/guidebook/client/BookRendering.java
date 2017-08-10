package gigaherz.guidebook.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import gigaherz.guidebook.guidebook.elements.IClickablePageElement;
import gigaherz.guidebook.guidebook.elements.IContainerPageElement;
import gigaherz.guidebook.guidebook.elements.IHoverPageElement;
import gigaherz.guidebook.guidebook.elements.IPageElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;

import java.util.Collection;

public class BookRendering implements IBookGraphics
{
    public static final int DEFAULT_BOOK_WIDTH = 276;
    public static final int DEFAULT_BOOK_HEIGHT = 198;
    public static final int DEFAULT_INNER_MARGIN = 22;
    public static final int DEFAULT_OUTER_MARGIN = 10;
    public static final int DEFAULT_VERTICAL_MARGIN = 18;

    private BookDocument book;
    int scaledWidth;
    int scaledHeight;

    final Minecraft mc = Minecraft.getMinecraft();
    final GuiGuidebook gui;

    private int bookWidth;
    private int bookHeight;
    private int innerMargin;
    private int outerMargin;
    private int verticalMargin;
    private int pageWidth = bookWidth / 2 - innerMargin - outerMargin;
    private int pageHeight = bookHeight - verticalMargin;

    final java.util.Stack<PageRef> history = new java.util.Stack<>();
    private int currentChapter = 0;
    private int currentPair = 0;
    private boolean hasScale;

    private float scalingFactor;

    BookRendering(BookDocument book, GuiGuidebook gui)
    {
        this.book = book;
        this.gui = gui;
    }

    public void computeScaledResolution2(Minecraft minecraftClient, float scaleFactorCoef)
    {
        this.scaledWidth = minecraftClient.displayWidth;
        this.scaledHeight = minecraftClient.displayHeight;
        int scaleFactor = 1;
        boolean flag = minecraftClient.isUnicode();
        int i = GuidebookMod.bookGUIScale < 0 ? minecraftClient.gameSettings.guiScale : GuidebookMod.bookGUIScale;

        if (i == 0)
        {
            i = 1000;
        }

        while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240)
        {
            ++scaleFactor;
        }

        scaleFactor = MathHelper.floor(Math.max(1, scaleFactor * scaleFactorCoef));

        if (flag && scaleFactor % 2 != 0 && scaleFactor != 1)
        {
            --scaleFactor;
        }

        double scaledWidthD = (double) this.scaledWidth / (double) scaleFactor;
        double scaledHeightD = (double) this.scaledHeight / (double) scaleFactor;
        this.scaledWidth = MathHelper.ceil(scaledWidthD);
        this.scaledHeight = MathHelper.ceil(scaledHeightD);
    }

    @Override
    public void setScalingFactor()
    {
        float fontSize = book.getFontSize();

        if (MathHelper.epsilonEquals(fontSize, 1.0f))
        {
            this.scaledWidth = gui.width;
            this.scaledHeight = gui.height;

            this.hasScale = false;
            this.scalingFactor = 1.0f;

            this.bookWidth = DEFAULT_BOOK_WIDTH;
            this.bookHeight = DEFAULT_BOOK_HEIGHT;
            this.innerMargin = DEFAULT_INNER_MARGIN;
            this.outerMargin = DEFAULT_OUTER_MARGIN;
            this.verticalMargin = DEFAULT_VERTICAL_MARGIN;
        }
        else
        {
            ScaledResolution sr = new ScaledResolution(mc);
            computeScaledResolution2(mc, fontSize);

            this.hasScale = true;
            this.scalingFactor = Math.min(sr.getScaledWidth() / (float) scaledWidth, sr.getScaledHeight() / (float) scaledHeight);

            this.bookWidth = (int) (DEFAULT_BOOK_WIDTH / fontSize);
            this.bookHeight = (int) (DEFAULT_BOOK_HEIGHT / fontSize);
            this.innerMargin = (int) (DEFAULT_INNER_MARGIN / fontSize);
            this.outerMargin = (int) (DEFAULT_OUTER_MARGIN / fontSize);
            this.verticalMargin = (int) (DEFAULT_VERTICAL_MARGIN / fontSize);
        }

        this.pageWidth = this.bookWidth / 2 - this.innerMargin - this.outerMargin;
        this.pageHeight = this.bookHeight - this.verticalMargin;
    }

    @Override
    public float getScalingFactor()
    {
        return scalingFactor;
    }

    @Override
    public boolean canGoBack()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    @Override
    public boolean canGoNextPage()
    {
        return (currentPair + 1 < book.getChapter(currentChapter).pagePairs || currentChapter + 1 < book.chapterCount());
    }

    @Override
    public boolean canGoPrevPage()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    @Override
    public boolean canGoNextChapter()
    {
        return (currentChapter + 1 < book.chapterCount());
    }

    @Override
    public boolean canGoPrevChapter()
    {
        return (currentChapter > 0);
    }

    @Override
    public void navigateTo(final PageRef target)
    {
        if (!target.resolve(book))
            return;
        pushHistory();
        currentChapter = Math.max(0, Math.min(book.chapterCount() - 1, target.chapter));
        currentPair = Math.max(0, Math.min(book.getChapter(currentChapter).pagePairs - 1, target.page / 2));
    }

    @Override
    public void nextPage()
    {
        if (currentPair + 1 < book.getChapter(currentChapter).pagePairs)
        {
            pushHistory();
            currentPair++;
        }
        else if (currentChapter + 1 < book.chapterCount())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

    @Override
    public void prevPage()
    {
        if (currentPair > 0)
        {
            pushHistory();
            currentPair--;
        }
        else if (currentChapter > 0)
        {
            pushHistory();
            currentChapter--;
            currentPair = book.getChapter(currentChapter).pairCount() - 1;
        }
    }

    @Override
    public void nextChapter()
    {
        if (currentChapter + 1 < book.chapterCount())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

    @Override
    public void prevChapter()
    {
        if (currentChapter > 0)
        {
            pushHistory();
            currentPair = 0;
            currentChapter--;
        }
    }

    @Override
    public void navigateBack()
    {
        if (history.size() > 0)
        {
            PageRef target = history.pop();
            target.resolve(book);
            currentChapter = target.chapter;
            currentPair = target.page / 2;
        }
        else
        {
            currentChapter = 0;
            currentPair = 0;
        }
    }

    private void pushHistory()
    {
        history.push(new PageRef(currentChapter, currentPair * 2));
    }

    private int getSplitWidth(FontRenderer fontRenderer, String s, float scale)
    {
        int height = (int)(fontRenderer.getWordWrappedHeight(s, (int)(pageWidth / scale)) * scale);
        return height > (fontRenderer.FONT_HEIGHT * scale) ? pageWidth : (int)(fontRenderer.getStringWidth(s) * scale);
    }

    @Override
    public int addStringWrapping(int left, int top, String s, int color, int align, float scale)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        if (align == 1)
        {
            left += (pageWidth - getSplitWidth(fontRenderer, s, scale)) / 2;
        }
        else if (align == 2)
        {
            left += pageWidth - getSplitWidth(fontRenderer, s, scale);
        }

        // Does scaling need to be performed?
        if(!(MathHelper.epsilonEquals(scale, 1.0f)))
        {
            GlStateManager.pushMatrix();
            {
                GlStateManager.scale(scale, scale, 1f);
                fontRenderer.drawSplitString(s, (int)(left / scale), (int)(top / scale), (int)(pageWidth / scale), color);
            }
            GlStateManager.popMatrix();
        }
        else
        {
            fontRenderer.drawSplitString(s, left, top, pageWidth, color);
        }

        return (int)(fontRenderer.getWordWrappedHeight(s, (int)(pageWidth / scale)) * scale);
    }

    @Override
    public boolean mouseClicked(int mouseButton)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mouseX = Mouse.getX() * dw / mc.displayWidth;
        int mouseY = dh - Mouse.getY() * dh / mc.displayHeight;

        if (mouseButton == 0)
        {
            BookDocument.ChapterData ch = book.getChapter(currentChapter);
            BookDocument.PageData pg = ch.pages.get(currentPair * 2);
            if (mouseClickPage(mouseX, mouseY, pg))
                return true;

            if (currentPair * 2 + 1 < ch.pages.size())
            {
                pg = ch.pages.get(currentPair * 2 + 1);
                if (mouseClickPage(mouseX, mouseY, pg))
                    return true;
            }
        }

        return false;
    }

    private boolean mouseClickPage(int mX, int mY, BookDocument.PageData pg)
    {
        return mouseClickContainer(mX, mY, pg.elements);
    }

    private boolean mouseClickContainer(int mX, int mY, Collection<IPageElement> elements)
    {
        for (IPageElement e : elements)
        {
            if (e instanceof IClickablePageElement)
            {
                IClickablePageElement l = (IClickablePageElement) e;
                Rectangle b = l.getBounds();
                if (mX >= b.getX() && mX <= (b.getX() + b.getWidth()) &&
                        mY >= b.getY() && mY <= (b.getY() + b.getHeight()))
                {
                    l.click(this);
                    return true;
                }
            }
            else if (e instanceof IContainerPageElement)
            {
                mouseClickContainer(mX, mY, ((IContainerPageElement) e).getChildren());
            }
        }
        return false;
    }

    @Override
    public boolean mouseHover(int mouseX, int mouseY)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);
        BookDocument.PageData pg = ch.pages.get(currentPair * 2);
        if (mouseHoverPage(mouseX, mouseY, pg))
            return true;

        if (currentPair * 2 + 1 < ch.pages.size())
        {
            pg = ch.pages.get(currentPair * 2 + 1);
            if (mouseHoverPage(mouseX, mouseY, pg))
                return true;
        }

        return false;
    }

    private boolean mouseHoverPage(int mouseX, int mouseY, BookDocument.PageData pg)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mX = Mouse.getX() * dw / mc.displayWidth;
        int mY = dh - Mouse.getY() * dh / mc.displayHeight;

        return mouseHoverContainer(mouseX, mouseY, mX, mY, pg.elements);
    }

    private boolean mouseHoverContainer(int mouseX, int mouseY, int mX, int mY, Collection<IPageElement> elements)
    {
        for (IPageElement e : elements)
        {
            if (e instanceof IHoverPageElement)
            {
                IHoverPageElement l = (IHoverPageElement) e;
                Rectangle b = l.getBounds();
                if (mX >= b.getX() && mX <= (b.getX() + b.getWidth()) &&
                        mY >= b.getY() && mY <= (b.getY() + b.getHeight()))
                {
                    l.mouseOver(this, mouseX, mouseY);
                    return true;
                }
            }
            else if (e instanceof IContainerPageElement)
            {
                mouseHoverContainer(mouseX, mouseY, mX, mY, ((IContainerPageElement) e).getChildren());
            }
        }
        return false;
    }

    @Override
    public void drawCurrentPages()
    {
        int guiWidth = gui.width;
        int guiHeight = gui.height;

        if (hasScale)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scalingFactor, scalingFactor, scalingFactor);

            guiWidth = scaledWidth;
            guiHeight = scaledHeight;
        }

        int left = guiWidth / 2 - pageWidth - innerMargin;
        int right = guiWidth / 2 + innerMargin;
        int top = (guiHeight - pageHeight) / 2 - 9;
        int bottom = top + pageHeight - 3;

        drawPage(left, top, currentPair * 2);
        drawPage(right, top, currentPair * 2 + 1);

        String cnt = "" + ((book.getChapter(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (book.getTotalPairCount() * 2);
        addStringWrapping(left, bottom, cnt, 0xFF000000, 1, 1f);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private void drawPage(int left, int top, int page)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        BookDocument.PageData pg = ch.pages.get(page);

        for (IPageElement e : pg.elements)
        {
            top += e.apply(this, left, top);
        }
    }

    @Override
    public BookDocument getBook()
    {
        return book;
    }

    @Override
    public int getPageWidth()
    {
        return pageWidth;
    }

    @Override
    public int getPageHeight()
    {
        return pageHeight;
    }

    @Override
    public void drawItemStack(int left, int top, int z, ItemStack stack, int color, float scale)
    {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();

        GlStateManager.pushMatrix();
        GlStateManager.translate(left, top, z);
        GlStateManager.scale(scale, scale, scale);

        RenderHelper.enableGUIStandardItemLighting();
        gui.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();

        gui.mc.getRenderItem().renderItemOverlayIntoGUI(gui.getFontRenderer(), stack, 0, 0, null);

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    @Override
    public void drawImage(ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale)
    {
        int sw = tw != 0 ? tw : 256;
        int sh = th != 0 ? th : 256;

        if (w == 0) w = sw;
        if (h == 0) h = sh;

        ResourceLocation locExpanded = new ResourceLocation(loc.getResourceDomain(), "textures/" + loc.getResourcePath() + ".png");
        gui.getRenderEngine().bindTexture(locExpanded);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.drawScaledCustomSizeModalRect(x, y, tx, ty, w, h, (int) (w * scale), (int) (h * scale), sw, sh);
    }

    @Override
    public Rectangle getStringBounds(String text, int left, int top)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        int height = fontRenderer.getWordWrappedHeight(text, pageWidth);
        int width = height > fontRenderer.FONT_HEIGHT ? pageWidth : fontRenderer.getStringWidth(text);
        return new Rectangle(left, top, width, height);
    }

    @Override
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        gui.drawTooltip(stack, x, y);
    }

    @Override
    public Object owner()
    {
        return gui;
    }
}
