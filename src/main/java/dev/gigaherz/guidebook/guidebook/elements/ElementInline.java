package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public abstract class ElementInline extends Element
{
    protected final boolean isFirstElement;
    protected final boolean isLastElement;
    private ElementParagraph temporaryParagraph = null;

    protected ElementInline(boolean isFirstElement, boolean isLastElement)
    {
        this.isFirstElement = isFirstElement;
        this.isLastElement = isLastElement;
    }

    @Override
    public abstract List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth);

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        if (temporaryParagraph == null)
        {
            temporaryParagraph = new ElementParagraph();
            temporaryParagraph.inlines.add(this);
        }
        return temporaryParagraph.reflow(paragraph, nav, bounds, page);
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        super.parse(context, attributes);
    }

    @Override
    public abstract ElementInline copy();
}
