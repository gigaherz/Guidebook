package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.common.client.StackRenderingHelper;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.management.openmbean.KeyAlreadyExistsException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.minecraftforge.fml.common.LoaderState.INITIALIZATION;

/**
 * CLIENT ONLY!
 */
public class BookDocument
{
    public static final int DEFAULT_BOOK_WIDTH = 276;
    public static final int DEFAULT_BOOK_HEIGHT = 198;
    private static final int DEFAULT_INNER_MARGIN = 22;
    private static final int DEFAULT_OUTER_MARGIN = 10;
    private static final int DEFAULT_VERTICAL_MARGIN = 18;
    private static final float DEFAULT_FONT_SIZE = 1.0f;

    private int bookWidth;
    private int bookHeight;
    private int innerMargin;
    private int outerMargin;
    private int verticalMargin;
    private int pageWidth = bookWidth / 2 - innerMargin - outerMargin;
    private int pageHeight = bookHeight - verticalMargin;
    private boolean hasScale;
    private float fontSize;
    private float scalingFactor;
    private ScaledResolution2 sr2;

    public static final Map<ResourceLocation, BookDocument> REGISTRY = Maps.newHashMap();

    public static void registerBook(ResourceLocation loc)
    {
        if (Loader.instance().hasReachedState(INITIALIZATION))
            throw new IllegalStateException("Books must be registered before init, preferably in the BookRegistryEvent.");
        if (REGISTRY.containsKey(loc))
            throw new KeyAlreadyExistsException("A book with this id has already been registered.");
        BookDocument book = new BookDocument(loc);
        REGISTRY.put(loc, book);
    }

    @Nullable
    public static BookDocument get(ResourceLocation loc)
    {
        return REGISTRY.get(loc);
    }

    public static void parseAllBooks()
    {
        REGISTRY.values().forEach(BookDocument::parseBook);
    }

