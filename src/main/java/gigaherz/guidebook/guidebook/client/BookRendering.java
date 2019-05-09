package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gigaherz.guidebook.ConfigValues;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.HoverContext;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.SectionRef;
import gigaherz.guidebook.guidebook.drawing.*;
import gigaherz.guidebook.guidebook.util.PointD;
import gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BookRendering implements IBookGraphics
{
    public static final int DEFAULT_BOOK_WIDTH = 276;
    public static final int DEFAULT_BOOK_HEIGHT = 198;
    public static final int DEFAULT_INNER_MARGIN = 16;
    public static final int DEFAULT_OUTER_MARGIN = 10;
    public static final int DEFAULT_TOP_MARGIN = 10;
    public static final int DEFAULT_BOTTOM_MARGIN = 18;
    public static final int BOOK_SCALE_MARGIN = 20;

    private final Minecraft mc = Minecraft.getInstance();
    private GuiGuidebook gui;

    private BookDocument book;

    private boolean hasScale;
    private double scalingFactor;

    private double scaledWidthD;
    private double scaledHeightD;
    private double scaledWidth;
    private double scaledHeight;

    private int bookWidth;
    private int bookHeight;
    private int innerMargin;
    private int outerMargin;
    private int topMargin;
    private int bottomMargin;
    private int pageWidth;
    private int pageHeight;

    private final List<VisualChapter> chapters = Lists.newArrayList();
    private int lastProcessedChapter = 0;

    private final java.util.Stack<PageRef> history = new java.util.Stack<>();
    private int currentChapter = 0;
    private int currentPair = 0;

    private boolean currentDrawingPage = false;

    private VisualElement previousHovering = null;

    public static boolean DEBUG_DRAW_BOUNDS = false;

    BookRendering(BookDocument book, GuiGuidebook gui)
    {
        this.book = book;
        this.gui = gui;
    }

    @Override
    public void resetRendering(boolean contentsChanged)
    {
        chapters.clear();
        lastProcessedChapter = 0;
        previousHovering = null;
        if(contentsChanged)
        {
            history.clear();
            currentChapter = 0;
            currentPair = 0;
        }
    }

    @Override
    public Object owner()
    {
        return gui;
    }

    @Override
    public BookDocument getBook()
    {
        return book;
    }

    public void computeBookScale(double scaleFactorCoef)
    {
        int width = mc.mainWindow.getFramebufferWidth();
        int height = mc.mainWindow.getFramebufferHeight();

        double w = (DEFAULT_BOOK_WIDTH + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;
        double h = (DEFAULT_BOOK_HEIGHT + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;

        int scaleFactor = 1;
        boolean flag = mc.getForceUnicodeFont(); // FIXME
        int i = ConfigValues.bookGUIScale < 0 ? mc.gameSettings.guiScale : ConfigValues.bookGUIScale;

        if (i == 0)
        {
            i = 1000;
        }

        while (scaleFactor < i && width / (scaleFactor + 1) >= w && height / (scaleFactor + 1) >= h)
        {
            ++scaleFactor;
        }

        if (flag && scaleFactor % 2 != 0 && scaleFactor > 1)
        {
            --scaleFactor;
        }

        this.scaledWidthD = (double) width / (double) scaleFactor;
        this.scaledHeightD = (double) height / (double) scaleFactor;
        this.scaledWidth = Math.ceil(scaledWidthD);
        this.scaledHeight = Math.ceil(scaledHeightD);
    }

    public void computeFlexScale(double scaleFactorCoef)
    {
        int width = mc.mainWindow.getFramebufferWidth();
        int height = mc.mainWindow.getFramebufferHeight();

        double w = (DEFAULT_BOOK_WIDTH + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;
        double h = (DEFAULT_BOOK_HEIGHT + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;

        double scale = Math.min(
                width / w,
                height / h
        );

        this.scaledWidth = this.scaledWidthD = width / scale;
        this.scaledHeight = this.scaledHeightD = height / scale;
    }

    public static boolean epsilonEquals(double a, double b)
    {
        return Math.abs(b - a) < 1.0E-5F;
    }

    @Override
    public boolean refreshScalingFactor()
    {
        double oldScale = scalingFactor;

        double fontSize = book.getFontSize();

        if (ConfigValues.flexibleScale)
        {
            computeFlexScale(fontSize);

            this.hasScale = true;
        }
        else if (!epsilonEquals(fontSize, 1.0))
        {
            computeBookScale(fontSize);

            this.hasScale = true;
        }

        if (hasScale)
        {
            this.scalingFactor = Math.min(gui.width / scaledWidth, gui.height / scaledHeight);

            this.bookWidth = (int) (DEFAULT_BOOK_WIDTH / fontSize);
            this.bookHeight = (int) (DEFAULT_BOOK_HEIGHT / fontSize);
            this.innerMargin = (int) (DEFAULT_INNER_MARGIN / fontSize);
            this.outerMargin = (int) (DEFAULT_OUTER_MARGIN / fontSize);
            this.topMargin = (int) (DEFAULT_TOP_MARGIN / fontSize);
            this.bottomMargin = (int) (DEFAULT_BOTTOM_MARGIN / fontSize);
        }
        else
        {
            this.hasScale = false;
            this.scalingFactor = 1.0f;
            this.scaledWidth = gui.width;
            this.scaledHeight = gui.height;

            this.bookWidth = DEFAULT_BOOK_WIDTH;
            this.bookHeight = DEFAULT_BOOK_HEIGHT;
            this.innerMargin = DEFAULT_INNER_MARGIN;
            this.outerMargin = DEFAULT_OUTER_MARGIN;
            this.topMargin = DEFAULT_TOP_MARGIN;
            this.bottomMargin = DEFAULT_BOTTOM_MARGIN;
        }

        this.pageWidth = this.bookWidth / 2 - this.innerMargin - this.outerMargin;
        this.pageHeight = this.bookHeight - this.topMargin - this.bottomMargin;

        return !epsilonEquals(scalingFactor, oldScale);
    }

    @Override
    public double getScalingFactor()
    {
        return scalingFactor;
    }

    private void pushHistory()
    {
        history.push(new PageRef(currentChapter, currentPair * 2));
    }

    @Override
    public boolean canGoNextPage()
    {
        return (getNextPair() >= 0 || canGoNextChapter());
    }

    @Override
    public void nextPage()
    {
        int pg = getNextPair();
        if (pg >= 0)
        {
            pushHistory();
            currentPair = pg;
        }
        else
        {
            nextChapter();
        }
    }

    @Override
    public boolean canGoPrevPage()
    {
        return getPrevPair() >= 0 || canGoPrevChapter();
    }

    @Override
    public void prevPage()
    {
        int pg = getPrevPair();
        if (pg >= 0)
        {
            pushHistory();
            currentPair = pg;
        }
        else
        {
            prevChapter(true);
        }
    }

    @Override
    public boolean canGoNextChapter()
    {
        return getNextChapter() >= 0;
    }

    @Override
    public void nextChapter()
    {
        int ch = getNextChapter();
        if (ch >= 0)
        {
            pushHistory();
            currentPair = 0;
            currentChapter = ch;
        }
    }

    @Override
    public boolean canGoPrevChapter()
    {
        return getPrevChapter() >= 0;
    }

    @Override
    public void prevChapter()
    {
        prevChapter(false);
    }

    private void prevChapter(boolean lastPage)
    {
        int ch = getPrevChapter();
        if (ch >= 0)
        {
            pushHistory();
            currentPair = 0;
            currentChapter = ch;
            if (lastPage) { currentPair = getVisualChapter(ch).totalPairs - 1; }
        }
    }

    @Override
    public boolean canGoBack()
    {
        return history.size() > 0;
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

    @Override
    public void navigateHome()
    {
        if (book.home != null)
        {
            navigateTo(book.home);
        }
    }

    private int getNextChapter()
    {
        for (int i = currentChapter + 1; i < book.chapterCount(); i++)
        {
            if (needChapter(i))
                return i;
        }
        return -1;
    }

    private int getPrevChapter()
    {
        for (int i = currentChapter - 1; i >= 0; i--)
        {
            if (needChapter(i))
                return i;
        }
        return -1;
    }

    private int getNextPair()
    {
        VisualChapter ch = getVisualChapter(currentChapter);
        if (currentPair + 1 >= ch.totalPairs)
            return -1;
        return currentPair + 1;
    }

    private int getPrevPair()
    {
        if (currentPair - 1 < 0)
            return -1;
        return currentPair - 1;
    }

    private boolean needChapter(int chapterNumber)
    {
        if (chapterNumber < 0 || chapterNumber >= book.chapterCount())
            return false;
        BookDocument.ChapterData ch = book.getChapter(chapterNumber);
        return ch.conditionResult && !ch.isEmpty();
    }

    private boolean needSection(int chapterNumber, int sectionNumber)
    {
        BookDocument.ChapterData ch = book.getChapter(chapterNumber);
        if (sectionNumber < 0 || sectionNumber >= ch.sections.size())
            return false;
        BookDocument.PageData section = ch.sections.get(sectionNumber);
        return section.conditionResult && !section.isEmpty();
    }

    private int findSectionStart(SectionRef ref)
    {
        VisualChapter vc = getVisualChapter(currentChapter);
        for (int i = 0; i < vc.pages.size(); i++)
        {
            VisualPage page = vc.pages.get(i);
            if (page.ref.section == ref.section)
                return i / 2;

            if (page.ref.section > ref.section)
                return 0; // give up
        }
        return 0;
    }

    @Override
    public void navigateTo(final SectionRef target)
    {
        if (!target.resolve(book))
            return;
        pushHistory();

        if (!needChapter(target.chapter))
            return;

        if (!needSection(target.chapter, target.section))
            return;

        currentChapter = target.chapter;

        currentPair = findSectionStart(target);
    }

    private VisualChapter getVisualChapter(int chapter)
    {
        while (chapters.size() <= chapter && lastProcessedChapter < book.chapterCount())
        {
            BookDocument.ChapterData bc = book.getChapter(lastProcessedChapter++);
            if (!bc.conditionResult)
                continue;

            VisualChapter ch = new VisualChapter();
            if (chapters.size() > 0)
            {
                VisualChapter prev = chapters.get(chapters.size() - 1);
                ch.startPair = prev.startPair + prev.totalPairs;
            }

            Size pageSize = new Size(pageWidth, pageHeight);
            bc.reflow(this, ch, pageSize);

            ch.totalPairs = (ch.pages.size() + 1) / 2;
            chapters.add(ch);
        }

        if (chapter >= chapters.size())
        {
            VisualChapter vc = new VisualChapter();
            vc.pages.add(new VisualPage(new SectionRef(chapter, 0)));
            return vc;
        }

        return chapters.get(chapter);
    }

    @Override
    public int addString(int left, int top, String s, int color, float scale)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        double left0 = left;
        double top0 = top;
        if (hasScale && ConfigValues.flexibleScale)
        {
            PointD pt = getPageOffset(currentDrawingPage);
            left0 = Math.floor((pt.x + left) * scalingFactor) / scalingFactor - pt.x;
            top0 = Math.floor((pt.y + top) * scalingFactor) / scalingFactor - pt.y;
        }

        // Does scaling need to be performed?
        if ((hasScale && ConfigValues.flexibleScale) || !(MathHelper.epsilonEquals(scale, 1.0f)))
        {
            GlStateManager.pushMatrix();
            {
                GlStateManager.translated(left0, top0, 0);
                GlStateManager.scaled(scale, scale, 1f);
                fontRenderer.drawString(s, 0, 0, color);
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
        Minecraft mc = Minecraft.getInstance();
        int width = mc.mainWindow.getFramebufferWidth();
        int height = mc.mainWindow.getFramebufferHeight();
        double dw = scaledWidth;
        double dh = scaledHeight;
        double[] xPos = new double[1], yPos = new double[1];
        GLFW.glfwGetCursorPos(mc.mainWindow.getHandle(), xPos, yPos);
        double mouseX = xPos[0] * dw / width;
        double mouseY = dh - yPos[0] * dh / height;

        if (mouseButton == 0)
        {
            VisualChapter ch = getVisualChapter(currentChapter);

            if (currentPair * 2 < ch.pages.size())
            {
                final VisualPage pgLeft = ch.pages.get(currentPair * 2);

                if (mouseClickPage(mouseX, mouseY, pgLeft, true))
                    return true;

                if (currentPair * 2 + 1 < ch.pages.size())
                {
                    final VisualPage pgRight = ch.pages.get(currentPair * 2 + 1);

                    if (mouseClickPage(mouseX, mouseY, pgRight, false))
                        return true;
                }
            }
        }

        return false;
    }

    private boolean mouseClickPage(double mX, double mY, VisualPage pg, boolean isLeftPage)
    {
        PointD offset = getPageOffset(isLeftPage);
        mX -= offset.x;
        mY -= offset.y;
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
        VisualChapter ch = getVisualChapter(currentChapter);

        if (currentPair * 2 < ch.pages.size())
        {
            final VisualPage pgLeft = ch.pages.get(currentPair * 2);

            final HoverContext hoverContext = new HoverContext(mouseX, mouseY);
            VisualElement hovering = mouseHoverPage(pgLeft, true, hoverContext);

            if (hovering == null)
            {
                if (currentPair * 2 + 1 < ch.pages.size())
                {
                    final VisualPage pgRight = ch.pages.get(currentPair * 2 + 1);

                    hovering = mouseHoverPage(pgRight, false, hoverContext);
                }
            }

            if (hovering != previousHovering && previousHovering != null)
            {
                previousHovering.mouseOut(this, hoverContext);
            }
            previousHovering = hovering;

            if (hovering != null)
            {
                hovering.mouseOver(this, hoverContext);
                return true;
            }
        }

        return false;
    }

    @Nullable
    private VisualElement mouseHoverPage(VisualPage pg, boolean isLeftPage, HoverContext mouseCoords)
    {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.mainWindow.getFramebufferWidth();
        int height = mc.mainWindow.getFramebufferHeight();
        double dw = scaledWidth;
        double dh = scaledHeight;
        double[] xPos = new double[1], yPos = new double[1];
        GLFW.glfwGetCursorPos(mc.mainWindow.getHandle(), xPos, yPos);
        double mX = xPos[0] * dw / width;
        double mY = dh - yPos[0] * dh / height;
        PointD offset = getPageOffset(isLeftPage);

        mX -= offset.x;
        mY -= offset.y;

        mouseCoords.mouseScaledX = mX;
        mouseCoords.mouseScaledY = mY;

        for (VisualElement e : pg.children)
        {
            if (mX >= e.position.x && mX <= (e.position.x + e.size.width) &&
                    mY >= e.position.y && mY <= (e.position.y + e.size.height))
            {
                if (e.wantsHover())
                    return e;
            }
        }

        return null;
    }

    @Override
    public void drawCurrentPages()
    {
        if (hasScale)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scaled(scalingFactor, scalingFactor, scalingFactor);
        }

        if (DEBUG_DRAW_BOUNDS)
        {
            int l = (int) ((scaledWidth - bookWidth) / 2);
            int t = (int) ((scaledHeight - bookHeight) / 2);
            Gui.drawRect(l, t, l + bookWidth, t + bookHeight, 0x2f000000);
        }

        drawPage(currentPair * 2);
        drawPage(currentPair * 2 + 1);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private PointD getPageOffset(boolean leftPage)
    {
        double left = (scaledWidth - bookWidth) / 2 + outerMargin;
        double right = left + pageWidth + innerMargin * 2;
        double top = (scaledHeight - bookHeight) / 2 + topMargin;

        return new PointD(leftPage ? left : right, top);
    }

    private void drawPage(int page)
    {
        VisualChapter ch = getVisualChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        currentDrawingPage = (page & 1) == 0;

        VisualPage pg = ch.pages.get(page);

        PointD offset = getPageOffset(currentDrawingPage);
        GlStateManager.pushMatrix();
        if (ConfigValues.flexibleScale)
            GlStateManager.translated(offset.x, offset.y, 0);
        else
            GlStateManager.translated((int)offset.x, (int)offset.y, 0);

        if(DEBUG_DRAW_BOUNDS)
        {
            Gui.drawRect(0, 0, pageWidth, pageHeight, 0x3f000000);
        }

        for (VisualElement e : pg.children)
        {
            e.draw(this);
        }

        String cnt = String.valueOf(ch.startPair * 2 + page + 1);
        Size sz = measure(cnt);

        addString((pageWidth - sz.width) / 2, pageHeight + 8, cnt, 0xFF000000, 1.0f);

        GlStateManager.popMatrix();
    }

    @Override
    public void drawItemStack(int left, int top, int z, ItemStack stack, int color, float scale)
    {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();

        GlStateManager.pushMatrix();
        GlStateManager.translated(left, top, z);
        GlStateManager.scaled(scale, scale, scale);

        RenderHelper.enableGUIStandardItemLighting();
        gui.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();

        gui.mc.getItemRenderer().renderItemOverlayIntoGUI(gui.getFontRenderer(), stack, 0, 0, null);

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.disableDepthTest();
    }

    @Override
    public void drawImage(ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale)
    {
        int sw = tw != 0 ? tw : 256;
        int sh = th != 0 ? th : 256;

        if (w == 0) w = sw;
        if (h == 0) h = sh;

        ResourceLocation locExpanded = new ResourceLocation(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
        gui.getRenderEngine().bindTexture(locExpanded);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        drawFlexible(x, y, tx, ty, w, h, sw, sh, scale);
    }

    private static void drawFlexible(int x, int y, float tx, float ty, int w, int h, int tw, int th, float scale)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        float hs = h * scale;
        float ws = w * scale;
        float tsw = 1.0f / tw;
        float tsh = 1.0f / th;
        bufferbuilder
                .pos(x, y + hs, 0.0D)
                .tex(tx * tsw, (ty + h) * tsh)
                .endVertex();
        bufferbuilder
                .pos(x + ws, y + hs, 0.0D)
                .tex((tx + w) * tsw, (ty + h) * tsh)
                .endVertex();
        bufferbuilder
                .pos(x + ws, y, 0.0D)
                .tex((tx + w) * tsw, ty * tsh)
                .endVertex();
        bufferbuilder
                .pos(x, y, 0.0D)
                .tex(tx * tsw, ty * tsh)
                .endVertex();
        tessellator.draw();
    }

    public void drawCustomTexture(ResourceLocation loc, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh, int tw, int th)
    {
        ResourceLocation locExpanded = new ResourceLocation(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
        gui.getRenderEngine().bindTexture(locExpanded);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        drawCustomQuad(dx, dy, dw, dh, sx, sy, sw, sh, tw, th);
    }

    private static void drawCustomQuad(int dx, int dy, int dw, int dh, float sx, int sy, int sw, float sh, int tw, int th)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        float tsx = sx / (float)tw;
        float tsy = sy / (float)th;
        float tsw = sw / (float)tw;
        float tsh = sh / (float)th;
        bufferbuilder
                .pos(dx, dy + dh, 0.0D)
                .tex(tsx, tsy + tsh)
                .endVertex();
        bufferbuilder
                .pos(dx + dw, dy + dh, 0.0D)
                .tex(tsx + tsw, tsy + tsh)
                .endVertex();
        bufferbuilder
                .pos(dx + dw, dy, 0.0D)
                .tex(tsx + tsw, tsy)
                .endVertex();
        bufferbuilder
                .pos(dx, dy, 0.0D)
                .tex(tsx, tsy)
                .endVertex();
        tessellator.draw();
    }

    @Override
    public void drawTooltip(ItemStack stack, int x, int y)
    {
        gui.drawTooltip(stack, x, y);
    }

    @Override
    public Size measure(String text)
    {
        FontRenderer font = gui.getFontRenderer();
        int width = font.getStringWidth(text);
        return new Size(width, font.FONT_HEIGHT);
    }

    @Override
    public List<VisualElement> measure(String text, int width, int firstLineWidth, float scale, int position, float baseline, int verticalAlignment)
    {
        FontRenderer font = gui.getFontRenderer();
        List<VisualElement> sizes = Lists.newArrayList();
        TextMetrics.wrapFormattedStringToWidth(font, (s) -> {
            int width2 = font.getStringWidth(s);
            sizes.add(new VisualText(s, new Size((int) (width2 * scale), (int) (font.FONT_HEIGHT * scale)), position, baseline, verticalAlignment, scale));
        }, text, width/scale, firstLineWidth/scale, true);
        return sizes;
    }

    @Override
    public int getActualBookHeight()
    {
        return bookHeight;
    }

    @Override
    public int getActualBookWidth()
    {
        return bookWidth;
    }

    public void setGui(GuiGuidebook guiGuidebook)
    {
        this.gui = guiGuidebook;
    }

    public static final IAnimatedBookBackgroundFactory DEFAULT_BACKGROUND = AnimatedBookBackground::new;
    public static final Map<ResourceLocation, IAnimatedBookBackgroundFactory> BACKGROUND_FACTORY_MAP = Maps.newHashMap();
    public IAnimatedBookBackground createBackground(GuiGuidebook guiGuidebook)
    {
        ResourceLocation loc = book.getBackground();
        IAnimatedBookBackgroundFactory factory = null;
        if (loc != null) factory = BACKGROUND_FACTORY_MAP.get(loc);
        return ((factory != null) ? factory : DEFAULT_BACKGROUND).create(guiGuidebook);
    }

    private static class TextMetrics
    {
        private static boolean isFormatColor(char colorChar)
        {
            return colorChar >= '0' && colorChar <= '9' || colorChar >= 'a' && colorChar <= 'f' || colorChar >= 'A' && colorChar <= 'F';
        }

        private static int sizeStringToWidth(FontRenderer font, String str, float wrapWidth)
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
                        j += font.getStringWidth("" + c0);

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

        private static void wrapFormattedStringToWidth(FontRenderer font, Consumer<String> dest, String str, float wrapWidth, float wrapWidthFirstLine, boolean firstLine)
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
                dest.accept("\n"); // line break
                char c0 = str.charAt(i);
                boolean flag = c0 == ' ' || c0 == '\n';
                String s1 = TextFormatting.getFormatString(s) + str.substring(i + (flag ? 1 : 0));
                wrapFormattedStringToWidth(font, dest, s1, wrapWidth, wrapWidthFirstLine, false);
            }
        }
    }

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
}
