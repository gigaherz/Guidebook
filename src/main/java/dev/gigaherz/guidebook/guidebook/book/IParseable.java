package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Color;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

public interface IParseable
{
    void parse(ParsingContext context, NamedNodeMap attributes);

    void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle);

    default TextStyle childStyle(ParsingContext context, NamedNodeMap attributes, TextStyle defaultStyle)
    {
        return defaultStyle;
    }

    static String getAttribute(NamedNodeMap attributes, String name, String def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if (text != null)
                return text;
        }
        return def;
    }

    static ResourceLocation getAttribute(NamedNodeMap attributes, String name, ResourceLocation def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if (text != null)
                return new ResourceLocation(text);
        }
        return def;
    }

    static boolean getAttribute(NamedNodeMap attributes, String name, boolean def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            return text.isEmpty() || Boolean.parseBoolean(text);
        }
        return def;
    }

    static int getAttribute(NamedNodeMap attributes, String name, int def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            try
            {
                return Integer.parseInt(text);
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    static float getAttribute(NamedNodeMap attributes, String name, float def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            try
            {
                return Float.parseFloat(text);
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    static <T extends Enum<T>> T getAttribute(NamedNodeMap attributes, String name, T def, Class<T> enumClass)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if (text != null)
            {
                try
                {
                    return T.valueOf(enumClass, text.toUpperCase());
                }
                catch (IllegalArgumentException ignored)
                {
                }
            }
        }
        return def;
    }

    static Color getAttribute(NamedNodeMap attributes, String name, Color def)
    {
        Node attr = attributes.getNamedItem(name);

        if (attr == null) return def;

        try
        {
            return Color.parse(attr.getTextContent());
        }
        catch (Color.ColorParseException e)
        {
            GuidebookMod.logger.warn("Color value not valid {}", e.getColorString(), e);
            return def;
        }
    }
}
