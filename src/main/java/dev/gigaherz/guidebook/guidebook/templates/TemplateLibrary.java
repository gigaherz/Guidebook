package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Maps;
import dev.gigaherz.guidebook.guidebook.book.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.book.BookDocumentParser;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
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

    public void parseLibrary(ParsingContext context, InputStream stream) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stream);

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
        ParsingContext.Wrapper context = new ParsingContext.Wrapper(parentContext)
        {
            @Override
            public BookDocument document()
            {
                return null;
            }
        };
        BookDocumentParser.parseTemplateDefinition(context, templateItem, templates);
    }

    public static Map<String, TemplateLibrary> LIBRARIES = Maps.newHashMap();

    public static void clear()
    {
        LIBRARIES.clear();
    }

    public static TemplateLibrary get(ParsingContext context, String path, boolean useConfigFolder)
    {
        TemplateLibrary lib = LIBRARIES.get(path);
        if (lib == null)
        {
            try
            {
                lib = new TemplateLibrary();

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
                            lib.parseLibrary(context, stream);
                            LIBRARIES.put(path, lib);
                            return lib;
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
                    lib.parseLibrary(context, stream);
                    LIBRARIES.put(path, lib);
                }
            }
            catch (IOException | ParserConfigurationException | SAXException e)
            {
                // TODO: Fail
            }
        }

        return lib;
    }

    public static TemplateLibrary get(ParsingContext context, ResourceLocation path, Document doc)
    {
        TemplateLibrary lib = LIBRARIES.get(path.toString());
        if (lib == null)
        {
            try
            {
                lib = new TemplateLibrary();
                lib.parseLibrary(context, doc);
                LIBRARIES.put(path.toString(), lib);
            }
            catch (IOException | ParserConfigurationException | SAXException e)
            {
                // TODO: Fail
            }
        }

        return lib;
    }
}