    public static void initReloadHandler()
    {
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager)
        {
            ((IReloadableResourceManager) rm).registerReloadListener(__ -> parseAllBooks());
        }
    }

    private final ResourceLocation bookLocation;
    private String bookName;
    private ResourceLocation bookCover;

    private List<ChapterData> chapters = Lists.newArrayList();

    private Map<String, Integer> chaptersByName = Maps.newHashMap();
    private Map<String, PageRef> pagesByName = Maps.newHashMap();

    private int totalPairs = 0;

    private int currentChapter = 0;
    private int currentPair = 0;

    java.util.Stack<PageRef> history = new java.util.Stack<>();

    public float getScalingFactor()
    {
        return scalingFactor;
    }

    public float getFontSize()
    {
        return fontSize;
    }

    private static class ScaledResolution2
    {
        final double scaledWidthD;
        final double scaledHeightD;
        int scaledWidth;
        int scaledHeight;
        int scaleFactor;

        public ScaledResolution2(Minecraft minecraftClient, float scaleFactorCoef)
        {
            this.scaledWidth = minecraftClient.displayWidth;
            this.scaledHeight = minecraftClient.displayHeight;
            this.scaleFactor = 1;
            boolean flag = minecraftClient.isUnicode();
            int i = minecraftClient.gameSettings.guiScale;

            if (i == 0)
            {
                i = 1000;
            }

            while (this.scaleFactor < i && this.scaledWidth / (this.scaleFactor + 1) >= 320 && this.scaledHeight / (this.scaleFactor + 1) >= 240)
            {
                ++this.scaleFactor;
            }

            this.scaleFactor = MathHelper.floor_double(Math.max(1, this.scaleFactor * scaleFactorCoef));

            if (flag && this.scaleFactor % 2 != 0 && this.scaleFactor != 1)
            {
                --this.scaleFactor;
            }

            this.scaledWidthD = (double)this.scaledWidth / (double)this.scaleFactor;
            this.scaledHeightD = (double)this.scaledHeight / (double)this.scaleFactor;
            this.scaledWidth = MathHelper.ceiling_double_int(this.scaledWidthD);
            this.scaledHeight = MathHelper.ceiling_double_int(this.scaledHeightD);
        }
    }

    public void setScalingFactor()
    {
        if (MathHelper.epsilonEquals(fontSize, 1.0f))
        {
            this.hasScale = false;
            this.scalingFactor = 1.0f;
            this.sr2 = null;

            this.bookWidth = DEFAULT_BOOK_WIDTH;
            this.bookHeight = DEFAULT_BOOK_HEIGHT;
            this.innerMargin = DEFAULT_INNER_MARGIN;
            this.outerMargin = DEFAULT_OUTER_MARGIN;
            this.verticalMargin = DEFAULT_VERTICAL_MARGIN;
        }
        else
        {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            sr2 = new ScaledResolution2(Minecraft.getMinecraft(), fontSize);

            this.hasScale = true;
            this.scalingFactor = sr.getScaledHeight() / (float)sr2.scaledHeight;

            this.bookWidth = (int) (DEFAULT_BOOK_WIDTH / fontSize);
            this.bookHeight = (int) (DEFAULT_BOOK_HEIGHT / fontSize);
            this.innerMargin = (int) (DEFAULT_INNER_MARGIN / fontSize);
            this.outerMargin = (int) (DEFAULT_OUTER_MARGIN / fontSize);
            this.verticalMargin = (int) (DEFAULT_VERTICAL_MARGIN / fontSize);
        }

        this.pageWidth = this.bookWidth / 2 - this.innerMargin - this.outerMargin;
        this.pageHeight = this.bookHeight - this.verticalMargin;
    }

    private BookDocument(ResourceLocation bookLocation)
    {
        this.bookLocation = bookLocation;
    }

    @Nullable
    public String getBookName()
    {
        return bookName;
    }

    @Nullable
    public ResourceLocation getBookCover()
    {
        return bookCover;
    }

    public int chapterCount()
    {
        return chapters.size();
    }

    public int getBookWidth()
    {
        return bookWidth;
    }

    public int getBookHeight()
    {
        return bookHeight;
    }

    public void findTextures(Set<ResourceLocation> textures)
    {
        if (bookCover != null)
            textures.add(bookCover);

        // TODO: Add <image> texture locations when implemented
        for (ChapterData chapter : chapters)
        {
            for (PageData page : chapter.pages)
            {
                for (IPageElement element : page.elements)
                {
                    element.findTextures(textures);
                }
            }
        }
    }

    public boolean canGoBack()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    public boolean canGoNextPage()
    {
        return (currentPair + 1 < chapters.get(currentChapter).pagePairs || currentChapter + 1 < chapters.size());
    }

    public boolean canGoPrevPage()
    {
        return (currentPair > 0 || currentChapter > 0);
    }

    public boolean canGoNextChapter()
    {
        return (currentChapter + 1 < chapters.size());
    }

    public boolean canGoPrevChapter()
    {
        return (currentChapter > 0);
    }

    public void navigateTo(final PageRef target)
    {
        pushHistory();

        target.resolve();
        currentChapter = Math.max(0, Math.min(chapters.size() - 1, target.chapter));
        currentPair = Math.max(0, Math.min(chapters.get(currentChapter).pagePairs - 1, target.page / 2));
    }

    public void nextPage()
    {
        if (currentPair + 1 < chapters.get(currentChapter).pagePairs)
        {
            pushHistory();
            currentPair++;
        }
        else if (currentChapter + 1 < chapters.size())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

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
            currentPair = chapters.get(currentChapter).pagePairs - 1;
        }
    }

    public void nextChapter()
    {
        if (currentChapter + 1 < chapters.size())
        {
            pushHistory();
            currentPair = 0;
            currentChapter++;
        }
    }

    public void prevChapter()
    {
        if (currentChapter > 0)
        {
            pushHistory();
            currentPair = 0;
            currentChapter--;
        }
    }

    public void navigateBack()
    {
        if (history.size() > 0)
        {
            PageRef target = history.pop();
            target.resolve();
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

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (mouseButton == 0)
        {
            ChapterData ch = chapters.get(currentChapter);
            PageData pg = ch.pages.get(currentPair * 2);
            for (IPageElement e : pg.elements)
            {
                if (e instanceof Link)
                {
                    Link l = (Link) e;
                    Rectangle b = l.getBounds();
                    if (mouseX >= b.getX() && mouseX <= (b.getX() + b.getWidth()) &&
                            mouseY >= b.getY() && mouseY <= (b.getY() + b.getHeight()))
                    {
                        l.click();
                        return true;
                    }
                }
            }

            if (currentPair * 2 + 1 < ch.pages.size())
            {
                pg = ch.pages.get(currentPair * 2 + 1);
                for (IPageElement e : pg.elements)
                {
                    if (e instanceof Link)
                    {
                        Link l = (Link) e;
                        Rectangle b = l.getBounds();
                        if (mouseX >= b.getX() && mouseX <= (b.getX() + b.getWidth()) &&
                                mouseY >= b.getY() && mouseY <= (b.getY() + b.getHeight()))
                        {
                            l.click();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void parseBook()
    {
        try
        {
            chapters.clear();
            bookName = "";
            bookCover = null;
            totalPairs = 0;
            currentChapter = 0;
            currentPair = 0;
            chaptersByName.clear();
            pagesByName.clear();
            history.clear();

            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(bookLocation);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(res.getInputStream());

            doc.getDocumentElement().normalize();

            Node root = doc.getChildNodes().item(0);

            if(root.hasAttributes())
            {
                NamedNodeMap rootAttributes = root.getAttributes();
                Node n = rootAttributes.getNamedItem("title");
                if (n != null)
                {
                    bookName = n.getTextContent();
                }
                n = rootAttributes.getNamedItem("cover");
                if (n != null)
                {
                    bookCover = new ResourceLocation(n.getTextContent());
                }
                n = rootAttributes.getNamedItem("fontSize");
                if (n != null)
                {
                    Float f = Floats.tryParse(n.getTextContent());
                    fontSize = f != null ? f : DEFAULT_FONT_SIZE;
                }
            }

            NodeList chaptersList = root.getChildNodes();
            for (int i = 0; i < chaptersList.getLength(); i++)
            {
                Node chapterItem = chaptersList.item(i);

                parseChapter(chapterItem);
            }

            int prevCount = 0;
            for (ChapterData chapter : chapters)
            {
                chapter.startPair = prevCount;
                prevCount += chapter.pagePairs;
            }
            totalPairs = prevCount;
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            ChapterData ch = new ChapterData(0);
            chapters.add(ch);

            PageData pg = new PageData(0);
            ch.pages.add(pg);

            pg.elements.add(new Paragraph("Error loading book:"));
            pg.elements.add(new Paragraph(TextFormatting.RED + e.toString()));
        }
    }

    private void parseChapter(Node chapterItem)
    {
        if (!chapterItem.getNodeName().equals("chapter"))
        {
            return;
        }

        ChapterData chapter = new ChapterData(chapters.size());
        chapters.add(chapter);

        if (chapterItem.hasAttributes())
        {
            NamedNodeMap chapterAttributes = chapterItem.getAttributes();
            Node n = chapterAttributes.getNamedItem("id");
            if (n != null)
            {
                chapter.id = n.getTextContent();
                chaptersByName.put(chapter.id, chapter.num);
            }
        }

        NodeList pagesList = chapterItem.getChildNodes();
        for (int j = 0; j < pagesList.getLength(); j++)
        {
            Node pageItem = pagesList.item(j);

            parsePage(chapter, pageItem);
        }

        chapter.pagePairs = (chapter.pages.size() + 1) / 2;
    }

    private void parsePage(ChapterData chapter, Node pageItem)
    {
        if (!pageItem.getNodeName().equals("page"))
        {
            return;
        }

        PageData page = new PageData(chapter.pages.size());
        chapter.pages.add(page);

        if (pageItem.hasAttributes())
        {
            NamedNodeMap pageAttributes = pageItem.getAttributes();
            Node n = pageAttributes.getNamedItem("id");
            if (n != null)
            {
                page.id = n.getTextContent();
                pagesByName.put(page.id, new PageRef(chapter.num, page.num));
            }
        }

        NodeList elementsList = pageItem.getChildNodes();
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            Node elementItem = elementsList.item(k);

            if (elementItem.getNodeName().equals("p"))
            {
                Paragraph p = new Paragraph(elementItem.getTextContent());
                page.elements.add(p);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();
                    parseParagraphAttributes(p, pageAttributes);
                }
            }
            else if (elementItem.getNodeName().equals("title"))
            {
                Paragraph p = new Paragraph(elementItem.getTextContent());
                p.alignment = 1;
                p.space = 4;
                p.underline = true;
                p.italics = true;

                page.elements.add(p);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();
                    parseParagraphAttributes(p, pageAttributes);
                }
            }
            else if (elementItem.getNodeName().equals("link"))
            {
                Link link = new Link(elementItem.getTextContent());
                page.elements.add(link);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();

                    parseLinkAttributes(link, pageAttributes);
                }
            }
            else if (elementItem.getNodeName().equals("space"))
            {
                Space s = new Space();
                page.elements.add(s);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();

                    parseSpaceAttributes(s, pageAttributes);
                }
            }
            else if (elementItem.getNodeName().equals("stack"))
            {
                Stack s = new Stack();
                page.elements.add(s);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();

                    parseStackAttributes(s, pageAttributes);
                }
            }
            else if (elementItem.getNodeName().equals("image"))
            {
                Image i = new Image();
                page.elements.add(i);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();

                    parseImageAttributes(i, pageAttributes);
                }
            }
        }
    }

    private void parseImageAttributes(Image i, NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("x");
        if (attr != null)
        {
            i.x = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("y");
        if (attr != null)
        {
            i.y = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("w");
        if (attr != null)
        {
            i.w = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("h");
        if (attr != null)
        {
            i.h = Ints.tryParse(attr.getTextContent());
        }
        attr = attributes.getNamedItem("tx");
        if (attr != null)
        {
            i.tx = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("ty");
        if (attr != null)
        {
            i.ty = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("tw");
        if (attr != null)
        {
            i.tw = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("th");
        if (attr != null)
        {
            i.th = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("src");
        if (attr != null)
        {
            i.textureLocation = new ResourceLocation(attr.getTextContent());
            i.textureLocationExpanded = new ResourceLocation(i.textureLocation.getResourceDomain(), "textures/" + i.textureLocation.getResourcePath() + ".png");
        }
    }

    private void parseStackAttributes(Stack s, NamedNodeMap attributes)
    {
        int meta = 0;
        int stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();

        Node attr = attributes.getNamedItem("meta");
        if (attr != null)
        {
            meta = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("count");
        if (attr != null)
        {
            stackSize = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("tag");
        if (attr != null)
        {
            try
            {
                tag = JsonToNBT.getTagFromJson(attr.getTextContent());
            }
            catch (NBTException e)
            {
                GuidebookMod.logger.warn("Invalid tag format: " + e.getMessage());
            }
        }

        attr = attributes.getNamedItem("item");
        if (attr != null)
        {
            String itemName = attr.getTextContent();

            Item item = Item.REGISTRY.getObject(new ResourceLocation(itemName));

            if (item != null)
            {
                s.stack = new ItemStack(item, stackSize, meta);
                s.stack.setTagCompound(tag);
            }
        }

        attr = attributes.getNamedItem("x");
        if (attr != null)
        {
            s.x = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("y");
        if (attr != null)
        {
            s.y = Ints.tryParse(attr.getTextContent());
        }
    }

    private void parseSpaceAttributes(Space s, NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("height");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                s.asPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            s.space = Ints.tryParse(t);
        }
    }

    private void parseLinkAttributes(Link link, NamedNodeMap attributes)
    {
        parseParagraphAttributes(link, attributes);

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();

            if (ref.indexOf(':') >= 0)
            {
                String[] parts = ref.split(":");
                link.target = new PageRef(parts[0], parts[1]);
            }
            else
            {
                link.target = new PageRef(ref, null);
            }
        }
    }

    private void parseParagraphAttributes(Paragraph p, NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("align");
        if (attr != null)
        {
            String a = attr.getTextContent();
            switch (a)
            {
                case "left":
                    p.alignment = 0;
                    break;
                case "center":
                    p.alignment = 1;
                    break;
                case "right":
                    p.alignment = 2;
                    break;
            }
        }

        attr = attributes.getNamedItem("indent");
        if (attr != null)
        {
            p.indent = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("space");
        if (attr != null)
        {
            p.space = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("bold");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                p.bold = true;
        }

        attr = attributes.getNamedItem("italics");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                p.italics = true;
        }

        attr = attributes.getNamedItem("underline");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                p.underline = true;
        }

        attr = attributes.getNamedItem("color");
        if (attr != null)
        {
            String c = attr.getTextContent();

            if (c.startsWith("#"))
                c = c.substring(1);

            try
            {
                if (c.length() <= 6)
                {
                    p.color = 0xFF000000 | Integer.parseInt(c, 16);
                }
                else
                {
                    p.color = Integer.parseInt(c, 16);
                }
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
    }

    public void drawCurrentPages(GuiGuidebook gui)
    {
        setScalingFactor();

        int guiWidth = gui.width;
        int guiHeight = gui.height;

        if(hasScale)
        {
            GlStateManager.pushMatrix();
            GlStateManager.scale(scalingFactor, scalingFactor, scalingFactor);

            guiWidth = sr2.scaledWidth;
            guiHeight = sr2.scaledHeight;
        }

        int left = guiWidth / 2 - pageWidth - innerMargin;
        int right = guiWidth / 2 + innerMargin;
        int top = (guiHeight - pageHeight) / 2 - 9;
        int bottom = top + pageHeight - 3;

        drawPage(gui, left, top, currentPair * 2);
        drawPage(gui, right, top, currentPair * 2 + 1);

        String cnt = "" + ((chapters.get(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (totalPairs * 2);
        addStringWrapping(gui, left, bottom, cnt, 0xFF000000, 1);

        if (hasScale)
        {
            GlStateManager.popMatrix();
        }
    }

    private void drawPage(GuiGuidebook gui, int left, int top, int page)
    {
        ChapterData ch = chapters.get(currentChapter);
        if (page >= ch.pages.size())
            return;

        PageData pg = ch.pages.get(page);

        for (IPageElement e : pg.elements)
        {
            top += e.apply(gui, left, top);
        }
    }

    private int addStringWrapping(GuiGuidebook gui, int left, int top, String s, int color, int align)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        if (align == 1)
        {
            left += (pageWidth - getSplitWidth(fontRenderer, s)) / 2;
        }
        else if (align == 2)
        {
            left += pageWidth - getSplitWidth(fontRenderer, s);
        }

        fontRenderer.drawSplitString(s, left, top, pageWidth, color);
        return fontRenderer.splitStringWidth(s, pageWidth);
    }

    private int getSplitWidth(FontRenderer fontRenderer, String s)
    {
        int height = fontRenderer.splitStringWidth(s, pageWidth);
        return height > fontRenderer.FONT_HEIGHT ? pageWidth : fontRenderer.getStringWidth(s);
    }

    private class PageRef
    {
        public int chapter;
        public int page;

        public boolean resolvedNames = false;
        public String chapterName;
        public String pageName;

        private PageRef(int chapter, int page)
        {
            this.chapter = chapter;
            this.page = page;
            resolvedNames = true;
        }

        private PageRef(String chapter, @Nullable String page)
        {
            this.chapterName = chapter;
            this.pageName = page;
        }

        public void resolve()
        {
            if (!resolvedNames)
            {
                if (chapterName != null)
                {
                    Integer ch = Ints.tryParse(chapterName);
                    if (ch != null)
                    {
                        chapter = ch;
                    }
                    else
                    {
                        chapter = chaptersByName.get(chapterName);
                    }

                    if (pageName != null)
                    {
                        Integer pg = Ints.tryParse(pageName);
                        if (pg != null)
                        {
                            page = pg;
                        }
                    }
                }
                else if (pageName != null)
                {
                    PageRef temp = pagesByName.get(pageName);
                    temp.resolve();
                    chapter = temp.chapter;
                    page = temp.page;
                }
            }
        }
    }

    private class ChapterData
    {
        public final int num;
        public String id;

        public final List<PageData> pages = Lists.newArrayList();

        public int pagePairs;
        public int startPair;

        private ChapterData(int num)
        {
            this.num = num;
        }
    }

    private class PageData
    {
        public final int num;
        public String id;

        public final List<IPageElement> elements = Lists.newArrayList();

        private PageData(int num)
        {
            this.num = num;
        }
    }

    private interface IPageElement
    {
        int apply(GuiGuidebook gui, int left, int top);

        default void findTextures(Set<ResourceLocation> textures)  {}
    }

    private class Paragraph implements IPageElement
    {
        public final String text;
        public int alignment = 0;
        public int color = 0xFF000000;
        public int indent = 0;
        public int space = 2;
        public boolean bold;
        public boolean italics;
        public boolean underline;

        public Paragraph(String text)
        {
            this.text = text;
        }

        @Override
        public int apply(GuiGuidebook gui, int left, int top)
        {
            String textWithFormat = text;
            if (bold) textWithFormat = TextFormatting.BOLD + textWithFormat;
            if (italics) textWithFormat = TextFormatting.ITALIC + textWithFormat;
            if (underline) textWithFormat = TextFormatting.UNDERLINE + textWithFormat;
            return addStringWrapping(gui, left + indent, top, textWithFormat, color, alignment) + space;
        }
    }

    private class Link extends Paragraph
    {
        public PageRef target;
        public int colorHover = 0xFF77cc66;

        public boolean isHovering;
        public Rectangle bounds;

        public Link(String text)
        {
            super(text);
            underline = true;
            color = 0xFF7766cc;
        }

        public Rectangle getBounds()
        {
            return bounds;
        }

        public void click()
        {
            navigateTo(target);
        }

        @Override
        public int apply(GuiGuidebook gui, int left, int top)
        {
            FontRenderer fontRenderer = gui.getFontRenderer();

            int height = fontRenderer.splitStringWidth(text, pageWidth);
            int width = height > fontRenderer.FONT_HEIGHT ? pageWidth : fontRenderer.getStringWidth(text);
            bounds = new Rectangle(left, top, width, height);

            return addStringWrapping(gui, left + indent, top, text, isHovering ? colorHover : color, alignment) + space;
        }
    }

    private class Space implements IPageElement
    {
        public boolean asPercent;
        public int space;

        public Space()
        {
        }

        @Override
        public int apply(GuiGuidebook gui, int left, int top)
        {
            return asPercent ? pageHeight * space / 100 : space;
        }
    }

    private class Stack implements IPageElement
    {
        public ItemStack stack;
        public int x = 0;
        public int y = 0;

        public Stack()
        {
        }

        @Override
        public int apply(GuiGuidebook gui, int left, int top)
        {
            StackRenderingHelper.renderItemStack(gui.getMesher(), gui.getRenderEngine(), left + x, top + y, stack, 0xFFFFFFFF);
            return 0;
        }
    }

    private class Image implements IPageElement
    {
        public ResourceLocation textureLocation;
        public ResourceLocation textureLocationExpanded;
        public int x = 0;
        public int y = 0;
        public int w = 0;
        public int h = 0;
        public int tx = 0;
        public int ty = 0;
        public int tw = 0;
        public int th = 0;

        public Image()
        {
        }

        @Override
        public int apply(GuiGuidebook gui, int left, int top)
        {
            if (w == 0 || h == 0)
            {
                TextureAtlasSprite tas = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(textureLocation.toString());
                if (w == 0) w = tas.getIconWidth();
                if (h == 0) h = tas.getIconHeight();
            }

            int sw = tw != 0 ? tw : w;
            int sh = th != 0 ? th : h;
            gui.getRenderEngine().bindTexture(textureLocationExpanded);

            drawImage(left+x, top+y, tx, ty, w, h, sw, sh);
            return 0;
        }

        private void drawImage(int x, int y, int tx, int ty, int w, int h, int sw, int sh)
        {
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            Gui.drawModalRectWithCustomSizedTexture(x, y, tx, ty, w, h, sw, sh);
        }

        @Override
        public void findTextures(Set<ResourceLocation> textures)
        {
            textures.add(textureLocation);
        }
    }
}
