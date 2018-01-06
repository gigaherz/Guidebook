package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.*;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualPage;
import gigaherz.guidebook.guidebook.drawing.VisualText;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BookRendering implements IBookGraphics
{
    public static final int DEFAULT_BOOK_WIDTH = 276;
    public static final int DEFAULT_BOOK_HEIGHT = 198;
    public static final int DEFAULT_INNER_MARGIN = 22;
    public static final int DEFAULT_OUTER_MARGIN = 10;
    public static final int DEFAULT_VERTICAL_MARGIN = 18;

    private BookDocument book;
    private Map<PageRef,VisualPage> visualPages = Maps.newHashMap();
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

    private int getSplitWidth(FontRenderer fontRenderer, String s)
    {
        int height = fontRenderer.getWordWrappedHeight(s, pageWidth);
        return height > fontRenderer.FONT_HEIGHT ? pageWidth : fontRenderer.getStringWidth(s);
    }

    @Override
    public int addString(int left, int top, String s, int color)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        fontRenderer.drawString(s, left, top, color);

        return fontRenderer.FONT_HEIGHT;
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

            final VisualPage pgLeft = getVisualPage(ch, new PageRef(currentChapter, currentPair * 2), true);

            if (mouseClickPage(mouseX, mouseY, pgLeft))
                return true;

            if (currentPair * 2 + 1 < ch.pages.size())
            {
                final VisualPage pgRight = getVisualPage(ch, new PageRef(currentChapter, currentPair * 2 + 1), false);

                if (mouseClickPage(mouseX, mouseY, pgRight))
                    return true;
            }
        }

        return false;
    }

    private VisualPage getVisualPage(BookDocument.ChapterData ch, PageRef pr, boolean isLeftPage)
    {
        VisualPage pg = visualPages.get(pr);

        if (pg == null)
        {
            Rectangle r = getPageBounds(isLeftPage);
            BookDocument.PageData pd = ch.pages.get(pr.page);
            pg = pd.reflow(r.getX(), r.getY(), r.getWidth(), r.getHeight());
            visualPages.put(pr, pg);
        }
        return pg;
    }

    private boolean mouseClickPage(int mX, int mY, VisualPage pg)
    {
        for (VisualElement e : pg.children)
        {
            if (mX >= e.position.x && mX <= (e.position.x + e.size.width) &&
                    mY >= e.position.y && mY <= (e.position.y + e.size.height))
            {
                e.click(this);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseHover(int mouseX, int mouseY)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);

        final VisualPage pgLeft = getVisualPage(ch, new PageRef(currentChapter, currentPair * 2), true);

        if (mouseHoverPage(mouseX, mouseY, pgLeft))
            return true;

        if (currentPair * 2 + 1 < ch.pages.size())
        {
            final VisualPage pgRight = getVisualPage(ch, new PageRef(currentChapter, currentPair * 2 + 1), false);

            if (mouseHoverPage(mouseX, mouseY, pgRight))
                return true;
        }

        return false;
    }

    private boolean mouseHoverPage(int mouseX, int mouseY, VisualPage pg)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mX = Mouse.getX() * dw / mc.displayWidth;
        int mY = dh - Mouse.getY() * dh / mc.displayHeight;

        return mouseHoverContainer(mouseX, mouseY, mX, mY, pg.children);
    }

    private boolean mouseHoverContainer(int mouseX, int mouseY, int mX, int mY, List<VisualElement> elements)
    {
        for (VisualElement e : elements)
        {
            if (mX >= e.position.x && mX <= (e.position.x + e.size.width) &&
                    mY >= e.position.y && mY <= (e.position.y + e.size.height))
            {
                e.mouseOver(this, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    Rectangle getPageBounds(boolean leftPage)
    {
        int guiWidth = gui.width;
        int guiHeight = gui.height;

        if (hasScale)
        {
            guiWidth = scaledWidth;
            guiHeight = scaledHeight;
        }

        int left = guiWidth / 2 - pageWidth - innerMargin;
        int right = guiWidth / 2 + innerMargin;
        int top = (guiHeight - pageHeight) / 2 - 9;

        return new Rectangle(leftPage ? left : right,top,pageWidth,pageHeight);
    }

    @Override
    public void drawCurrentPages()
    {
        if (hasScale)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scalingFactor, scalingFactor, scalingFactor);
        }

        int guiWidth = gui.width;
        int guiHeight = gui.height;

        if (hasScale)
        {
            guiWidth = scaledWidth;
            guiHeight = scaledHeight;
        }

        int left = guiWidth / 2 - pageWidth - innerMargin;
        int top = (guiHeight - pageHeight) / 2 - 9;
        int bottom = top + pageHeight - 3;

        drawPage(currentPair * 2, true);
        drawPage(currentPair * 2 + 1, false);

        String cnt = "" + ((book.getChapter(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (book.getTotalPairCount() * 2);
        Size sz = measure(cnt);

        addString(left + (pageWidth-sz.width)/2, bottom, cnt, 0xFF000000);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private void drawPage(int page, boolean isLeftPage)
    {
        BookDocument.ChapterData ch = book.getChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        VisualPage pg = getVisualPage(ch, new PageRef(currentChapter, page), isLeftPage);

        for (VisualElement e : pg.children)
        {
            e.draw(this);
        }
    }

    @Override
    public BookDocument getBook()
    {
        return book;
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
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        gui.drawTooltip(stack, x, y);
    }

    @Override
    public Object owner()
    {
        return gui;
    }

    @Override
    public Size measure(String text)
    {
        FontRenderer font = gui.getFontRenderer();
        int width = font.getStringWidth(text);
        return new Size(width, font.FONT_HEIGHT);
    }

    @Override
    public List<VisualElement> measure(String text, int width, int firstLineWidth)
    {
        //TODO: Actually measure the string width taking into account the first line in an efficient way.
        FontRenderer font = gui.getFontRenderer();
        int spaceWidth = font.getCharWidth(' ');
        List<String> lines = font.listFormattedStringToWidth(text, firstLineWidth);
        if (lines.size() > 1)
        {
            List<VisualElement> sizes = Lists.newArrayList();

            String firstLine = lines.get(0);
            int width1 = font.getStringWidth(firstLine);
            sizes.add(new VisualText(firstLine, new Size(width1, font.FONT_HEIGHT)));

            String remaining = text.substring(firstLine.length()).trim();
            List<String> lines2 = font.listFormattedStringToWidth(remaining, width);
            for (String s : lines2)
            {
                int width2 = font.getStringWidth(s);
                sizes.add(new VisualText(s, new Size(width2, font.FONT_HEIGHT)));
            }
            return sizes;
        }
        else
        {
            int width1 = font.getStringWidth(text);
            return Collections.singletonList(new VisualText(text, new Size(width1, font.FONT_HEIGHT * lines.size())));
        }
    }
}
