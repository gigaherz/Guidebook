package dev.gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionManager;
import dev.gigaherz.guidebook.guidebook.drawing.*;
import dev.gigaherz.guidebook.guidebook.elements.*;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.templates.TemplateElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BookDocument
{
    private static final float DEFAULT_FONT_SIZE = 1.0f;

    private float fontSize = 1.0f;

    public SectionRef home = new SectionRef(0, 0);

    private final ResourceLocation bookLocation;
    private String bookName;
    private ResourceLocation bookCover;
    private ModelResourceLocation bookModel;

    final List<ChapterData> chapters = Lists.newArrayList();
    private Map<Item, SectionRef> stackLinks = Maps.newHashMap();

    final Map<String, Integer> chaptersByName = Maps.newHashMap();
    final Map<String, SectionRef> sectionsByName = Maps.newHashMap();

    private final Map<String, TemplateDefinition> templates = Maps.newHashMap();
    private final Map<String, Predicate<ConditionContext>> conditions = Maps.newHashMap();

    private IBookGraphics renderingManager;

    private static final Map<ResourceLocation, ElementFactory> customElements = Maps.newHashMap();
    private ResourceLocation background;

    public static void registerCustomElement(ResourceLocation location, ElementFactory factory)
    {
        if (customElements.containsKey(location))
            throw new RuntimeException("Can not register two custom element factories with the same id.");

        customElements.put(location, factory);
    }

    static
    {
        registerCustomElement(ResourceLocation.withDefaultNamespace("recipe"), ElementRecipe::new);
    }

    public BookDocument(ResourceLocation bookLocation)
    {
        this.bookLocation = bookLocation;
    }

    public ResourceLocation getLocation()
    {
        return bookLocation;
    }

    @Nullable
    public String getName()
    {
        return bookName;
    }

    @Nullable
    public ResourceLocation getCover()
    {
        return bookCover;
    }

    @Nullable
    public ModelResourceLocation getModel()
    {
        return bookModel;
    }

    @Nullable
    public ResourceLocation getBackground()
    {
        return background;
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
        return stackLinks.get(item);
    }

    public float getFontSize()
    {
        return fontSize;
    }

    public int chapterCount()
    {
        return chapters.size();
    }

    public void findTextures(Set<Material> textures)
    {
        if (bookCover != null)
            textures.add(new Material(TextureAtlas.LOCATION_BLOCKS, bookCover));

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

        PageData pg = new PageData(new SectionRef(0, 0));
        ch.sections.add(pg);

        pg.elements.add(ElementParagraph.of("Error loading book:", TextStyle.ERROR));
        pg.elements.add(ElementParagraph.of(error, TextStyle.ERROR));
    }

    public boolean parseBook(InputStream stream, boolean loadedFromConfigFolder)
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

            var parsingContext = new ParsingContext()
            {

                @Override
                public Predicate<ConditionContext> getCondition(String name)
                {
                    return this.getCondition(name);
                }

                @Override
                public boolean loadedFromConfigFolder()
                {
                    return loadedFromConfigFolder;
                }

                @Override
                public DocumentBuilder xmlDocumentBuilder()
                {
                    return dBuilder;
                }
            };

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
                    bookCover = ResourceLocation.parse(n.getTextContent());
                }
                n = attributes.getNamedItem("model");
                if (n != null)
                {
                    var text = n.getTextContent();
                    if (text.contains("#"))
                    {
                        var parts = text.split("#");
                        var loc = ResourceLocation.parse(parts[0]);
                        var variant = parts[1];
                        bookModel = new ModelResourceLocation(loc, variant);
                    }
                    else
                    {
                        bookModel = ModelResourceLocation.standalone(ResourceLocation.parse(text));
                    }
                }
                n = attributes.getNamedItem("background");
                if (n != null)
                {
                    background = ResourceLocation.parse(n.getTextContent());
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
                        if (!ModList.get().isLoaded(s))
                        {
                            initializeWithLoadError("Dependency not loaded: " + s);
                            return false;
                        }
                    }
                }
            }

            parseDocumentLevelElements(parsingContext, root.getChildNodes());
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            initializeWithLoadError(e.toString());
        }
        return true;
    }

    private static final Map<ResourceLocation, Document> includeCache = new HashMap<>();
    private static final Map<ResourceLocation, Document> includeCacheNoConfig = new HashMap<>();

    private void parseDocumentLevelElements(ParsingContext context, NodeList firstLevel)
    {
        int chapterNumber = 0;
        for (int i = 0; i < firstLevel.getLength(); i++)
        {
            Node firstLevelNode = firstLevel.item(i);

            chapterNumber = parseDocumentLevelElement(context, chapterNumber, firstLevelNode);
        }
    }

    private int parseDocumentLevelElement(ParsingContext context, int chapterNumber, Node firstLevelNode)
    {
        String nodeName = firstLevelNode.getNodeName();
        if (nodeName.equals("template"))
        {
            parseTemplateDefinition(context,firstLevelNode, templates);
        }
        else if (nodeName.equals("include"))
        {
            int[] cn = new int[]{chapterNumber};
            parseInclude(context, firstLevelNode, (resLoc, document) -> {
                var includeRoot = document.getDocumentElement();
                if (includeRoot.getTagName().equals("library"))
                {
                    TemplateLibrary tpl = TemplateLibrary.get(context, document);
                    templates.putAll(tpl.templates);
                }
                else
                {
                    cn[0] = parseDocumentLevelElement(context, cn[0], includeRoot);
                }
            });
            chapterNumber = cn[0];
        }
        else if (nodeName.equals("chapter"))
        {
            parseChapter(context, chapterNumber++, firstLevelNode);
        }
        else if (nodeName.equals("stack-links"))
        {
            parseStackLinks(firstLevelNode);
        }
        else if (nodeName.equals("conditions"))
        {
            parseConditions(firstLevelNode);
        }
        return chapterNumber;
    }

    private static void parseInclude(ParsingContext context, Node firstLevelNode, BiConsumer<ResourceLocation, Document> includeAction)
    {
        NamedNodeMap attributes = firstLevelNode.getAttributes();
        Node n = attributes.getNamedItem("ref");

        ResourceLocation id = ResourceLocation.parse(n.getTextContent());
        Document include = context.loadedFromConfigFolder() ? parseIncludeFromConfig(context, id) : parseIncludeFromResources(context, id);;

        includeAction.accept(id, include);
    }

    private static Document parseIncludeFromConfig(ParsingContext context, ResourceLocation id)
    {
        return includeCache.computeIfAbsent(id, resLoc -> {
            // Prevents loading includes from config folder if the book was found in resource packs.
            if (context.loadedFromConfigFolder() && resLoc.getNamespace().equals("gbook"))
            {
                File booksFolder = BookRegistry.getBooksFolder();
                File file = new File(booksFolder, resLoc.getPath());
                if (file.exists() && file.isFile())
                {
                    try (InputStream stream = new FileInputStream(file))
                    {
                        return context.xmlDocumentBuilder().parse(stream);
                    }
                    catch (FileNotFoundException e)
                    {
                        // WUT? continue and try to load from resource pack
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                    catch (SAXException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }

            return parseIncludeFromResources(context, resLoc);
        });
    }

    private static Document parseIncludeFromResources(ParsingContext context, ResourceLocation resLoc)
    {
        return includeCacheNoConfig.computeIfAbsent(resLoc, resLoc2 -> {
            try
            {
                var res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(resLoc);
                try (InputStream stream = res.open())
                {
                    return context.xmlDocumentBuilder().parse(stream);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                catch (SAXException e)
                {
                    throw new RuntimeException(e);
                }
            }
            catch (FileNotFoundException e)
            {
                throw new UncheckedIOException(e);
            }
        });
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

    private void parseTemplateDefinition(ParsingContext context, Node templateItem, Map<String, TemplateDefinition> templates)
    {
        if (!templateItem.hasAttributes())
            return; // TODO: Throw error

        TemplateDefinition template = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null)
            return;

        templates.put(n.getTextContent(), template);

        parseChildElements(context, templateItem.getChildNodes(), template.elements, templates, true, TextStyle.DEFAULT);

        for(var i =0;i<attributes.getLength();i++)
        {
            var attr = attributes.item(i);
            var key = attr.getNodeName();
            if (!key.equals("id"))
                template.attributes.put(key, attr.getTextContent());
        }
    }

    private void parseChapter(ParsingContext context, int chapterNumber, Node chapterItem)
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

            sectionNumber = parseChapterElement(context, chapterNumber, chapter, sectionNumber, pageItem);
        }
    }

    private int parseChapterElement(ParsingContext context, int chapterNumber, ChapterData chapter, int sectionNumber, Node pageItem)
    {
        String nodeName = pageItem.getNodeName();

        if (nodeName.equals("page"))
        {
            parsePage(context, chapter, new SectionRef(chapterNumber, sectionNumber++), pageItem);
        }
        else if (nodeName.equals("section"))
        {
            parseSection(context, chapter, new SectionRef(chapterNumber, sectionNumber++), pageItem);
        }
        else if (nodeName.equals("include"))
        {
            int[] sn = new int[]{sectionNumber};
            parseInclude(context, pageItem, (name,document) -> {
                sn[0] = parseChapterElement(context, chapterNumber, chapter, sn[0], document.getDocumentElement());
            });
            sectionNumber = sn[0];
        }
        return sectionNumber;
    }

    private void parsePage(ParsingContext context, ChapterData chapter, SectionRef ref, Node pageItem)
    {
        PageData page = new PageData(ref);
        parseSection(context, chapter, pageItem, page);
    }

    private void parseSection(ParsingContext context, ChapterData chapter, SectionRef ref, Node pageItem)
    {
        PageData page = new PageGroup(ref);
        parseSection(context, chapter, pageItem, page);
    }

    private void parseSection(ParsingContext context, ChapterData chapter, Node pageItem, PageData page)
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

        parseChildElements(context, pageItem.getChildNodes(), page.elements, templates, true, TextStyle.DEFAULT);
    }

    public static void parseChildElements(ParsingContext context, NodeList elementsList, List<Element> elements, Map<String, TemplateDefinition> templates,
                                          boolean generateParagraphs, TextStyle defaultStyle)
    {
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            boolean isFirstElement = k == 0;
            boolean isLastElement = (k + 1) == elementsList.getLength();

            Node elementItem = elementsList.item(k);

            parsePageElement(context, elements, templates, generateParagraphs, defaultStyle, isFirstElement, isLastElement, elementItem);
        }
    }

    private static void parsePageElement(ParsingContext context, List<Element> elements, Map<String, TemplateDefinition> templates, boolean generateParagraphs,
                                         TextStyle defaultStyle, boolean isFirstElement, boolean isLastElement, Node elementItem)
    {
        Element parsedElement = null;

        String nodeName = elementItem.getNodeName();
        ResourceLocation nodeLoc =
                elementItem.getNodeType() == Node.ELEMENT_NODE ?
                        ResourceLocation.parse(nodeName) : ResourceLocation.parse("_");

        if (nodeName.equals("section-break"))
        {
            parsedElement = new ElementBreak();
        }
        else if (nodeName.equals("include"))
        {
            parseInclude(context, elementItem, (name,document) -> {
                parsePageElement(context, elements, templates, generateParagraphs, defaultStyle, isFirstElement, isLastElement, document.getDocumentElement());
            });
        }
        else if (nodeName.equals("p") || nodeName.equals("title"))
        {
            ElementParagraph p = new ElementParagraph();

            TextStyle tagDefaults = defaultStyle;
            if (nodeName.equals("title"))
            {
                p.alignment = ElementParagraph.ALIGN_CENTER;
                p.space = 4;
                tagDefaults = new TextStyle(defaultStyle.color, true, false, true, false, false, null, 1.0f);
            }

            TextStyle paragraphDefautls = TextStyle.parse(AttributeGetter.of(elementItem), tagDefaults);

            NodeList childList = elementItem.getChildNodes();
            int l = childList.getLength();
            for (int q = 0; q < l; q++)
            {
                Node childNode = childList.item(q);
                ElementInline parsedChild = parseParagraphElement(context, childNode, childNode.getNodeName(), isFirstElement, isLastElement, paragraphDefautls);

                if (parsedChild == null && childNode.getNodeType() != Node.TEXT_NODE)
                {
                    GuidebookMod.logger.warn("Unrecognized tag inside paragraph: {}", childNode.getNodeName());
                }
                else
                {
                    p.inlines.add(parsedChild);
                }
            }

            if (elementItem.hasAttributes())
            {
                p.parse(context, AttributeGetter.of(elementItem));
            }

            parsedElement = p;
        }
        else if (nodeName.equals("space")
                || nodeName.equals("group")
                || nodeName.equals("panel")
                || nodeName.equals("div"))
        {
            ElementPanel p = new ElementPanel();

            if (elementItem.hasAttributes())
            {
                p.parse(context, AttributeGetter.of(elementItem));
            }

            if (elementItem.hasChildNodes())
            {
                p.parseChildNodes(context, elementItem.getChildNodes(), templates, defaultStyle);
            }

            parsedElement = p;
        }
        else if (nodeName.equals("grid"))
        {
            ElementGrid g = new ElementGrid();

            if (elementItem.hasAttributes())
            {
                g.parse(context, AttributeGetter.of(elementItem));
            }

            if (elementItem.hasChildNodes())
            {
                g.parseChildNodes(context, elementItem.getChildNodes(), templates, defaultStyle);
            }

            parsedElement = g;
        }
        else if (customElements.containsKey(nodeLoc))
        {
            ElementFactory factory = customElements.get(nodeLoc);

            Element t = factory.newInstance();

            if (elementItem.hasAttributes())
            {
                t.parse(context, AttributeGetter.of(elementItem));
            }

            if (elementItem.hasChildNodes())
            {
                t.parseChildNodes(context, elementItem.getChildNodes(), templates, defaultStyle);
            }

            parsedElement = t;
        }
        else if (elementItem.getNodeType() == Node.ELEMENT_NODE && templates.containsKey(nodeName))
        {
            TemplateDefinition tDef = templates.get(nodeName);

            ElementPanel t = new ElementPanel();
            t.mode = ElementPanel.PanelMode.DEFAULT;
            t.parse(context, tDef);

            if (elementItem.hasAttributes())
            {
                t.parse(context, AttributeGetter.of(elementItem));
            }

            List<Element> elementList = Lists.newArrayList();

            parseChildElements(context, elementItem.getChildNodes(), elementList, templates, false, defaultStyle);

            List<Element> effectiveList = tDef.applyTemplate(context, elementList);

            t.innerElements.addAll(effectiveList);

            parsedElement = t;
        }
        else if (elementItem.getNodeType() == Node.TEXT_NODE)
        {
            if (generateParagraphs)
            {
                String textContent = ElementText.compactString(elementItem.getTextContent(), isFirstElement, isLastElement);
                if (!Strings.isNullOrEmpty(textContent) && !textContent.matches("^[ \t\r\n]+$"))
                    parsedElement = ElementSpan.of(textContent, defaultStyle);
            }
        }
        else if (elementItem.getNodeType() == Node.COMMENT_NODE)
        {
            // Ignore.
        }
        else
        {
            parsedElement = parseParagraphElement(context, elementItem, nodeName, isFirstElement, isLastElement, defaultStyle);

            if (parsedElement == null)
            {
                GuidebookMod.logger.warn("Unrecognized page-level tag: {}", nodeName);
            }
        }

        if (parsedElement != null)
        {
            if (!parsedElement.supportsPageLevel() && generateParagraphs && parsedElement instanceof ElementInline)
            {
                ElementParagraph p = new ElementParagraph();

                if (elementItem.hasAttributes())
                {
                    p.parse(context, AttributeGetter.of(elementItem));
                    parsedElement.parse(context, AttributeGetter.of(elementItem));
                }

                p.inlines.add((ElementInline) parsedElement);

                parsedElement = p;
            }

            if (parsedElement.supportsPageLevel())
                elements.add(parsedElement);
        }
    }

    @Nullable
    public static ElementInline parseParagraphElement(ParsingContext context, Node elementItem, String nodeName, boolean isFirstElement, boolean isLastElement, TextStyle defaultStyle)
    {
        ElementInline parsedElement = null;
        if (nodeName.equals("span"))
        {
            ElementSpan span = new ElementSpan(isFirstElement, isLastElement);

            if (elementItem.hasAttributes())
            {
                span.parse(context, AttributeGetter.of(elementItem));
            }

            if (elementItem.hasChildNodes())
            {
                TextStyle spanDefaults = TextStyle.parse(AttributeGetter.of(elementItem), defaultStyle);

                List<ElementInline> elementList = Lists.newArrayList();

                parseRunElements(context, elementItem, elementList, spanDefaults);

                span.inlines.addAll(elementList);
            }

            parsedElement = span;
        }
        else if (nodeName.equals("link") || nodeName.equals("a"))
        {
            ElementLink link = new ElementLink(isFirstElement, isLastElement);

            if (elementItem.hasAttributes())
            {
                link.parse(context, AttributeGetter.of(elementItem));
            }

            if (elementItem.hasChildNodes())
            {
                TextStyle spanDefaults = TextStyle.parse(AttributeGetter.of(elementItem), TextStyle.LINK);

                List<ElementInline> elementList = Lists.newArrayList();

                parseRunElements(context, elementItem, elementList, spanDefaults);

                link.inlines.addAll(elementList);
            }

            parsedElement = link;
        }
        else if (nodeName.equals("stack"))
        {
            ElementStack s = new ElementStack(isFirstElement, isLastElement, defaultStyle);

            if (elementItem.hasAttributes())
            {
                s.parse(context, AttributeGetter.of(elementItem));
            }

            parsedElement = s;
        }
        else if (nodeName.equals("image"))
        {
            ElementImage i = new ElementImage(isFirstElement, isLastElement);

            if (elementItem.hasAttributes())
            {
                i.parse(context, AttributeGetter.of(elementItem));
            }

            parsedElement = i;
        }
        else if (nodeName.equals("element"))
        {
            TemplateElement i = new TemplateElement(isFirstElement, isLastElement);

            if (elementItem.hasAttributes())
            {
                i.parse(context, AttributeGetter.of(elementItem));
            }

            parsedElement = i;
        }
        else if (elementItem.getNodeType() == Node.TEXT_NODE)
        {
            String textContent = ElementText.compactString(elementItem.getTextContent(), isFirstElement, isLastElement);
            if (!Strings.isNullOrEmpty(textContent))
                parsedElement = ElementSpan.of(textContent, isFirstElement, isLastElement, defaultStyle);
        }
        return parsedElement;
    }

    public static void parseRunElements(ParsingContext context, Node pageItem, List<ElementInline> elements, TextStyle defaultStyle)
    {
        NodeList elementsList = pageItem.getChildNodes();
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            boolean isFirstElement = k == 0;
            boolean isLastElement = (k + 1) == elementsList.getLength();

            Node elementItem = elementsList.item(k);

            ElementInline parsedElement = null;

            String nodeName = elementItem.getNodeName();
            ResourceLocation nodeLoc =
                    elementItem.getNodeType() == Node.ELEMENT_NODE ?
                            ResourceLocation.parse(nodeName) : ResourceLocation.parse("_");

            if (customElements.containsKey(nodeLoc))
            {
                ElementFactory factory = customElements.get(nodeLoc);

                Element t = factory.newInstance();

                if (t instanceof ElementInline)
                {
                    if (elementItem.hasAttributes())
                    {
                        t.parse(context, AttributeGetter.of(elementItem));
                    }

                    if (elementItem.hasChildNodes())
                    {
                        t.parseChildNodes(context, elementItem.getChildNodes(), Collections.emptyMap(), defaultStyle);
                    }

                    parsedElement = (ElementInline) t;
                }
            }
            else if (elementItem.getNodeType() == Node.TEXT_NODE)
            {
                String textContent = ElementText.compactString(elementItem.getTextContent(), isFirstElement, isLastElement);
                if (!Strings.isNullOrEmpty(textContent))
                    parsedElement = ElementSpan.of(textContent, isFirstElement, isLastElement, defaultStyle);
            }
            else if (elementItem.getNodeType() == Node.COMMENT_NODE)
            {
                // Ignore.
            }
            else
            {
                parsedElement = parseParagraphElement(context, elementItem, nodeName, isFirstElement, isLastElement, defaultStyle);

                if (parsedElement == null)
                {
                    GuidebookMod.logger.warn("Unrecognized inline tag: {}", nodeName);
                }
            }

            if (parsedElement != null)
            {
                elements.add(parsedElement);
            }
        }
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
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item_node.getTextContent()));
                        if (item != Items.AIR)
                        {
                            String ref = refItem.getTextContent();
                            stackLinks.put(item, SectionRef.fromString(ref));
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
            for (PageData section : sections)
            {
                if (!section.conditionResult || section.isEmpty())
                    continue;

                if (!Strings.isNullOrEmpty(section.id))
                    ch.pagesByName.put(section.id, ch.pages.size());

                ch.pages.addAll(section.reflow(rendering, pageSize));
            }
        }

        public boolean isEmpty()
        {
            return sections.stream().noneMatch(s -> s.conditionResult && !s.isEmpty());
        }
    }

    public static class PageData
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

            if (VisualDebugArea.INJECT_DEBUG)
            {
                var area = new VisualDebugArea(pageSize, 0, 0, 0, Component.literal("page"));
                area.position = new Point();

                page.children.add(area);
            }

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
    public static class PageGroup extends PageData
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

            if (VisualDebugArea.INJECT_DEBUG)
            {
                var area = new VisualDebugArea(pageSize, 0, 0, 0, Component.literal("page"));
                area.position = new Point();

                page.children.add(area);
            }

            int top = pageBounds.position.y();
            for (Element element : elements)
            {
                if (element.conditionResult)
                    top = element.reflow(page.children, rendering, new Rect(new Point(pageBounds.position.x(), top), pageBounds.size), pageBounds);
            }

            boolean needsRepagination = false;
            for (VisualElement child : page.children)
            {
                if (child instanceof VisualPageBreak || (child.position.y() + child.size.height()) > (pageBounds.position.y() + pageBounds.size.height()))
                {
                    needsRepagination = true;
                    break;
                }
            }

            if (needsRepagination)
            {
                VisualPage page2 = new VisualPage(ref);

                if (VisualDebugArea.INJECT_DEBUG)
                {
                    var area2 = new VisualDebugArea(pageSize, 0, 0, 0, Component.literal("page"));
                    area2.position = new Point();

                    page2.children.add(area2);
                }

                int offsetY = 0;
                boolean pageBreakRequired = false;
                for (VisualElement child : page.children)
                {
                    int cpy = child.position.y() + offsetY;
                    if (pageBreakRequired || (cpy + child.size.height()) > (pageBounds.position.y() + pageBounds.size.height())
                            && (child.position.y() > pageBounds.position.y()))
                    {
                        page2.updateDebugIndices();
                        pages.add(page2);
                        page2 = new VisualPage(ref);

                        if (VisualDebugArea.INJECT_DEBUG)
                        {
                            var area3 = new VisualDebugArea(pageSize, 0, 0, 0, Component.literal("page"));
                            area3.position = new Point();

                            page2.children.add(area3);
                        }

                        offsetY = pageBounds.position.y() - child.position.y();
                        pageBreakRequired = false;
                    }

                    if (child instanceof VisualPageBreak)
                    {
                        pageBreakRequired = true;
                    }
                    else
                    {
                        child.move(0,offsetY);
                        page2.children.add(child);
                    }
                }

                page2.updateDebugIndices();
                pages.add(page2);
            }
            else
            {
                page.updateDebugIndices();
                pages.add(page);
            }

            return pages;
        }
    }
}
