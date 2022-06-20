package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import org.w3c.dom.NamedNodeMap;
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
}
