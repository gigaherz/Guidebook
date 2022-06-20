package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import org.w3c.dom.NamedNodeMap;

public class ElementTitle extends ElementParagraph
{
    public ElementTitle()
    {
        alignment = ElementParagraph.ALIGN_CENTER;
        space = 4;
    }

    @Override
    public TextStyle childStyle(ParsingContext context, NamedNodeMap attributes, TextStyle defaultStyle)
    {
        return TextStyle.parse(attributes, new TextStyle(defaultStyle.color, true, false, true, false, false, null, 1.0f));
    }
}
