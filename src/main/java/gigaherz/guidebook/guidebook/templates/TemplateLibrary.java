package gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Maps;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.elements.TextStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
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

public class TemplateLibrary implements IConditionSource
{
    public final Map<String, TemplateDefinition> templates = Maps.newHashMap();

    public void parseLibrary(InputStream stream) throws ParserConfigurationException, IOException, SAXException
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
                parseTemplateDefinition(chapterItem);
            }
        }
    }

    private void parseTemplateDefinition(Node templateItem)
    {
        if (!templateItem.hasAttributes())
            return; // TODO: Throw error

        TemplateDefinition page = new TemplateDefinition();

        NamedNodeMap attributes = templateItem.getAttributes();
        Node n = attributes.getNamedItem("id");
        if (n == null)
            return;

        templates.put(n.getTextContent(), page);

        BookDocument.parseChildElements(this, templateItem, page.elements, templates, true, TextStyle.DEFAULT);

        attributes.removeNamedItem("id");
        page.attributes = attributes;
    }

    public static Map<String, TemplateLibrary> LIBRARIES = Maps.newHashMap();

    public static void clear()
    {
        LIBRARIES.clear();
    }

    public static TemplateLibrary get(String path, boolean useConfigFolder)
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
                            lib.parseLibrary(stream);
                            LIBRARIES.put(path, lib);
                            return lib;
                        }
                        catch (FileNotFoundException e)
                        {
                            // WUT? continue and try to load from resource pack
                        }
                    }
                }

                IResource res = Minecraft.getMinecraft().getResourceManager().getResource(loc);
                try (InputStream stream = res.getInputStream())
                {
                    lib.parseLibrary(stream);
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

    @Override
    public Predicate<ConditionContext> getCondition(String name)
    {
        return null;
    }
}
