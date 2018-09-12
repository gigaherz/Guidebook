package gigaherz.guidebook.guidebook;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.conditions.ConditionManager;
import gigaherz.guidebook.guidebook.drawing.*;
import gigaherz.guidebook.guidebook.elements.*;
import gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import gigaherz.guidebook.guidebook.templates.TemplateElement;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import joptsimple.internal.Strings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class BookDocument implements IConditionSource
{
    private static final float DEFAULT_FONT_SIZE = 1.0f;

    private float fontSize = 1.0f;

    public SectionRef home = new SectionRef(0, 0);

    private final ResourceLocation bookLocation;
    private String bookName;
    private ResourceLocation bookCover;

    final List<ChapterData> chapters = Lists.newArrayList();
    private Table<Item, Integer, SectionRef> stackLinks = HashBasedTable.create();

    final Map<String, Integer> chaptersByName = Maps.newHashMap();
    final Map<String, SectionRef> sectionsByName = Maps.newHashMap();

    private final Map<String, TemplateDefinition> templates = Maps.newHashMap();
    private final Map<String, Predicate<ConditionContext>> conditions = Maps.newHashMap();

    private IBookGraphics renderingManager;

    private static final Map<ResourceLocation, ElementFactory> customElements = Maps.newHashMap();

    public static void registerCustomElement(ResourceLocation location, ElementFactory factory)
    {
        if (customElements.containsKey(location))
            throw new RuntimeException("Can not register two custom element factories with the same id.");

        customElements.put(location, factory);
    }

    static
    {
        registerCustomElement(new ResourceLocation("minecraft:recipe"), ElementRecipe::new);
    }

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

    @Nullable
    public IBookGraphics getRendering()
    {
        return renderingManager;
    }

    public void setRendering(IBookGraphics rendering)
    {
        this.renderingManager = rendering;
    }

    public ChapterData getChapter(int i)
    {
        return chapters.get(i);
    }

    @Nullable
    public SectionRef getStackLink(ItemStack stack)
    {
        Item item = stack.getItem();
        int damage = stack.getItemDamage();

        if (stackLinks.contains(item, damage))
        {
            return stackLinks.get(item, damage);
        }
        else if (stackLinks.contains(item, -1))
        {
            return stackLinks.get(item, -1);
        }
        return null;
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
            for (PageData page : chapter.sections)
            {
                for (Element element : page.elements)
                {
                    element.findTextures(textures);
                }
            }
        }
    }

    public void initializeWithLoadError(String error)
    {
        ChapterData ch = new ChapterData(0);
        chapters.add(ch);

        PageData pg = new PageData(new SectionRef(0,0));
        ch.sections.add(pg);

        pg.elements.add(ElementParagraph.of("Error loading book:"));
        pg.elements.add(ElementParagraph.of(TextFormatting.RED + error));
    }

    public boolean parseBook(InputStream stream)
    {
        try
        {
            chapters.clear();
            bookName = "";
            bookCover = null;
            fontSize = DEFAULT_FONT_SIZE;
            chaptersByName.clear();

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
                n = attributes.getNamedItem("home");
                if (n != null)
                {
                    String ref = n.getTextContent();
                    home = SectionRef.fromString(ref);
                }
                n = attributes.getNamedItem("dependencies");
                if (n != null)
                {
                    for (String s : n.getTextContent().split(","))
                    {
                        if (!Loader.isModLoaded(s))
                        {
                            initializeWithLoadError("Dependency not loaded: " + s);
                            return false;
                        }
                    }
                }
            }

            int chapterNumber = 0;
            NodeList firstLevel = root.getChildNodes();
            for (int i = 0; i < firstLevel.getLength(); i++)
            {
                Node firstLevelNode = firstLevel.item(i);

                String nodeName = firstLevelNode.getNodeName();
                if (nodeName.equals("template"))
                {
                    parseTemplateDefinition(firstLevelNode, templates);
                }
                else if (nodeName.equals("include"))
                {
                    NamedNodeMap attributes = firstLevelNode.getAttributes();
                    Node n = attributes.getNamedItem("ref");
                    TemplateLibrary tpl = TemplateLibrary.get(n.getTextContent());
                    templates.putAll(tpl.templates);
                }
                else if (nodeName.equals("chapter"))
                {
                    parseChapter(chapterNumber++, firstLevelNode);
                }
                else if (nodeName.equals("stack-links"))
                {
                    parseStackLinks(firstLevelNode);
                }
                else if (nodeName.equals("conditions"))
                {
                    parseConditions(firstLevelNode);
                }
            }
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            initializeWithLoadError(e.toString());
        }
        return true;
    }

    private void parseConditions(Node node)
    {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node condition = children.item(i);
            if (condition.getNodeType() != Node.ELEMENT_NODE)
                continue;

            NamedNodeMap attributes = condition.getAttributes();
            if (attributes == null)
                continue;

            Node conditionName = attributes.getNamedItem("name");
            String name = conditionName != null ? conditionName.getTextContent() : null;

            if (Strings.isNullOrEmpty(name))
            {
                throw new BookParsingException("Condition node found without a name attribute");
            }

            Predicate<ConditionContext> displayCondition = parseSingleCondition(this, condition);

            conditions.put(name, displayCondition);
        }
    }

    public static List<Predicate<ConditionContext>> parseChildConditions(BookDocument context, Node node)
    {
        List<Predicate<ConditionContext>> conditions = Lists.newArrayList();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node condition = children.item(i);
            if (condition.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Predicate<ConditionContext> displayCondition = parseSingleCondition(context, condition);

            conditions.add(displayCondition);
        }
        return conditions;
    }

    private static Predicate<ConditionContext> parseSingleCondition(BookDocument context, Node condition)
    {
        Predicate<ConditionContext> displayCondition;
        try
        {
            displayCondition = ConditionManager.parseCondition(context, condition);
            if (displayCondition == null)
            {
                throw new BookParsingException("Condition not found");
            }
        }
        catch (Exception e)
        {
            throw new BookParsingException("Exception parsing condition", e);
        }
        return displayCondition;
    }

    private void parseTemplateDefinition(Node templateItem, Map<String, TemplateDefinition> templates)
    {
        if (!templateItem.hasAttributes())
            return; // TODO: Throw error

        TemplateDefinition page = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null)
            return;

        templates.put(n.getTextContent(), page);

        parseChildElements(this, templateItem, page.elements, templates, true);

        attributes.removeNamedItem("id");
        page.attributes = attributes;
    }

    private void parseChapter(int chapterNumber, Node chapterItem)
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

            n = attributes.getNamedItem("condition");
            if (n != null)
            {
                chapter.condition = conditions.get(n.getTextContent());
            }
        }

        int sectionNumber = 0;
        NodeList pagesList = chapterItem.getChildNodes();
        for (int j = 0; j < pagesList.getLength(); j++)
        {
            Node pageItem = pagesList.item(j);

            String nodeName = pageItem.getNodeName();

            if (nodeName.equals("page"))
            {
                parsePage(chapter, new SectionRef(chapterNumber, sectionNumber++), pageItem);
            }
            else if (nodeName.equals("section"))
            {
                parseSection(chapter, new SectionRef(chapterNumber, sectionNumber++), pageItem);
            }
        }
    }

    private void parsePage(ChapterData chapter, SectionRef ref, Node pageItem)
    {
        PageData page = new PageData(ref);
        parseSection(chapter, pageItem, page);
    }

    private void parseSection(ChapterData chapter, SectionRef ref, Node pageItem)
    {
        PageData page = new PageGroup(ref);
        parseSection(chapter, pageItem, page);
    }

    private void parseSection(ChapterData chapter, Node pageItem, PageData page)
    {
        int num = chapter.sections.size();
        chapter.sections.add(page);

        if (pageItem.hasAttributes())
        {
            NamedNodeMap attributes = pageItem.getAttributes();
            Node n = attributes.getNamedItem("id");
            if (n != null)
            {
                page.id = n.getTextContent();
                sectionsByName.put(page.id, new SectionRef(chapter.num, num));
                chapter.sectionsByName.put(page.id, num);
            }

            n = attributes.getNamedItem("condition");
            if (n != null)
            {
                page.condition = conditions.get(n.getTextContent());
            }
        }

        parseChildElements(this, pageItem, page.elements, templates, true);
    }


    public static void parseChildElements(IConditionSource book, Node pageItem, List<Element> elements, Map<String, TemplateDefinition> templates, boolean generateParagraphs)
    {
        NodeList elementsList = pageItem.getChildNodes();
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            Node elementItem = elementsList.item(k);

            Element parsedElement = null;

            String nodeName = elementItem.getNodeName();
            ResourceLocation nodeLoc = new ResourceLocation(nodeName);

            if (nodeName.equals("section-break"))
            {
                parsedElement = new ElementBreak();
            }
            else if (nodeName.equals("p") || nodeName.equals("title"))
            {
                ElementParagraph p = new ElementParagraph();
                if (nodeName.equals("title"))
                {
                    p.alignment = ElementParagraph.ALIGN_CENTER;
                    p.space = 4;
                }

                NodeList childList = elementItem.getChildNodes();
                int l = childList.getLength();
                int firstText = -1;
                int lastText = -1;
                for (int q = 0; q < l; q++)
                {
                    Node childNode = childList.item(q);
                    short type = childNode.getNodeType();
                    if (type == Node.TEXT_NODE || type == Node.ELEMENT_NODE)
                    {
                        if (firstText < 0)
                            firstText = q;
                        lastText = q;
                    }
                }
                for (int q = 0; q < l; q++)
                {
                    Node childNode = childList.item(q);
                    if (childNode.getNodeType() == Node.TEXT_NODE)
                    {
                        String st = ElementSpan.compactString(childNode.getTextContent(), q == firstText, q == lastText);
                        if (!Strings.isNullOrEmpty(st))
                        {
                            // Trimming already performed above
                            ElementSpan s = new ElementSpan(st, false, false);

                            if (elementItem.hasAttributes())
                            {
                                s.parse(book, elementItem.getAttributes());
                            }

                            p.inlines.add(s);
                        }
                    }
                    else
                    {
                        Element parsedChild = parseParagraphElement(book, childNode, childNode.getNodeName());

                        if (parsedChild == null)
                        {
                            GuidebookMod.logger.warn("Unrecognized tag: {}", childNode.getNodeName());
                        }
                        else
                        {
                            p.inlines.add(parsedChild);
                        }
                    }
                }

                if (elementItem.hasAttributes())
                {
                    p.parse(book, elementItem.getAttributes());
                }

                parsedElement = p;
            }
            else if (nodeName.equals("space")
                    || nodeName.equals("group"))
            {
                ElementPanel s = new ElementPanel();

                if (elementItem.hasAttributes())
                {
                    s.parse(book, elementItem.getAttributes());
                }

                if (elementItem.hasChildNodes())
                {
                    List<Element> elementList = Lists.newArrayList();

                    parseChildElements(book, elementItem, elementList, templates, true);

                    s.innerElements.addAll(elementList);
                }

                parsedElement = s;
            }
            else if (customElements.containsKey(nodeLoc))
            {
                ElementFactory factory = customElements.get(nodeLoc);

                Element t = factory.newInstance();

                if (elementItem.hasAttributes())
                {
                    t.parse(book, elementItem.getAttributes());
                }

                if (elementItem.hasChildNodes())
                {
                    t.parseChildNodes(book, elementItem);
                }

                parsedElement = t;
            }
            else if (templates.containsKey(nodeName))
            {
                TemplateDefinition tDef = templates.get(nodeName);

                ElementPanel t = new ElementPanel();
                t.parse(book, tDef.attributes);

                if (elementItem.hasAttributes())
                {
                    t.parse(book, elementItem.getAttributes());
                }

                List<Element> elementList = Lists.newArrayList();

                parseChildElements(book, elementItem, elementList, templates, false);

                if(elementList.isEmpty())
                {
                    if(!elementItem.getTextContent().trim().isEmpty())
                    {
                        elementList.add(new ElementSpan(elementItem.getTextContent(), true, true));
                    }
                }

                List<Element> effectiveList = tDef.applyTemplate(book, elementList);

                t.innerElements.addAll(effectiveList);

                parsedElement = t;
            }
            else if (elementItem.getNodeType() == Node.TEXT_NODE)
            {
                // Ignore? Generate paragraph?
                // Ignore for now.
            }
            else if (elementItem.getNodeType() == Node.COMMENT_NODE)
            {
                // Ignore.
            }
            else
            {
                parsedElement = parseParagraphElement(book, elementItem, nodeName);

                if (parsedElement == null)
                {
                    GuidebookMod.logger.warn("Unrecognized tag: {}", nodeName);
                }
            }

            if (parsedElement != null)
            {
                if (!parsedElement.supportsPageLevel() && generateParagraphs)
                {
                    ElementParagraph p = new ElementParagraph();

                    if (elementItem.hasAttributes())
                    {
                        p.parse(book, elementItem.getAttributes());
                        parsedElement.parse(book, elementItem.getAttributes());
                    }

                    p.inlines.add(parsedElement);

                    parsedElement = p;
                }

                elements.add(parsedElement);
            }
        }
    }

    @Nullable
    public static Element parseParagraphElement(IConditionSource book, Node elementItem, String nodeName)
    {
        Element parsedElement = null;
        if (nodeName.equals("span"))
        {
            ElementSpan link = new ElementSpan(elementItem.getTextContent(), true, true);

            if (elementItem.hasAttributes())
            {
                link.parse(book, elementItem.getAttributes());
            }

            parsedElement = link;
        }
        else if (nodeName.equals("link"))
        {
            ElementLink link = new ElementLink(elementItem.getTextContent());

            if (elementItem.hasAttributes())
            {
                link.parse(book, elementItem.getAttributes());
            }

            parsedElement = link;
        }
        else if (nodeName.equals("stack"))
        {
            ElementStack s = new ElementStack();

            if (elementItem.hasAttributes())
            {
                s.parse(book, elementItem.getAttributes());
            }

            parsedElement = s;
        }
        else if (nodeName.equals("image"))
        {
            ElementImage i = new ElementImage();

            if (elementItem.hasAttributes())
            {
                i.parse(book, elementItem.getAttributes());
            }

            parsedElement = i;
        }
        else if (nodeName.equals("element"))
        {
            TemplateElement i = new TemplateElement();

            if (elementItem.hasAttributes())
            {
                i.parse(book, elementItem.getAttributes());
            }

            parsedElement = i;
        }
        return parsedElement;
    }

    private void parseStackLinks(Node refsItem)
    {
        NodeList refsList = refsItem.getChildNodes();
        for (int j = 0; j < refsList.getLength(); j++)
        {
            Node refItem = refsList.item(j);
            String nodeName = refItem.getNodeName();

            if (nodeName.equals("stack"))
            {
                if (refItem.hasAttributes())
                {
                    Node item_node = refItem.getAttributes().getNamedItem("item"); //get item
                    if (item_node != null)
                    {
                        Item item = Item.REGISTRY.getObject(new ResourceLocation(item_node.getTextContent()));
                        if (item != null)
                        {

                            int damage_value = 0;
                            Node meta = refItem.getAttributes().getNamedItem("meta");
                            if (meta != null)
                            {
                                // meta="*" -> wildcard
                                if (meta.getTextContent().equals("*"))
                                    damage_value = -1;
                                else
                                    damage_value = Ints.tryParse(meta.getTextContent());
                            }

                            String ref = refItem.getTextContent();
                            stackLinks.put(item, damage_value, SectionRef.fromString(ref));
                        }
                    }
                }
            }
        }
    }

    public Predicate<ConditionContext> getCondition(String name)
    {
        return conditions.get(name);
    }

    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean anyChanged = false;
        for (ChapterData chapter : chapters)
        {
            anyChanged |= chapter.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    public class ChapterData
    {
        public final int num;
        public String id;
        public Predicate<ConditionContext> condition;
        public boolean conditionResult;

        public final List<PageData> sections = Lists.newArrayList();
        public final Map<String, Integer> sectionsByName = Maps.newHashMap();

        private ChapterData(int num)
        {
            this.num = num;
        }

        public boolean reevaluateConditions(ConditionContext ctx)
        {
            boolean oldValue = conditionResult;
            conditionResult = condition == null || condition.test(ctx);

            boolean anyChanged = conditionResult != oldValue;
            for (PageData section : sections)
            {
                anyChanged |= section.reevaluateConditions(ctx);
            }

            return anyChanged;
        }

        public void reflow(IBookGraphics rendering, VisualChapter ch, Size pageSize)
        {
            for (BookDocument.PageData section : sections)
            {
                if (!section.conditionResult || section.isEmpty())
                    continue;

                if (!com.google.common.base.Strings.isNullOrEmpty(section.id))
                    ch.pagesByName.put(section.id, ch.pages.size());

                ch.pages.addAll(section.reflow(rendering, pageSize));
            }
        }

        public boolean isEmpty()
        {
            return sections.stream().noneMatch(s -> s.conditionResult && !s.isEmpty());
        }
    }

    public class PageData
    {
        public final SectionRef ref;
        public String id;
        public Predicate<ConditionContext> condition;
        public boolean conditionResult;

        public final List<Element> elements = Lists.newArrayList();

        public PageData(SectionRef ref)
        {
            this.ref = ref;
        }

        public List<VisualPage> reflow(IBookGraphics rendering, Size pageSize)
        {
            VisualPage page = new VisualPage(ref);
            Rect pageBounds = new Rect(new Point(), pageSize);

            int top = 0;
            for (Element element : elements)
            {
                if (element.conditionResult)
                    top = element.reflow(page.children, rendering, new Rect(new Point(0, top), pageSize), pageBounds);
            }

            return Collections.singletonList(page);
        }

        public boolean reevaluateConditions(ConditionContext ctx)
        {
            boolean oldValue = conditionResult;
            conditionResult = condition == null || condition.test(ctx);

            boolean anyChanged = conditionResult != oldValue;
            for (Element element : elements)
            {
                anyChanged |= element.reevaluateConditions(ctx);
            }

            return anyChanged;
        }

        public boolean isEmpty()
        {
            return elements.stream().noneMatch(e -> e.conditionResult);
        }
    }

    /**
     * This class represents a group of pages clearly delimited by start/end of chapters, sections, or explicit section braks.
     * Example:
     * <book>
     * <chapter>
     * <section>
     * Group 1
     * </section>
     * <section>
     * Group 2
     * </section>
     * </chapter>
     * <chapter>
     * Group 3
     * <page_break />
     * Group 4
     * </chapter>
     * </book>
     */
    public class PageGroup extends PageData
    {
        public PageGroup(SectionRef ref)
        {
            super(ref);
        }

        @Override
        public List<VisualPage> reflow(IBookGraphics rendering, Size pageSize)
        {
            List<VisualPage> pages = Lists.newArrayList();

            VisualPage page = new VisualPage(ref);
            Rect pageBounds = new Rect(new Point(0, 0), pageSize);

            int top = pageBounds.position.y;
            for (Element element : elements)
            {
                if (element.conditionResult)
                    top = element.reflow(page.children, rendering, new Rect(new Point(pageBounds.position.x, top), pageBounds.size), pageBounds);
            }

            boolean needsRepagination = false;
            for (VisualElement child : page.children)
            {
                if (child instanceof VisualPageBreak || (child.position.y + child.size.height > (pageBounds.position.y + pageBounds.size.height)))
                {
                    needsRepagination = true;
                    break;
                }
            }

            if (needsRepagination)
            {
                VisualPage page2 = new VisualPage(ref);

                int offsetY = 0;
                boolean pageBreakRequired = false;
                for (VisualElement child : page.children)
                {
                    int cpy = child.position.y + offsetY;
                    if (pageBreakRequired || (cpy + child.size.height > (pageBounds.position.y + pageBounds.size.height)
                            && child.position.y > pageBounds.position.y))
                    {
                        pages.add(page2);
                        page2 = new VisualPage(ref);

                        offsetY = pageBounds.position.y - child.position.y;
                        pageBreakRequired = false;
                    }

                    if (child instanceof VisualPageBreak)
                    {
                        pageBreakRequired = true;
                    }
                    else
                    {
                        child.position = new Point(
                                child.position.x,
                                child.position.y + offsetY);
                        page2.children.add(child);
                    }
                }

                pages.add(page2);
            }
            else
            {
                pages.add(page);
            }

            return pages;
        }
    }
}
