package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Maps;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import io.netty.util.AttributeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Map;
import java.util.function.Predicate;

public class TemplateLibrary
{
    public final Map<String, TemplateDefinition> templates = Maps.newHashMap();

    @Deprecated(forRemoval = true)
    public void parseLibrary(ParsingContext context, InputStream stream) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stream);

        parseLibrary(context, doc);
    }

    public void parseLibrary(ParsingContext context, Document doc) throws ParserConfigurationException, IOException, SAXException
    {
        doc.getDocumentElement().normalize();

        Node root = doc.getChildNodes().item(0);

        NodeList chaptersList = root.getChildNodes();
        for (int i = 0; i < chaptersList.getLength(); i++)
        {
            Node chapterItem = chaptersList.item(i);

            String nodeName = chapterItem.getNodeName();
            if (nodeName.equals("template"))
            {
                parseTemplateDefinition(context, chapterItem);
            }
        }
    }

    private void parseTemplateDefinition(ParsingContext parentContext, Node templateItem)
    {
        if (!templateItem.hasAttributes())
            return; // TODO: Throw error

        TemplateDefinition template = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null)
            return;

        templates.put(n.getTextContent(), template);

        var context = new ParsingContext()
        {
            @Override
            public Predicate<ConditionContext> getCondition(String name)
            {
                return null;
            }

            @Override
            public boolean loadedFromConfigFolder()
            {
                return parentContext.loadedFromConfigFolder();
            }

            @Override
            public DocumentBuilder xmlDocumentBuilder()
            {
                return parentContext.xmlDocumentBuilder();
            }
        };

        BookDocument.parseChildElements(context, templateItem.getChildNodes(), template.elements, templates, true, TextStyle.DEFAULT);

        for(var i =0;i<attributes.getLength();i++)
        {
            var attr = attributes.item(i);
            var key = attr.getNodeName();
            if (!key.equals("id"))
                template.attributes.put(key, attr.getTextContent());
        }
    }

    @Deprecated(forRemoval = true)
    public static TemplateLibrary get(ParsingContext context, String path, boolean useConfigFolder)
    {
        try
        {
            ResourceLocation loc = new ResourceLocation(path);

            // Prevents loading libraries from config folder if the book was found in resource packs.
            if (useConfigFolder && loc.getNamespace().equals("gbook"))
            {
                File booksFolder = BookRegistry.getBooksFolder();
                File file = new File(booksFolder, loc.getPath());
                if (file.exists() && file.isFile())
                {
                    try (InputStream stream = new FileInputStream(file))
                    {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(stream);

                        return get(context, doc);
                    }
                    catch (FileNotFoundException e)
                    {
                        // WUT? continue and try to load from resource pack
                    }
                }
            }

            Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(loc);
            try (InputStream stream = res.open())
            {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(stream);

                return get(context, doc);
            }
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            // TODO: Fail
            return new TemplateLibrary();
        }
    }

    @Deprecated(forRemoval = true)
    public static TemplateLibrary get(ParsingContext context, ResourceLocation path, Document doc)
    {
        return get(context, doc);
    }

    public static TemplateLibrary get(ParsingContext context, Document doc)
    {
        try
        {
            var lib = new TemplateLibrary();
            lib.parseLibrary(context, doc);
            return lib;
        }
        catch (IOException | ParserConfigurationException | SAXException e)
        {
            // TODO: Fail
            return new TemplateLibrary();
        }
    }
}
