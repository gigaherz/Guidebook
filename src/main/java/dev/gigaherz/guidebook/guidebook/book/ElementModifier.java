package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import org.w3c.dom.NamedNodeMap;

@FunctionalInterface
public interface ElementModifier
{
    void modify(ParsingContext context, Element element, String value, NamedNodeMap attributes, TextStyle defaultStyle);

    default boolean canModify(ParsingContext context, Element element)
    {
        return true;
    }
}
