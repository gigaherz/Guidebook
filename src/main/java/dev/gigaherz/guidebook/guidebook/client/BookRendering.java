package dev.gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import dev.gigaherz.guidebook.ConfigValues;
import dev.gigaherz.guidebook.guidebook.book.BookDocument;
import dev.gigaherz.guidebook.guidebook.HoverContext;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.SectionRef;
import dev.gigaherz.guidebook.guidebook.book.ChapterData;
import dev.gigaherz.guidebook.guidebook.book.PageData;
import dev.gigaherz.guidebook.guidebook.drawing.VisualChapter;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPage;
import dev.gigaherz.guidebook.guidebook.drawing.VisualText;
import dev.gigaherz.guidebook.guidebook.util.PointD;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class BookRendering implements IBookGraphics
{
    public static final int DEFAULT_INNER_MARGIN = 12;
    public static final int DEFAULT_OUTER_MARGIN = 10;
    public static final int DEFAULT_TOP_MARGIN = 12;
    public static final int DEFAULT_BOTTOM_MARGIN = 16;
    public static final int DEFAULT_BOOK_WIDTH = 308 + DEFAULT_OUTER_MARGIN*2;
    public static final int DEFAULT_BOOK_HEIGHT = 192 + DEFAULT_TOP_MARGIN + DEFAULT_BOTTOM_MARGIN;
    public static final int BOOK_SCALE_MARGIN = 0;

    private final Minecraft mc = Minecraft.getInstance();
    private GuidebookScreen gui;

    private BookDocument book;

    private boolean hasScale;
    private float scalingFactor;

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

    BookRendering(BookDocument book, GuidebookScreen gui)
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
        if (contentsChanged)
        {
            history.clear();
            currentChapter = 0;
            currentPair = 0;
        }
    }

    @Override
    public Level getWorld()
    {
        return Objects.requireNonNull(mc.level);
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
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        double w = (DEFAULT_BOOK_WIDTH + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;
        double h = (DEFAULT_BOOK_HEIGHT + 2 * BOOK_SCALE_MARGIN) / scaleFactorCoef;

        int scaleFactor = 1;
        boolean flag = mc.isEnforceUnicode(); // FIXME
        int i = ConfigValues.bookGUIScale < 0 ? mc.options.guiScale().get() : ConfigValues.bookGUIScale;

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
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

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
            this.scalingFactor = (float) Math.min(gui.width / scaledWidth, gui.height / scaledHeight);

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
            if (lastPage) {currentPair = getVisualChapter(ch).totalPairs - 1;}
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
        ChapterData ch = book.getChapter(chapterNumber);
        return ch.conditionResult && !ch.isEmpty();
    }

    private boolean needSection(int chapterNumber, int sectionNumber)
    {
        ChapterData ch = book.getChapter(chapterNumber);
        if (sectionNumber < 0 || sectionNumber >= ch.sections.size())
            return false;
        PageData section = ch.sections.get(sectionNumber);
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
            ChapterData bc = book.getChapter(lastProcessedChapter++);
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
    public int addString(PoseStack matrixStack, int left, int top, Component text, int color, float scale)
    {
        Font fontRenderer = gui.getFontRenderer();

        double left0 = left;
        double top0 = top;
        if (hasScale && ConfigValues.flexibleScale)
        {
            PointD pt = getPageOffset(currentDrawingPage);
            left0 = Math.floor((pt.x + left) * scalingFactor) / scalingFactor - pt.x;
            top0 = Math.floor((pt.y + top) * scalingFactor) / scalingFactor - pt.y;
        }

        // Does scaling need to be performed?
        if ((hasScale && ConfigValues.flexibleScale) || !(Mth.equal(scale, 1.0f)))
        {
            matrixStack.pushPose();
            {
                matrixStack.translate(left0, top0, 0);
                matrixStack.scale(scale, scale, 1f);
                fontRenderer.draw(matrixStack, text, 0, 0, color);
            }
            matrixStack.popPose();
        }
        else
        {
            fontRenderer.draw(matrixStack, text, left, top, color);
        }

        return fontRenderer.lineHeight;
    }

    @Override
    public boolean mouseClicked(int mx, int my, int mouseButton)
    {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        double dw = scaledWidth;
        double dh = scaledHeight;
        double[] xPos = new double[1], yPos = new double[1];
        GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), xPos, yPos);
        double mouseX = xPos[0] * dw / width;
        double mouseY = yPos[0] * dh / height;

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
    public boolean mouseHover(PoseStack matrixStack, int mouseX, int mouseY)
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
                hovering.mouseOver(this, hoverContext, matrixStack);
                return true;
            }
        }

        return false;
    }

    @Nullable
    private VisualElement mouseHoverPage(VisualPage pg, boolean isLeftPage, HoverContext mouseCoords)
    {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        double dw = scaledWidth;
        double dh = scaledHeight;
        double[] xPos = new double[1], yPos = new double[1];
        GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), xPos, yPos);
        double mX = xPos[0] * dw / width;
        double mY = yPos[0] * dh / height;
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
    public void drawCurrentPages(PoseStack matrixStack)
    {
        if (hasScale)
        {
            matrixStack.pushPose();
            matrixStack.scale(scalingFactor, scalingFactor, scalingFactor);
        }

        if (DEBUG_DRAW_BOUNDS)
        {
            int l = (int) ((scaledWidth - bookWidth) / 2);
            int t = (int) ((scaledHeight - bookHeight) / 2);
            GuiComponent.fill(matrixStack, l, t, l + bookWidth, t + bookHeight, 0x2f000000);
        }

        drawPage(matrixStack, currentPair * 2);
        drawPage(matrixStack, currentPair * 2 + 1);

        if (hasScale)
        {
            matrixStack.popPose();
        }
    }

    private PointD getPageOffset(boolean leftPage)
    {
        double left = (scaledWidth - bookWidth) / 2 + outerMargin;
        double right = left + pageWidth + innerMargin * 2;
        double top = (scaledHeight - bookHeight) / 2 + topMargin;

        return new PointD(leftPage ? left : right, top);
    }

    private void drawPage(PoseStack matrixStack, int page)
    {
        VisualChapter ch = getVisualChapter(currentChapter);
        if (page >= ch.pages.size())
            return;

        currentDrawingPage = (page & 1) == 0;

        VisualPage pg = ch.pages.get(page);

        PointD offset = getPageOffset(currentDrawingPage);
        matrixStack.pushPose();
        if (ConfigValues.flexibleScale)
            matrixStack.translate(offset.x, offset.y, 0);
        else
            matrixStack.translate((int) offset.x, (int) offset.y, 0);

        if (DEBUG_DRAW_BOUNDS)
        {
            GuiComponent.fill(matrixStack, 0, 0, pageWidth, pageHeight, 0x3f000000);
        }

        for (VisualElement e : pg.children)
        {
            e.draw(this, matrixStack);
        }

        Component cnt = Component.literal(String.valueOf(ch.startPair * 2 + page + 1));
        Size sz = measure(cnt);

        addString(matrixStack, (pageWidth - sz.width) / 2, pageHeight + 8, cnt, 0xFF000000, 1.0f);

        matrixStack.popPose();
    }

    @Override
    public void drawItemStack(PoseStack matrixStack, int left, int top, int z, ItemStack stack, int color, float scale)
    {
        RenderSystem.enableDepthTest();

        matrixStack.pushPose();
        matrixStack.translate(left, top, z);
        matrixStack.scale(scale, scale, scale);

        ItemRenderer renderer = gui.getMinecraft().getItemRenderer();

        PoseStack viewModelPose = RenderSystem.getModelViewStack();
        viewModelPose.pushPose();
        viewModelPose.mulPoseMatrix(matrixStack.last().pose());
        viewModelPose.translate(-8, -8, z);
        RenderSystem.applyModelViewMatrix();
        renderer.renderAndDecorateItem(stack, 8, 8);
        renderer.renderGuiItemDecorations(mc.font, stack, 8, 8, "");
        viewModelPose.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();

        matrixStack.popPose();
    }

    @Override
    public void drawImage(PoseStack matrixStack, ResourceLocation loc, int x, int y, int tx, int ty, int w, int h, int tw, int th, float scale)
    {
        int sw = tw != 0 ? tw : 256;
        int sh = th != 0 ? th : 256;

        if (w == 0) w = sw;
        if (h == 0) h = sh;

        ResourceLocation locExpanded = new ResourceLocation(loc.getNamespace(), "textures/" + loc.getPath() + ".png");

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, locExpanded);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawFlexible(matrixStack, x, y, tx, ty, w, h, sw, sh, scale);
    }

    private static void drawFlexible(PoseStack matrixStack, int x, int y, float tx, float ty, int w, int h, int tw, int th, float scale)
    {
        drawFlexible(matrixStack.last().pose(), x, y, tx, ty, w, h, tw, th, scale);
    }

    private static void drawFlexible(Matrix4f matrix, int x, int y, float tx, float ty, int w, int h, int tw, int th, float scale)
    {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float hs = h * scale;
        float ws = w * scale;
        float tsw = 1.0f / tw;
        float tsh = 1.0f / th;
        bufferbuilder
                .vertex(matrix, x, y + hs, 0.0f)
                .uv(tx * tsw, (ty + h) * tsh)
                .endVertex();
        bufferbuilder
                .vertex(matrix, x + ws, y + hs, 0.0f)
                .uv((tx + w) * tsw, (ty + h) * tsh)
                .endVertex();
        bufferbuilder
                .vertex(matrix, x + ws, y, 0.0f)
                .uv((tx + w) * tsw, ty * tsh)
                .endVertex();
        bufferbuilder
                .vertex(matrix, x, y, 0.0f)
                .uv(tx * tsw, ty * tsh)
                .endVertex();
        tessellator.end();
    }

    @Override
    public void drawTooltip(PoseStack matrixStack, ItemStack stack, int x, int y)
    {
        gui.drawTooltip(matrixStack, stack, x, y);
    }

    @Override
    public Size measure(FormattedText text)
    {
        Font font = gui.getFontRenderer();
        int width = font.width(text);
        return new Size(width, font.lineHeight);
    }

    @Override
    public List<VisualElement> measure(FormattedText text, int width, int firstLineWidth, float scale, int position, float baseline, int verticalAlignment)
    {
        Font font = gui.getFontRenderer();
        List<VisualElement> sizes = Lists.newArrayList();
        TextMetrics.wrapFormattedStringToWidth(font, (s) -> {
            int width2 = font.width(s);
            sizes.add(new VisualText(s, new Size((int) (width2 * scale), (int) (font.lineHeight * scale)), position, baseline, verticalAlignment, scale));
        }, text, width / scale, firstLineWidth / scale, true);
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

    public void setGui(GuidebookScreen guidebookScreen)
    {
        this.gui = guidebookScreen;
    }

    public static final IAnimatedBookBackgroundFactory DEFAULT_BACKGROUND = AnimatedBookBackground::new;
    public static final Map<ResourceLocation, IAnimatedBookBackgroundFactory> BACKGROUND_FACTORY_MAP = Maps.newHashMap();

    public IAnimatedBookBackground createBackground(GuidebookScreen guidebookScreen)
    {
        ResourceLocation loc = book.getBackground();
        IAnimatedBookBackgroundFactory factory = null;
        if (loc != null) factory = BACKGROUND_FACTORY_MAP.get(loc);
        return ((factory != null) ? factory : DEFAULT_BACKGROUND).create(guidebookScreen);
    }

    private static class TextMetrics
    {
        private static void wrapFormattedStringToWidth(Font font, Consumer<Component> dest, FormattedText str, float wrapWidth, float wrapWidthFirstLine, boolean firstLine)
        {
            str.visit((style, text) -> {
                wrapFormattedStringToWidth(font, dest, text, style, wrapWidth, wrapWidthFirstLine, firstLine);
                return FormattedText.STOP_ITERATION;
            }, Style.EMPTY);
        }

        private static void wrapFormattedStringToWidth(final Font font, final Consumer<Component> dest, final String str, final Style style, final float wrapWidth, final float wrapWidthFirstLine, final boolean firstLine)
        {
            if (str.length() == 0)
                return;

            int i = sizeStringToWidth(font, str, style, firstLine ? wrapWidthFirstLine : wrapWidth);

            if (str.length() <= i)
            {
                dest.accept(Component.literal(str).withStyle(style));
            }
            else
            {
                if (i < 1) i = 1;
                String firstPart = str.substring(0, i);
                dest.accept(Component.literal(firstPart).withStyle(style));
                char nextChar = str.charAt(i);
                boolean isWhitespace = nextChar == ' ' || nextChar == '\n';
                String secondPart = str.substring(i + (isWhitespace ? 1 : 0));
                wrapFormattedStringToWidth(font, dest, secondPart, style, wrapWidth, wrapWidthFirstLine, false);
            }
        }

        private static int sizeStringToWidth(Font font, String str, Style style, float wrapWidth)
        {
            int w = font.getSplitter().plainIndexAtWidth(str, (int) wrapWidth, style);

            // If nothing fits or everything fits, no need to check for whitespace.
            if (w == 0 || w == str.length()) return w;

            if (isNonBreakWhitespace(str.charAt(w)))
            {
                return w;
            }

            while (w >= 0 && !isNonBreakWhitespace(str.charAt(w)))
            {
                w--;
            }
            return w + 1;
        }

        private static boolean isNonBreakWhitespace(char c)
        {
            return Character.isWhitespace(c) && c != '\u00A0' && c != '\u202F' && c != '\uFeFF';
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
