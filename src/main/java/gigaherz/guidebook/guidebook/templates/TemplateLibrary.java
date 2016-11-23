package gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Maps;
import gigaherz.guidebook.guidebook.BookDocument;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TemplateLibrary
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

        BookDocument.parseChildElements(templateItem, page.elements, templates);

        attributes.removeNamedItem("id");
        page.attributes = attributes;
    }

    public static Map<String, TemplateLibrary> LIBRARIES = Maps.newHashMap();

    public static void clear()
    {
        LIBRARIES.clear();
    }

    public static TemplateLibrary get(String path)
    {
        TemplateLibrary lib = LIBRARIES.get(path);
        if (lib == null)
        {
            try
            {
                lib = new TemplateLibrary();

                IResource res = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(path));
                InputStream stream = res.getInputStream();
                lib.parseLibrary(stream);

                LIBRARIES.put(path, lib);
            }
            catch (IOException | ParserConfigurationException | SAXException e)
            {
                // TODO: Fail
            }
        }

        return lib;
    }
}
