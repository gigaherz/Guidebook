package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPageBreak;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class ElementBreak extends Element
{
    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect page)
    {
        list.add(new VisualPageBreak(new Size()));
        return bounds.position.y;
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
    }

    @Override
    public Element copy()
    {
        return new ElementBreak();
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    @Override
    public boolean supportsSpanLevel()
    {
        return false;
    }

    @Override
    public String toString(boolean complete)
    {
        return "<br/>";
    }
}
