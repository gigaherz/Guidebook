package gigaherz.guidebook.guidebook;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BookDocument
{
    private static final float DEFAULT_FONT_SIZE = 1.0f;

    private float fontSize;

    private final ResourceLocation bookLocation;
    private String bookName;
    private ResourceLocation bookCover;

    private List<ChapterData> chapters = Lists.newArrayList();

    private final Map<String, Integer> chaptersByName = Maps.newHashMap();
    private final Map<String, PageRef> pagesByName = Maps.newHashMap();

    private int totalPairs = 0;

    public BookDocument(ResourceLocation bookLocation)
    {
        this.bookLocation = bookLocation;
    }

    public ResourceLocation getBookLocation()
    {
        return bookLocation;
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

    public ChapterData getChapter(int i)
    {
        return chapters.get(i);
    }

    public int getTotalPairCount()
    {
        return totalPairs;
    }

    public float getFontSize()
    {
        return fontSize;
    }

    public int chapterCount()
    {
        return chapters.size();
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

    public void parseBook(InputStream stream)
    {
        try
        {
            chapters.clear();
            bookName = "";
            bookCover = null;
            totalPairs = 0;
            chaptersByName.clear();
            pagesByName.clear();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);

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
            initializeWithLoadError(e);
        }
    }

    public void initializeWithLoadError(Exception e)
    {
        ChapterData ch = new ChapterData(0);
        chapters.add(ch);

        PageData pg = new PageData(0);
        ch.pages.add(pg);

        pg.elements.add(new Paragraph("Error loading book:"));
        pg.elements.add(new Paragraph(TextFormatting.RED + e.toString()));
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
                chapter.pagesByName.put(page.id, page.num);
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

    public class PageRef
    {
        public int chapter;
        public int page;

        public boolean resolvedNames = false;
        public String chapterName;
        public String pageName;

        public PageRef(int chapter, int page)
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
                        else
                        {
                            page = chapters.get(chapter).pagesByName.get(pageName);
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

    public class ChapterData
    {
        public final int num;
        public String id;

        public final List<PageData> pages = Lists.newArrayList();
        public final Map<String, Integer> pagesByName = Maps.newHashMap();

        public int pagePairs;
        public int startPair;

        private ChapterData(int num)
        {
            this.num = num;
        }

        public int pairCount()
        {
            return pagePairs;
        }
    }

    public class PageData
    {
        public final int num;
        public String id;

        public final List<IPageElement> elements = Lists.newArrayList();

        private PageData(int num)
        {
            this.num = num;
        }
    }

    public interface IPageElement
    {
        int apply(IBookGraphics nav, int left, int top);

        default void findTextures(Set<ResourceLocation> textures)  {}
    }

    private interface IBoundedPageElement extends IPageElement
    {
        Rectangle getBounds();
    }

    public interface IClickablePageElement extends IBoundedPageElement
    {
        void click(IBookGraphics nav);
    }

    public interface IHoverPageElement extends IBoundedPageElement
    {
        void mouseOver(IBookGraphics info, int x, int y);
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
        public int apply(IBookGraphics nav, int left, int top)
        {
            String textWithFormat = text;
            if (bold) textWithFormat = TextFormatting.BOLD + textWithFormat;
            if (italics) textWithFormat = TextFormatting.ITALIC + textWithFormat;
            if (underline) textWithFormat = TextFormatting.UNDERLINE + textWithFormat;
            return nav.addStringWrapping(left + indent, top, textWithFormat, color, alignment) + space;
        }
    }

    private class Link extends Paragraph implements IClickablePageElement
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

        @Override
        public Rectangle getBounds()
        {
            return bounds;
        }

        @Override
        public void click(IBookGraphics nav)
        {
            nav.navigateTo(target);
        }

        @Override
        public int apply(IBookGraphics nav, int left, int top)
        {
            bounds = nav.getStringBounds(text, left, top);

            return nav.addStringWrapping(left + indent, top, text, isHovering ? colorHover : color, alignment) + space;
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
        public int apply(IBookGraphics nav, int left, int top)
        {
            return asPercent ? nav.getPageHeight() * space / 100 : space;
        }
    }

    private class Stack implements IHoverPageElement
    {
        public ItemStack stack;
        public int x = 0;
        public int y = 0;

        public Rectangle bounds;

        public Stack()
        {
        }

        @Override
        public int apply(IBookGraphics nav, int left, int top)
        {
            left +=x;
            top += y;
            int width = 16;
            int height = 16;
            bounds = new Rectangle(left, top, width, height);

            nav.drawItemStack(left, top, stack, 0xFFFFFFFF);
            return 0;
        }

        @Override
        public void mouseOver(IBookGraphics nav, int x, int y)
        {
            nav.drawTooltip(stack, x, y);
        }

        @Override
        public Rectangle getBounds()
        {
            return bounds;
        }
    }

    private class Image implements IPageElement
    {
        public ResourceLocation textureLocation;
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
        public int apply(IBookGraphics nav, int left, int top)
        {
            drawImage(nav, left, top);
            return 0;
        }

        private void drawImage(IBookGraphics nav, int left, int top)
        {
            nav.drawImage(textureLocation, left+x, top+y, tx, ty, w, h, tw, th);
        }

        @Override
        public void findTextures(Set<ResourceLocation> textures)
        {
            textures.add(textureLocation);
        }
    }
}
