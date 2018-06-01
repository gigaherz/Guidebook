package gigaherz.guidebook.guidebook.client;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.*;
import gigaherz.guidebook.guidebook.drawing.*;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    private class PageRef
    {
        public int chapter;
        public int page;

        public PageRef(int currentChapter, int currentPage)
        {
            chapter = currentChapter;
            page = currentPage;
        }
    }

    private class VisualChapter
    {
        public final List<VisualPage> pages = Lists.newArrayList();
        public final Map<String, Integer> pagesByName = Maps.newHashMap();
        public int startPair;
        public int totalPairs;
    }

    final List<VisualChapter> chapters = Lists.newArrayList();

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
        return (currentPair + 1 < getVisualChapter(currentChapter).totalPairs || currentChapter + 1 < book.chapterCount());
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
    public void navigateTo(final SectionRef target)
    {
        if (!target.resolve(book))
            return;
        pushHistory();
        currentChapter = Math.max(0, Math.min(book.chapterCount() - 1, target.chapter));
        currentPair = Math.max(0, Math.min(getVisualChapter(currentChapter).totalPairs - 1, target.page / 2));
    }

    @Override
    public void nextPage()
    {
        if (currentPair + 1 < getVisualChapter(currentChapter).totalPairs)
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
            currentPair = getVisualChapter(currentChapter).totalPairs - 1;
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
            //target.resolve(book);
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

    @Override
    public int addString(int left, int top, String s, int color, float scale)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        // Does scaling need to be performed?
        if(!(MathHelper.epsilonEquals(scale, 1.0f)))
        {
            GlStateManager.pushMatrix();
            {
                GlStateManager.scale(scale, scale, 1f);
                fontRenderer.drawString(s, (int)(left / scale), (int)(top / scale), color);
            }
            GlStateManager.popMatrix();
        }
        else
        {
            fontRenderer.drawString(s, left, top, color);
        }

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
            VisualChapter ch = getVisualChapter(currentChapter);

            final VisualPage pgLeft = ch.pages.get(currentPair * 2);

            if (mouseClickPage(mouseX, mouseY, pgLeft))
                return true;

            if (currentPair * 2 + 1 < ch.pages.size())
            {
                final VisualPage pgRight = ch.pages.get(currentPair * 2 + 1);

                if (mouseClickPage(mouseX, mouseY, pgRight))
                    return true;
            }
        }

        return false;
    }

    private VisualChapter getVisualChapter(int chapter)
    {
        while (chapters.size() <= chapter && chapters.size() < book.chapterCount())
        {
            VisualChapter ch = new VisualChapter();
            if (chapters.size() > 0)
            {
                VisualChapter prev = chapters.get(chapters.size() - 1);
                ch.startPair = prev.startPair + prev.totalPairs;
            }

            BookDocument.SectionData bc = book.getChapter(chapters.size());

            Rect rl = getPageBounds(true);
            Rect rr = getPageBounds(false);
            for(BookDocument.PageData section : bc.sections)
            {
                if(!Strings.isNullOrEmpty(section.id))
                    ch.pagesByName.put(section.id, ch.pages.size());

                ch.pages.addAll(section.reflow(rl,rr,ch.pages.size()));
            }

            ch.totalPairs = (ch.pages.size()+1)/2;
            chapters.add(ch);
        }

        return chapters.get(chapter);
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


    VisualElement previousHovering = null;

    @Override
    public boolean mouseHover(int mouseX, int mouseY)
    {
        VisualChapter ch = getVisualChapter(currentChapter);

        final VisualPage pgLeft = ch.pages.get(currentPair * 2);

        VisualElement hovering = mouseHoverPage(pgLeft);

        if (hovering == null)
        {
            if (currentPair * 2 + 1 < ch.pages.size())
            {
                final VisualPage pgRight = ch.pages.get(currentPair * 2 + 1);

                hovering = mouseHoverPage(pgRight);
            }
        }

        if (hovering != previousHovering && previousHovering != null)
        {
            previousHovering.mouseOut(this, mouseX, mouseY);
        }
        previousHovering = hovering;

        if (hovering != null)
        {
            hovering.mouseOver(this, mouseX, mouseY);
            return true;
        }

        return false;
    }

    @Nullable
    private VisualElement mouseHoverPage(VisualPage pg)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = hasScale ? scaledWidth : gui.width;
        int dh = hasScale ? scaledHeight : gui.height;
        int mX = Mouse.getX() * dw / mc.displayWidth;
        int mY = dh - Mouse.getY() * dh / mc.displayHeight;

        return mouseHoverContainer(mX, mY, pg.children);
    }

    @Nullable
    private VisualElement mouseHoverContainer(int mX, int mY, List<VisualElement> elements)
    {
        for (VisualElement e : elements)
        {
            if (e.wantsHover() &&
                    mX >= e.position.x && mX <= (e.position.x + e.size.width) &&
                    mY >= e.position.y && mY <= (e.position.y + e.size.height))
            {
                return e;
            }
        }
        return null;
    }

    Rect getPageBounds(boolean leftPage)
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

        return new Rect(leftPage ? left : right,top,pageWidth,pageHeight);
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

        drawPage(currentPair * 2);
        drawPage(currentPair * 2 + 1);

        String cnt = "" + ((getVisualChapter(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (getTotalPairCount() * 2);
        Size sz = measure(cnt);

        addString(left + (pageWidth-sz.width)/2, bottom, cnt, 0xFF000000, 1.0f);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private int getTotalPairCount()
    {
        VisualChapter last = chapters.get(chapters.size() - 1);
        return last.startPair + last.totalPairs;
    }

    private void drawPage(int page)
    {
        VisualChapter ch = getVisualChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        VisualPage pg = ch.pages.get(page);

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

    private static boolean isFormatColor(char colorChar)
    {
        return colorChar >= '0' && colorChar <= '9' || colorChar >= 'a' && colorChar <= 'f' || colorChar >= 'A' && colorChar <= 'F';
    }

    private static int sizeStringToWidth(FontRenderer font, String str, int wrapWidth)
    {
        int i = str.length();
        int j = 0;
        int k = 0;
        int l = -1;

        for (boolean flag = false; k < i; ++k)
        {
            char c0 = str.charAt(k);

            switch (c0)
            {
                case '\n':
                    --k;
                    break;
                case ' ':
                    l = k;
                default:
                    j += font.getCharWidth(c0);

                    if (flag)
                    {
                        ++j;
                    }

                    break;
                case '\u00a7':

                    if (k < i - 1)
                    {
                        ++k;
                        char c1 = str.charAt(k);

                        if (c1 != 'l' && c1 != 'L')
                        {
                            if (c1 == 'r' || c1 == 'R' || isFormatColor(c1))
                            {
                                flag = false;
                            }
                        }
                        else
                        {
                            flag = true;
                        }
                    }
            }

            if (c0 == '\n')
            {
                ++k;
                l = k;
                break;
            }

            if (j > wrapWidth)
            {
                break;
            }
        }

        return k != i && l != -1 && l < k ? l : k;
    }

    private static void wrapFormattedStringToWidth(FontRenderer font, Consumer<String> dest, String str, int wrapWidth, int wrapWidthFirstLine, boolean firstLine)
    {
        int i = sizeStringToWidth(font, str, firstLine ? wrapWidthFirstLine : wrapWidth);

        if (str.length() <= i)
        {
            dest.accept(str);
        }
        else
        {
            String s = str.substring(0, i);
            dest.accept(s);
            char c0 = str.charAt(i);
            boolean flag = c0 == ' ' || c0 == '\n';
            String s1 = FontRenderer.getFormatFromString(s) + str.substring(i + (flag ? 1 : 0));
            wrapFormattedStringToWidth(font, dest, s1, wrapWidth, wrapWidthFirstLine, false);
        }
    }

    @Override
    public List<VisualElement> measure(String text, int width, int firstLineWidth, float scale)
    {
        FontRenderer font = gui.getFontRenderer();
        List<VisualElement> sizes = Lists.newArrayList();
        wrapFormattedStringToWidth(font, (s) -> {
            int width2 = font.getStringWidth(s);
            sizes.add(new VisualText(s, new Size(width2, font.FONT_HEIGHT), scale));
        }, text, width, firstLineWidth, true);
        return sizes;
    }
}
