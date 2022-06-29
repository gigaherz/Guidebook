package dev.gigaherz.guidebook.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ExistingFileHelper.IResourceType;
import net.minecraftforge.common.data.ExistingFileHelper.ResourceType;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class BookProvider implements DataProvider
{
    public static final IResourceType RESOURCE_TYPE = new ResourceType(PackType.CLIENT_RESOURCES, ".xml", "");

    private final Map<ResourceLocation, Document> documents = new HashMap<>();
    private final Logger logger = LogUtils.getLogger();

    private final DataGenerator generator;
    private final ExistingFileHelper existingFileHelper;
    private final String modid;
    private final DocumentBuilder documentBuilder;

    public BookProvider(DataGenerator generator, ExistingFileHelper existingFileHelper, String modid)
    {
        this.generator = generator;
        this.existingFileHelper = existingFileHelper;
        this.modid = modid;
        try
        {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(CachedOutput cachedOutput) throws IOException
    {
        documents.clear();
        createBooks();
        JsonArray books = new JsonArray();
        documents.entrySet()
                .stream()
                .filter(entry -> "book".equals(entry.getValue().getDocumentElement().getNodeName()))
                .map(Map.Entry::getKey)
                .map(Objects::toString)
                .forEach(books::add);
        DataProvider.saveStable(cachedOutput, books, generator.getOutputFolder().resolve("assets/" + modid + "/books.json"));
        for (Map.Entry<ResourceLocation, Document> builder : documents.entrySet())
        {
            ResourceLocation key = builder.getKey();
            Document value = builder.getValue();
            try
            {
                save(cachedOutput, makePath(key), value);
            }
            catch (TransformerException e)
            {
                logger.error("Couldn't save book {}", key, e);
            }
        }
    }

    private Path makePath(ResourceLocation location)
    {
        return generator.getOutputFolder()
                .resolve(RESOURCE_TYPE.getPackType().getDirectory())
                .resolve(location.getNamespace())
                .resolve(RESOURCE_TYPE.getPrefix())
                .resolve(location.getPath());
    }

    protected abstract void createBooks();

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private static void save(CachedOutput cachedOutput, Path path, Node doc) throws TransformerException, IOException
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
        transformer.transform(new DOMSource(doc), new StreamResult(hashingoutputstream));
        cachedOutput.writeIfNeeded(path, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
    }

    public BookBuilder book(String name)
    {
        return book(name, false, false, false);
    }

    public BookBuilder book(String name, boolean extractChapters, boolean extractSections, boolean extractPages)
    {
        BookBuilder bookBuilder = new BookBuilder(new ResourceLocation(modid, name + ".xml"), documentBuilder.newDocument(), extractChapters, extractSections, extractPages);
        trackDocument(bookBuilder.location, bookBuilder.document());
        return bookBuilder;
    }

    protected final void trackDocument(ResourceLocation location, Document doc)
    {
        existingFileHelper.trackGenerated(location, RESOURCE_TYPE);
        documents.put(location, doc);
    }

    public abstract static class ElementBuilder
    {
        private final Document document;
        private final Element element;

        protected ElementBuilder(Document document, Element element)
        {
            this.document = document;
            this.element = element;
        }

        protected Element createChildElement(String name)
        {
            Element child = createElement(name);
            this.element.appendChild(child);
            return child;
        }

        protected Element createElement(String name)
        {
            return document.createElement(name);
        }

        protected void setAttribute(String name, String value)
        {
            this.element.setAttribute(name, value);
        }

        public Document document()
        {
            return document;
        }

        public Element element()
        {
            return element;
        }
    }

    public class BookBuilder extends ElementBuilder
    {
        private final Set<String> conditions = new HashSet<>();
        private final ResourceLocation location;
        private final boolean extractChapters;
        private final boolean extractSections;
        private final boolean extractPages;
        private Element conditionsElement;

        private static Element root(Document document)
        {
            document.appendChild(document.createElement("book"));
            return document.getDocumentElement();
        }

        private BookBuilder(ResourceLocation location, Document document, boolean extractChapters, boolean extractSections, boolean extractPages)
        {
            super(document, root(document));
            this.location = location;
            this.extractChapters = extractChapters;
            this.extractSections = extractSections;
            this.extractPages = extractPages;
        }

        public BookBuilder title(String title)
        {
            setAttribute("title", title);
            return this;
        }

        public BookBuilder cover(ResourceLocation cover)
        {
            setAttribute("cover", cover.toString());
            return this;
        }

        public BookBuilder model(ResourceLocation model)
        {
            setAttribute("model", model.toString());
            return this;
        }

        public BookBuilder background(ResourceLocation background)
        {
            setAttribute("background", background.toString());
            return this;
        }

        public BookBuilder fontSize(float size)
        {
            setAttribute("fontSize", String.valueOf(size));
            return this;
        }

        public BookBuilder home(String chapter, String section)
        {
            setAttribute("home", chapter + ":" + section);
            return this;
        }

        public BookBuilder home(String ref)
        {
            setAttribute("home", ref);
            return this;
        }

        public BookBuilder dependencies(String... dependencies)
        {
            setAttribute("dependencies", String.join(",", dependencies));
            return this;
        }

        public BookBuilder include(ResourceLocation location)
        {
            existingFileHelper.exists(location, PackType.CLIENT_RESOURCES);
            Element include = createChildElement("include");
            include.setAttribute("ref", location.toString());
            return this;
        }

        public BookBuilder chapter(String name, Consumer<ChapterBuilder> consumer)
        {
            return chapter(name, extractChapters, consumer);
        }

        public BookBuilder chapter(String name, boolean extract, Consumer<ChapterBuilder> consumer)
        {
            if (extract)
            {
                Document chapterDoc = documentBuilder.newDocument();
                chapterDoc.appendChild(chapterDoc.createElement("chapter"));
                ResourceLocation chapterLocation = new ResourceLocation(location.getNamespace(), String.join("/", location.getPath().split("\\.")[0], "chapters", name + ".xml"));
                consumer.accept(new ChapterBuilder(chapterDoc, chapterDoc.getDocumentElement()));
                trackDocument(chapterLocation, chapterDoc);
                return include(chapterLocation);
            }
            consumer.accept(new ChapterBuilder(document(), createChildElement("chapter")));
            return this;
        }

        public BookBuilder condition(String type, String name, Map<String, String> attributes)
        {
            if (this.conditionsElement == null)
                this.conditionsElement = createChildElement("conditions");
            Element element = createElement(type);
            attributes.forEach(element::setAttribute);
            element.setAttribute("name", name);
            this.conditionsElement.appendChild(element);
            this.conditions.add(name);
            return this;
        }

        public BookBuilder template(String id, Consumer<Element> consumer)
        {
            Element template = createChildElement("template");
            consumer.accept(template);
            template.setAttribute("id", id);
            return this;
        }

        protected boolean conditionExists(String name)
        {
            return this.conditions.contains(name);
        }

        public class ChapterBuilder extends ElementBuilder
        {
            public ChapterBuilder(Document document, Element chapterElement)
            {
                super(document, chapterElement);
            }

            public ChapterBuilder id(String id)
            {
                setAttribute("id", id);
                return this;
            }

            public ChapterBuilder condition(String condition)
            {
                if (!conditionExists(condition))
                    throw new IllegalArgumentException("Condition " + condition + " does not exist");
                setAttribute("condition", condition);
                return this;
            }

            public ChapterBuilder include(ResourceLocation location)
            {
                existingFileHelper.exists(location, PackType.CLIENT_RESOURCES);
                Element include = createChildElement("include");
                include.setAttribute("ref", location.toString());
                return this;
            }

            public ChapterBuilder page(String name, Consumer<PageBuilder> consumer)
            {
                return page(name, extractPages, consumer);
            }

            public ChapterBuilder page(String name, boolean extract, Consumer<PageBuilder> consumer)
            {
                if (extract)
                {
                    Document pageDoc = documentBuilder.newDocument();
                    pageDoc.appendChild(pageDoc.createElement("page"));
                    ResourceLocation pageLoc = new ResourceLocation(location.getNamespace(), String.join("/", location.getPath().split("\\.")[0], "pages", name + ".xml"));
                    consumer.accept(new PageBuilder(pageDoc, pageDoc.getDocumentElement()));
                    trackDocument(pageLoc, pageDoc);
                    return include(pageLoc);
                }
                consumer.accept(new PageBuilder(document(), createChildElement("page")));
                return this;
            }

            public ChapterBuilder section(String name, Consumer<PageBuilder> consumer)
            {
                return section(name, extractSections, consumer);
            }

            public ChapterBuilder section(String name, boolean extract, Consumer<PageBuilder> consumer)
            {
                if (extract)
                {
                    Document pageDoc = documentBuilder.newDocument();
                    pageDoc.appendChild(pageDoc.createElement("section"));
                    ResourceLocation pageLoc = new ResourceLocation(location.getNamespace(), String.join("/", location.getPath().split("\\.")[0], "sections", name + ".xml"));
                    consumer.accept(new PageBuilder(pageDoc, pageDoc.getDocumentElement()));
                    trackDocument(pageLoc, pageDoc);
                    return include(pageLoc);
                }
                consumer.accept(new PageBuilder(document(), createChildElement("section")));
                return this;
            }

            public class PageBuilder extends ElementBuilder
            {
                public PageBuilder(Document document, Element pageElement)
                {
                    super(document, pageElement);
                }

                public PageBuilder id(String id)
                {
                    setAttribute("id", id);
                    return this;
                }

                public PageBuilder condition(String condition)
                {
                    if (!conditionExists(condition))
                        throw new IllegalArgumentException("Condition " + condition + " does not exist");
                    setAttribute("condition", condition);
                    return this;
                }

                public PageBuilder sectionBreak()
                {
                    createChildElement("section-break");
                    return this;
                }

                public PageBuilder include(ResourceLocation location)
                {
                    existingFileHelper.exists(location, PackType.CLIENT_RESOURCES);
                    Element include = createChildElement("include");
                    include.setAttribute("ref", location.toString());
                    return this;
                }

                public PageBuilder paragraph(Consumer<ParagraphBuilder> consumer)
                {
                    Element p = createChildElement("p");
                    consumer.accept(new ParagraphBuilder(document(), p));
                    return this;
                }

                public PageBuilder title(String text, Consumer<Element> consumer)
                {
                    Element p = createChildElement("title");
                    consumer.accept(p);
                    p.setTextContent(text);
                    return this;
                }

                public PageBuilder text(String text)
                {
                    element().appendChild(document().createTextNode(text));
                    return this;
                }

                private class ParagraphBuilder extends ElementBuilder
                {
                    public ParagraphBuilder(Document document, Element paragraphElement)
                    {
                        super(document, paragraphElement);
                    }

                    public ParagraphBuilder text(String text)
                    {
                        element().appendChild(document().createTextNode(text));
                        return this;
                    }

                    public ParagraphBuilder node(String type, Consumer<Element> consumer)
                    {
                        Element p = createChildElement(type);
                        consumer.accept(p);
                        return this;
                    }

                    public ParagraphBuilder span(Consumer<Element> consumer)
                    {
                        return node("span", consumer);
                    }

                    public ParagraphBuilder link(Consumer<Element> consumer)
                    {
                        return node("a", consumer);
                    }

                    public ParagraphBuilder stack(ItemStack stack)
                    {
                        return node("stack", e -> {
                            if (stack.hasTag())
                                e.setAttribute("nbt", stack.getTag().toString());
                            if (stack.getCount() > 1)
                                e.setAttribute("count", String.valueOf(stack.getCount()));
                            e.setAttribute("item", ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
                        });
                    }

                    public ParagraphBuilder image(ResourceLocation img, Consumer<Element> consumer)
                    {
                        return node("image", e -> {
                            consumer.accept(e);
                            e.setAttribute("src", img.toString());
                        });
                    }

                    public ParagraphBuilder element(Consumer<Element> consumer)
                    {
                        return node("element", consumer);
                    }
                }
            }
        }
    }
}
