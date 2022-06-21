package dev.gigaherz.guidebook.guidebook.book;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Floats;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookParsingException;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionManager;
import dev.gigaherz.guidebook.guidebook.elements.*;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.templates.TemplateElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BookDocumentParser
{
    private static final float DEFAULT_FONT_SIZE = 1.0f;

    private static final Map<ResourceLocation, DocumentLevelElementParser> documentLevelElements = Maps.newHashMap();
    private static final Map<ResourceLocation, InlineElementFactory> inlineElements = Maps.newHashMap();
    private static final Map<ResourceLocation, ElementFactory> elements = Maps.newHashMap();
    private static final Map<ResourceLocation, PageFactory> pages = Maps.newHashMap();
    private static final Map<ResourceLocation, ElementModifier> modifiers = Maps.newHashMap();
    private static final Map<ResourceLocation, Document> includeCache = Maps.newHashMap();

    static {
        registerDefaultPage("page", PageData::new);
        registerDefaultPage("section", PageGroup::new);
        registerDefaultElement("recipe", ElementRecipe::new);
        registerDefaultElement("grid", ElementGrid::new);
        registerDefaultElement("space", ElementPanel::new);
        registerDefaultElement("group", ElementPanel::new);
        registerDefaultElement("panel", ElementPanel::new);
        registerDefaultElement("div", ElementPanel::new);
        registerDefaultElement("section-break", ElementBreak::new);
        registerDefaultElement("p", ElementParagraph::new);
        registerDefaultElement("title", ElementTitle::new);
        registerDefaultInlineElement("span", ElementSpan::new);
        registerDefaultInlineElement("link", ElementLink::new);
        registerDefaultInlineElement("a", ElementLink::new);
        registerDefaultInlineElement("stack", ElementStack::new);
        registerDefaultInlineElement("image", ElementImage::new);
        registerDefaultInlineElement("element", TemplateElement::new);
        registerDefaultDocumentLevelElement("template", (context, chapterNumber, node) -> parseTemplateDefinition(context, node, context.document().templates));
        registerDefaultDocumentLevelElement("stack-links", (context, chapterNumber, node) -> parseStackLinks(context, node));
        registerDefaultDocumentLevelElement("conditions", (context, chapterNumber, node) -> parseConditions(context, node));
        registerDefaultDocumentLevelElement("chapter", (context, chapterNumber, node) -> parseChapter(context, chapterNumber.getAndIncrement(), node));
    }

    public static void registerCustomElement(ResourceLocation location, ElementFactory factory)
    {
        if (elements.containsKey(location))
        {
            throw new IllegalArgumentException("Can not register two custom element factories with the same id.");
        }

        elements.put(location, factory);
    }

    public static void registerCustomInlineElement(ResourceLocation location, InlineElementFactory factory)
    {
        if (inlineElements.containsKey(location))
        {
            throw new IllegalArgumentException("Can not register two custom inline element factories with the same id.");
        }

        inlineElements.put(location, factory);
    }

    public static void registerCustomPage(ResourceLocation location, PageFactory factory)
    {
        if (pages.containsKey(location))
        {
            throw new IllegalArgumentException("Can not register two page factories with the same id.");
        }

        pages.put(location, factory);
    }

    public static void registerCustomDocumentLevelElement(ResourceLocation location, DocumentLevelElementParser parser)
    {
        if (documentLevelElements.containsKey(location))
        {
            throw new IllegalArgumentException("Can not register two document level element parser with the same id.");
        }

        documentLevelElements.put(location, parser);
    }

    public static void registerCustomDocumentLevelElement(ResourceLocation location, ElementModifier modifier)
    {
        if (modifiers.containsKey(location))
        {
            throw new IllegalArgumentException("Can not register two element modifier with the same id.");
        }

        modifiers.put(location, modifier);
    }

    public static void invalidateIncludeCache()
    {
        includeCache.clear();
    }

    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "UnstableApiUsage"})
    @Nullable
    public static BookDocument parseBook(BookDocument document, InputStream stream, boolean loadedFromConfigFolder)
    {
        try
        {
            document.chapters.clear();
            document.bookName = "";
            document.bookCover = null;
            document.fontSize = DEFAULT_FONT_SIZE;
            document.chaptersByName.clear();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);

            var parsingContext = new ParsingContext()
            {
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

                @Override
                public BookDocument document()
                {
                    return document;
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
                    document.bookName = n.getTextContent();
                }
                n = attributes.getNamedItem("cover");
                if (n != null)
                {
                    document.bookCover = new ResourceLocation(n.getTextContent());
                }
                n = attributes.getNamedItem("model");
                if (n != null)
                {
                    var text = n.getTextContent();
                    if (text.contains("#"))
                    {
                        document.bookModel = new ModelResourceLocation(text);
                    }
                    else
                    {
                        document.bookModel = new ResourceLocation(text);
                    }
                }
                n = attributes.getNamedItem("background");
                if (n != null)
                {
                    document.background = new ResourceLocation(n.getTextContent());
                }
                n = attributes.getNamedItem("widgets");
                if (n != null)
                {
                    document.widgets = new ResourceLocation(n.getTextContent());
                }
                n = attributes.getNamedItem("fontSize");
                if (n != null)
                {
                    Float f = Floats.tryParse(n.getTextContent());
                    document.fontSize = f != null ? f : DEFAULT_FONT_SIZE;
                }
                n = attributes.getNamedItem("home");
                if (n != null)
                {
                    String ref = n.getTextContent();
                    document.home = SectionRef.fromString(ref);
                }
                n = attributes.getNamedItem("dependencies");
                if (n != null)
                {
                    for (String s : n.getTextContent().split(","))
                    {
                        if (!ModList.get().isLoaded(s))
                        {
                            document.initializeWithLoadError("Dependency not loaded: " + s);
                            return null;
                        }
                    }
                }
            }

            parseDocumentLevelElements(parsingContext, root.getChildNodes());
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            document.initializeWithLoadError(e.toString());
        }
        return document;
    }

    public static void parseTemplateDefinition(ParsingContext context, Node templateItem, Map<String, TemplateDefinition> templates)
    {
        if (!templateItem.hasAttributes()) return; // TODO: Throw error

        TemplateDefinition page = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null) return;

        templates.put(n.getTextContent(), page);

        parseChildElements(context, templateItem.getChildNodes(), page.elements, templates, true, TextStyle.DEFAULT);

        attributes.removeNamedItem("id");
        page.attributes = attributes;
    }

    public static List<Predicate<ConditionContext>> parseChildConditions(Node node)
    {
        List<Predicate<ConditionContext>> conditions = Lists.newArrayList();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node condition = children.item(i);
            if (condition.getNodeType() != Node.ELEMENT_NODE) continue;

            Predicate<ConditionContext> displayCondition = parseSingleCondition(condition);

            conditions.add(displayCondition);
        }
        return conditions;
    }

    public static void parseChildElements(ParsingContext context, NodeList elementsList, List<Element> elements, Map<String, TemplateDefinition> templates,
                                          boolean generateParagraphs, TextStyle defaultStyle)
    {
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            boolean isFirstElement = k == 0;
            boolean isLastElement = (k + 1) == elementsList.getLength();

            var ctx = new ParsingContext.Wrapper(context)
            {
                @Override
                public boolean isFirstElement()
                {
                    return isFirstElement;
                }

                @Override
                public boolean isLastElement()
                {
                    return isLastElement;
                }
            };

            Node elementItem = elementsList.item(k);

            parsePageElement(ctx, elements, templates, generateParagraphs, defaultStyle, elementItem);
        }
    }

    @Nullable
    public static ElementInline parseParagraphElement(ParsingContext context, Node elementItem, TextStyle defaultStyle)
    {
        if (elementItem.getNodeType() != Node.TEXT_NODE)
        {
            return createInlineElement(context, defaultStyle, elementItem, getNodeLoc(elementItem));
        }
        String textContent = ElementText.compactString(elementItem.getTextContent(), context.isFirstElement(), context.isLastElement());
        if (!Strings.isNullOrEmpty(textContent))
        {
            return ElementSpan.of(textContent, context.isFirstElement(), context.isLastElement(), defaultStyle);
        }
        return null;
    }

    public static void parseRunElements(ParsingContext parent, NodeList elementsList, List<ElementInline> elements, TextStyle defaultStyle)
    {
        for (int k = 0; k < elementsList.getLength(); k++)
        {
            boolean isFirstElement = k == 0;
            boolean isLastElement = (k + 1) == elementsList.getLength();

            var context = new ParsingContext.Wrapper(parent)
            {
                @Override
                public boolean isFirstElement()
                {
                    return isFirstElement;
                }

                @Override
                public boolean isLastElement()
                {
                    return isLastElement;
                }
            };

            parseRunElement(context, elements, elementsList.item(k), defaultStyle);
        }
    }

    private static void parseRunElement(ParsingContext context, List<ElementInline> elements, Node elementItem, TextStyle defaultStyle)
    {
        String nodeName = elementItem.getNodeName();
        ResourceLocation nodeLoc = getNodeLoc(elementItem);

        if (BookDocumentParser.elements.containsKey(nodeLoc))
        {
            if (createElement(context, Collections.emptyMap(), defaultStyle, elementItem, nodeLoc) instanceof ElementInline inline)
            {
                elements.add(inline);
            }
        }
        else if (inlineElements.containsKey(nodeLoc))
        {
            var e = createInlineElement(context, defaultStyle, elementItem, nodeLoc);
            if (e != null)
            {
                elements.add(e);
            }
        }
        else
        {
            var e = switch (elementItem.getNodeType())
            {
                case Node.TEXT_NODE ->
                {
                    String textContent = ElementText.compactString(elementItem.getTextContent(), context.isFirstElement(), context.isLastElement());
                    if (!Strings.isNullOrEmpty(textContent))
                    {
                        yield ElementSpan.of(textContent, context.isFirstElement(), context.isLastElement(), defaultStyle);
                    }
                    yield null;
                }
                case Node.COMMENT_NODE -> null;
                default ->
                {
                    var el = parseParagraphElement(context, elementItem, defaultStyle);
                    if (el == null)
                    {
                        GuidebookMod.logger.warn("Unrecognized tag: {}", nodeName);
                    }
                    yield el;
                }
            };
            if (e != null)
            {
                elements.add(e);
            }
        }

    }

    @NotNull
    private static ResourceLocation getNodeLoc(Node elementItem)
    {
        return elementItem.getNodeType() == Node.ELEMENT_NODE ? new ResourceLocation(elementItem.getNodeName()) : new ResourceLocation("_");
    }

    private static void registerDefaultElement(String location, ElementFactory factory)
    {
        elements.put(new ResourceLocation(location), factory);
    }

    private static void registerDefaultInlineElement(String location, InlineElementFactory factory)
    {
        inlineElements.put(new ResourceLocation(location), factory);
    }

    private static void registerDefaultDocumentLevelElement(String location, DocumentLevelElementParser parser)
    {
        documentLevelElements.put(new ResourceLocation(location), parser);
    }

    private static void registerDefaultPage(String location, PageFactory parser)
    {
        pages.put(new ResourceLocation(location), parser);
    }

    private static void parseDocumentLevelElements(ParsingContext context, NodeList firstLevel)
    {
        AtomicInteger chapterNumber = new AtomicInteger(0);
        for (int i = 0; i < firstLevel.getLength(); i++)
        {
            Node firstLevelNode = firstLevel.item(i);

            parseDocumentLevelElement(context, chapterNumber, firstLevelNode);
        }
    }

    private static void parseDocumentLevelElement(ParsingContext context, AtomicInteger chapterNumber, Node firstLevelNode)
    {
        String nodeName = firstLevelNode.getNodeName();
        if ("include".equals(nodeName))
        {
            parseInclude(context, firstLevelNode, (resLoc, includeRoot) -> {
                if ("library".equals(includeRoot.getNodeName()))
                {
                    TemplateLibrary tpl = TemplateLibrary.get(context, resLoc, includeRoot);
                    context.document().templates.putAll(tpl.templates);
                }
                else
                {
                    parseDocumentLevelElement(context, chapterNumber, includeRoot);
                }
            });
            return;
        }
        if (firstLevelNode.getNodeType() != Node.ELEMENT_NODE)
            return;
        DocumentLevelElementParser parser = documentLevelElements.get(getNodeLoc(firstLevelNode));
        if (parser != null)
        {
            parser.parse(context, chapterNumber, firstLevelNode);
        }
    }

    private static void parseStackLinks(ParsingContext context, Node refsItem)
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
                        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item_node.getTextContent()));
                        if (item != null)
                        {
                            String ref = refItem.getTextContent();
                            context.document().stackLinks.put(item, SectionRef.fromString(ref));
                        }
                    }
                }
            }
        }
    }

    private static void parseConditions(ParsingContext context, Node node)
    {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node condition = children.item(i);
            if (condition.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }

            NamedNodeMap attributes = condition.getAttributes();

            Node conditionName = attributes.getNamedItem("name");
            String name = conditionName != null ? conditionName.getTextContent() : null;

            if (Strings.isNullOrEmpty(name))
            {
                throw new BookParsingException("Condition node found without a name attribute");
            }

            Predicate<ConditionContext> displayCondition = parseSingleCondition(condition);

            context.document().conditions.put(name, displayCondition);
        }
    }

    private static void parseChapter(ParsingContext context, int chapterNumber, Node chapterItem)
    {
        ChapterData chapter = new ChapterData(context.document().chapters.size());
        context.document().chapters.add(chapter);

        if (chapterItem.hasAttributes())
        {
            chapter.parse(context, chapterItem.getAttributes());
        }

        AtomicInteger sectionNumber = new AtomicInteger(0);
        NodeList pagesList = chapterItem.getChildNodes();
        for (int j = 0; j < pagesList.getLength(); j++)
        {
            Node pageItem = pagesList.item(j);

            parseChapterElement(context, chapterNumber, chapter, sectionNumber, pageItem);
        }
    }

    private static void parseChapterElement(ParsingContext context, int chapterNumber, ChapterData chapter, AtomicInteger sectionNumber, Node pageItem)
    {
        if ("include".equals(pageItem.getNodeName()))
        {
            parseInclude(context, pageItem, (name, doc) -> parseChapterElement(context, chapterNumber, chapter, sectionNumber, doc));
        }
        else if (pageItem.getNodeType() == Node.ELEMENT_NODE && pages.containsKey(getNodeLoc(pageItem)))
        {
            PageFactory factory = pages.get(getNodeLoc(pageItem));
            PageData page = factory.newInstance(new SectionRef(chapterNumber, sectionNumber.getAndIncrement()));
            parseSection(context, chapter, pageItem, page);
        }
    }

    private static void parseSection(ParsingContext parentContext, ChapterData chapter, Node pageItem, PageData page)
    {
        chapter.sections.add(page);

        var context = new ParsingContext.Wrapper(parentContext)
        {
            @Override
            public ChapterData chapter()
            {
                return chapter;
            }
        };

        if (pageItem.hasAttributes())
        {
            page.parse(context, pageItem.getAttributes());
        }

        if (pageItem.hasChildNodes())
        {
            page.parseChildNodes(context, pageItem.getChildNodes(), context.document().templates, TextStyle.DEFAULT);
        }
    }

    private static void parseInclude(ParsingContext context, Node firstLevelNode, BiConsumer<ResourceLocation, Node> includeAction)
    {
        NamedNodeMap attributes = firstLevelNode.getAttributes();
        Node n = attributes.getNamedItem("ref");

        ResourceLocation id = new ResourceLocation(n.getTextContent());
        Document include = includeCache.computeIfAbsent(id, resLoc -> {

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

        includeAction.accept(id, include.getDocumentElement());
    }

    private static Predicate<ConditionContext> parseSingleCondition(Node condition)
    {
        Predicate<ConditionContext> displayCondition;
        try
        {
            displayCondition = ConditionManager.parseCondition(condition);
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

    private static void parsePageElement(ParsingContext context, List<Element> elements, Map<String, TemplateDefinition> templates, boolean generateParagraphs,
                                         TextStyle defaultStyle, Node elementItem)
    {
        Element parsedElement = null;

        String nodeName = elementItem.getNodeName();
        ResourceLocation nodeLoc =
                getNodeLoc(elementItem);

        if ("include".equals(nodeName))
        {
            parseInclude(context, elementItem, (name, document) ->
                    parsePageElement(context, elements, templates, generateParagraphs, defaultStyle, document)
            );
        }
        else if (BookDocumentParser.elements.containsKey(nodeLoc))
        {
            parsedElement = createElement(context, templates, defaultStyle, elementItem, nodeLoc);
        }
        else if (templates.containsKey(nodeName))
        {
            TemplateDefinition tDef = templates.get(nodeName);

            ElementPanel t = new ElementPanel();
            t.parse(context, tDef.attributes);

            if (elementItem.hasAttributes())
            {
                t.parse(context, elementItem.getAttributes());
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
                String textContent = ElementText.compactString(elementItem.getTextContent(), context.isFirstElement(), context.isLastElement());
                if (!Strings.isNullOrEmpty(textContent) && !textContent.matches("^[ \t\r\n]+$"))
                {
                    parsedElement = ElementSpan.of(textContent, defaultStyle);
                }
            }
        }
        else if (elementItem.getNodeType() != Node.COMMENT_NODE)
        {
            parsedElement = parseParagraphElement(context, elementItem, defaultStyle);

            if (parsedElement == null)
            {
                GuidebookMod.logger.warn("Unrecognized tag: {}", nodeName);
            }
        }

        if (parsedElement == null) return;

        if (!parsedElement.supportsPageLevel() && generateParagraphs && parsedElement instanceof ElementInline)
        {
            ElementParagraph p = new ElementParagraph();

            if (elementItem.hasAttributes())
            {
                p.parse(context, elementItem.getAttributes());
                parsedElement.parse(context, elementItem.getAttributes());
            }

            p.inlines.add((ElementInline) parsedElement);

            parsedElement = p;
        }

        if (parsedElement.supportsPageLevel())
        {
            elements.add(parsedElement);
        }
    }

    @Nullable
    private static Element createElement(ParsingContext context, Map<String, TemplateDefinition> templates, TextStyle defaultStyle, Node elementItem, ResourceLocation nodeLoc)
    {
        ElementFactory factory = elements.get(nodeLoc);
        if (factory == null)
        {
            return null;
        }
        Element t = factory.newInstance();
        TextStyle childStyle = t.childStyle(context, elementItem.getAttributes(), defaultStyle);
        if (elementItem.hasAttributes())
        {
            NamedNodeMap attributes = elementItem.getAttributes();
            t.parse(context, attributes);
            applyModifiers(context, defaultStyle, t, attributes);
        }
        if (elementItem.hasChildNodes())
        {
            t.parseChildNodes(context, elementItem.getChildNodes(), templates, childStyle);
        }
        return t;
    }

    @Nullable
    private static ElementInline createInlineElement(ParsingContext context, TextStyle defaultStyle, Node elementItem, ResourceLocation nodeLoc)
    {
        InlineElementFactory factory = inlineElements.get(nodeLoc);
        if (factory == null) return null;
        ElementInline element = factory.newInstance(context.isFirstElement(), context.isLastElement());
        TextStyle childStyle = element.childStyle(context, elementItem.getAttributes(), defaultStyle);
        if (elementItem.hasAttributes())
        {
            NamedNodeMap attributes = elementItem.getAttributes();
            element.parse(context, attributes);
            applyModifiers(context, defaultStyle, element, attributes);
        }
        if (elementItem.hasChildNodes())
        {
            element.parseChildNodes(context, elementItem.getChildNodes(), Collections.emptyMap(), childStyle);
        }
        return element;
    }

    private static void applyModifiers(ParsingContext context, TextStyle defaultStyle, Element element, NamedNodeMap attributes)
    {
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Node attr = attributes.item(i);
            ResourceLocation key = ResourceLocation.tryParse(attr.getNodeName());
            if (key == null) continue;
            if (!modifiers.containsKey(key)) continue;
            ElementModifier elementModifier = modifiers.get(key);
            if (!elementModifier.canModify(context, element)) continue;
            elementModifier.modify(context, element, attr.getNodeValue(), attributes, defaultStyle);
        }
    }
}
