package gigaherz.guidebook.guidebook;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import gigaherz.guidebook.guidebook.elements.*;
import gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import gigaherz.guidebook.guidebook.templates.TemplateElement;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
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

    private float fontSize = 1.0f;

    private final ResourceLocation bookLocation;
    private String bookName;
    private ResourceLocation bookCover;

    final List<ChapterData> chapters = Lists.newArrayList();

    final Map<String, Integer> chaptersByName = Maps.newHashMap();
    final Map<String, PageRef> pagesByName = Maps.newHashMap();

    private final Map<String, TemplateDefinition> templates = Maps.newHashMap();

    private int totalPairs = 0;
    private IBookGraphics rendering;

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

    public void initializeWithLoadError(Exception e)
    {
        ChapterData ch = new ChapterData(0);
        chapters.add(ch);

        PageData pg = new PageData(0);
        ch.pages.add(pg);

        pg.elements.add(new Paragraph("Error loading book:"));
        pg.elements.add(new Paragraph(TextFormatting.RED + e.toString()));
    }

    public void parseBook(InputStream stream)
    {
        try
        {
            chapters.clear();
            bookName = "";
            bookCover = null;
            totalPairs = 0;
            fontSize = DEFAULT_FONT_SIZE;
            chaptersByName.clear();
            pagesByName.clear();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);

            doc.getDocumentElement().normalize();

            Node root = doc.getChildNodes().item(0);

            if (root.hasAttributes())
            {
                NamedNodeMap attributes = root.getAttributes();
                Node n = attributes.getNamedItem("title");
                if (n != null)
                {
                    bookName = n.getTextContent();
                }
                n = attributes.getNamedItem("cover");
                if (n != null)
                {
                    bookCover = new ResourceLocation(n.getTextContent());
                }
                n = attributes.getNamedItem("fontSize");
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

                String nodeName = chapterItem.getNodeName();
                if (nodeName.equals("template"))
                {
                    parseTemplateDefinition(chapterItem, templates);
                }
                else if (nodeName.equals("include"))
                {
                    NamedNodeMap attributes = chapterItem.getAttributes();
                    Node n = attributes.getNamedItem("ref");
                    TemplateLibrary tpl = TemplateLibrary.get(n.getTextContent());
                    templates.putAll(tpl.templates);
                }
                else if (nodeName.equals("chapter"))
                {
                    parseChapter(chapterItem);
                }
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

    private static void parseTemplateDefinition(Node templateItem, Map<String, TemplateDefinition> templates)
    {
        if (!templateItem.hasAttributes())
            return; // TODO: Throw error

        TemplateDefinition page = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null)
            return;

        templates.put(n.getTextContent(), page);

        parseChildElements(templateItem, page.elements, templates);

        attributes.removeNamedItem("id");
        page.attributes = attributes;
    }

    private void parseChapter(Node chapterItem)
    {
        ChapterData chapter = new ChapterData(chapters.size());
        chapters.add(chapter);

        if (chapterItem.hasAttributes())
        {
            NamedNodeMap attributes = chapterItem.getAttributes();
            Node n = attributes.getNamedItem("id");
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

            String nodeName = pageItem.getNodeName();

            if (nodeName.equals("page"))
            {
                parsePage(chapter, pageItem);
            }
        }

        chapter.pagePairs = (chapter.pages.size() + 1) / 2;
    }

    private void parsePage(ChapterData chapter, Node pageItem)
    {
        PageData page = new PageData(chapter.pages.size());
        chapter.pages.add(page);

        if (pageItem.hasAttributes())
        {
            NamedNodeMap attributes = pageItem.getAttributes();
            Node n = attributes.getNamedItem("id");
            if (n != null)
            {
                page.id = n.getTextContent();
                pagesByName.put(page.id, new PageRef(chapter.num, page.num));
                chapter.pagesByName.put(page.id, page.num);
            }
        }

        parseChildElements(pageItem, page.elements, templates);
    }

    public static void parseChildElements(Node pageItem, List<IPageElement> elements, Map<String, TemplateDefinition> templates)
    {
        NodeList elementsList = pageItem.getChildNodes();
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            Node elementItem = elementsList.item(k);

            String nodeName = elementItem.getNodeName();
            if (nodeName.equals("p"))
            {
                Paragraph p = new Paragraph(elementItem.getTextContent());
                elements.add(p);

                if (elementItem.hasAttributes())
                {
                    p.parse(elementItem.getAttributes());
                }
            }
            else if (nodeName.equals("title"))
            {
                Paragraph p = new Paragraph(elementItem.getTextContent());
                p.alignment = 1;
                p.space = 4;
                p.underline = true;
                p.italics = true;

                elements.add(p);

                if (elementItem.hasAttributes())
                {
                    p.parse(elementItem.getAttributes());
                }
            }
            else if (nodeName.equals("link"))
            {
                Link link = new Link(elementItem.getTextContent());
                elements.add(link);

                if (elementItem.hasAttributes())
                {
                    link.parse(elementItem.getAttributes());
                }
            }
            else if (nodeName.equals("space")
                    || nodeName.equals("group"))
            {
                Space s = new Space();
                elements.add(s);

                if (elementItem.hasAttributes())
                {
                    s.parse(elementItem.getAttributes());
                }

                List<IPageElement> elementList = Lists.newArrayList();

                parseChildElements(elementItem, elementList, templates);

                s.innerElements.addAll(elementList);
            }
            else if (nodeName.equals("stack"))
            {
                Stack s = new Stack();
                elements.add(s);

                if (elementItem.hasAttributes())
                {
                    s.parse(elementItem.getAttributes());
                }
            }
            else if (nodeName.equals("image"))
            {
                Image i = new Image();
                elements.add(i);

                if (elementItem.hasAttributes())
                {
                    i.parse(elementItem.getAttributes());
                }
            }
            else if (nodeName.equals("element"))
            {
                TemplateElement i = new TemplateElement();
                elements.add(i);

                if (elementItem.hasAttributes())
                {
                    i.parse(elementItem.getAttributes());
                }
            }
            else if (templates.containsKey(nodeName))
            {
                TemplateDefinition tDef = templates.get(nodeName);

                Template t = new Template();
                t.parse(tDef.attributes);

                elements.add(t);

                if (elementItem.hasAttributes())
                {
                    t.parse(elementItem.getAttributes());
                }

                List<IPageElement> elementList = Lists.newArrayList();

                parseChildElements(elementItem, elementList, templates);

                List<IPageElement> effectiveList = tDef.applyTemplate(elementList);

                t.innerElements.addAll(effectiveList);
            }
        }
    }

    public void setRendering(IBookGraphics rendering)
    {
        this.rendering = rendering;
    }

    @Nullable
    public IBookGraphics getRendering()
    {
        return rendering;
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
}
