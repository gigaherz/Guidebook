package gigaherz.guidebook.guidebook;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import gigaherz.common.client.StackRenderingHelper;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class BookRenderer
{
    public static final int BOOK_WIDTH = 276;
    public static final int BOOK_HEIGHT = 198;
    public static final int INNER_MARGIN = 22;
    public static final int OUTER_MARGIN = 10;
    public static final int VERTICAL_MARGIN = 18;
    public static final int PAGE_WIDTH = BOOK_WIDTH / 2 - INNER_MARGIN - OUTER_MARGIN;
    public static final int PAGE_HEIGHT = BOOK_HEIGHT - VERTICAL_MARGIN;

    private final ResourceLocation bookLocation;
    private final GuiGuidebook gui;

    private List<ChapterData> chapters = Lists.newArrayList();

    private Map<String, Integer> chaptersByName = Maps.newHashMap();
    private Map<String, PageRef> pagesByName = Maps.newHashMap();

    private int totalPairs = 0;

    private int currentChapter = 0;
    private int currentPair = 0;

    java.util.Stack<PageRef> history = new java.util.Stack<>();

    BookRenderer(ResourceLocation bookLocation, GuiGuidebook gui)
    {
        this.bookLocation = bookLocation;
        this.gui = gui;
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

    public BookRenderer parseBook()
    {
        try
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(bookLocation);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(res.getInputStream());

            doc.getDocumentElement().normalize();

            NodeList chaptersList = doc.getChildNodes().item(0).getChildNodes();
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

        return this;
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
                Title title = new Title(elementItem.getTextContent());
                page.elements.add(title);

                if (elementItem.hasAttributes())
                {
                    NamedNodeMap pageAttributes = elementItem.getAttributes();
                    parseParagraphAttributes(title, pageAttributes);
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
        }
    }

    private void parseStackAttributes(Stack s, NamedNodeMap pageAttributes)
    {
        int meta = 0;
        int stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();

        Node attr = pageAttributes.getNamedItem("meta");
        if (attr != null)
        {
            meta = Ints.tryParse(attr.getTextContent());
        }

        attr = pageAttributes.getNamedItem("count");
        if (attr != null)
        {
            stackSize = Ints.tryParse(attr.getTextContent());
        }

        attr = pageAttributes.getNamedItem("tag");
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

        attr = pageAttributes.getNamedItem("item");
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

        attr = pageAttributes.getNamedItem("x");
        if (attr != null)
        {
            s.x = Ints.tryParse(attr.getTextContent());
        }

        attr = pageAttributes.getNamedItem("y");
        if (attr != null)
        {
            s.y = Ints.tryParse(attr.getTextContent());
        }
    }

    private void parseSpaceAttributes(Space s, NamedNodeMap pageAttributes)
    {
        Node attr = pageAttributes.getNamedItem("height");
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

    private void parseLinkAttributes(Link link, NamedNodeMap pageAttributes)
    {
        parseParagraphAttributes(link, pageAttributes);

        Node attr = pageAttributes.getNamedItem("ref");
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

    private void parseParagraphAttributes(Paragraph p, NamedNodeMap pageAttributes)
    {
        Node attr = pageAttributes.getNamedItem("align");
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

        attr = pageAttributes.getNamedItem("indent");
        if (attr != null)
        {
            p.indent = Ints.tryParse(attr.getTextContent());
        }

        attr = pageAttributes.getNamedItem("space");
        if (attr != null)
        {
            p.space = Ints.tryParse(attr.getTextContent());
        }

        attr = pageAttributes.getNamedItem("color");
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

    public void drawCurrentPages()
    {
        int left = gui.width / 2 - PAGE_WIDTH - INNER_MARGIN;
        int right = gui.width / 2 + INNER_MARGIN;
        int top = (gui.height - PAGE_HEIGHT) / 2 - 9;
        int bottom = top + PAGE_HEIGHT;

        drawPage(left, top, currentPair * 2);
        drawPage(right, top, currentPair * 2 + 1);

        String cnt = "" + ((chapters.get(currentChapter).startPair + currentPair) * 2 + 1) + "/" + (totalPairs * 2);
        addStringWrapping(left, bottom, cnt, 0xFF000000, 1);
    }

    private void drawPage(int left, int top, int page)
    {
        ChapterData ch = chapters.get(currentChapter);
        if (page >= ch.pages.size())
            return;

        PageData pg = ch.pages.get(page);

        for (IPageElement e : pg.elements)
        {
            top += e.apply(left, top);
        }
    }

    private int addStringWrapping(int left, int top, String s, int color, int align)
    {
        FontRenderer fontRenderer = gui.getFontRenderer();

        if (align == 1)
        {
            left += (PAGE_WIDTH - fontRenderer.getStringWidth(s)) / 2;
        }
        else if (align == 2)
        {
            left += PAGE_WIDTH - fontRenderer.getStringWidth(s);
        }

        fontRenderer.drawSplitString(s, left, top, PAGE_WIDTH, color);
        return fontRenderer.splitStringWidth(s, PAGE_WIDTH);
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
        int apply(int left, int top);
    }

    private class Paragraph implements IPageElement
    {
        public final String text;
        public int alignment = 0;
        public int color = 0xFF000000;
        public int indent = 0;
        public int space = 2;

        public Paragraph(String text)
        {
            this.text = text;
        }

        @Override
        public int apply(int left, int top)
        {
            return addStringWrapping(left + indent, top, text, color, alignment) + space;
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
            super(TextFormatting.UNDERLINE + text);
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
        public int apply(int left, int top)
        {
            FontRenderer fontRenderer = gui.getFontRenderer();

            int height = fontRenderer.splitStringWidth(text, PAGE_WIDTH);
            int width = height > fontRenderer.FONT_HEIGHT ? PAGE_WIDTH : fontRenderer.getStringWidth(text);
            bounds = new Rectangle(left, top, width, height);

            return addStringWrapping(left + indent, top, text, isHovering ? colorHover : color, alignment) + space;
        }
    }

    private class Title extends Paragraph
    {
        public Title(String text)
        {
            super(TextFormatting.ITALIC + "" + TextFormatting.UNDERLINE + text);
            alignment = 1;
            space = 4;
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
        public int apply(int left, int top)
        {
            return asPercent ? PAGE_HEIGHT * space / 100 : space;
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
        public int apply(int left, int top)
        {
            StackRenderingHelper.renderItemStack(gui.getMesher(), gui.getRenderEngine(), left + x, top + y, stack, 0xFFFFFFFF);
            return 0;
        }
    }
}
