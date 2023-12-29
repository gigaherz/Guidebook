package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.primitives.Ints;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.elements.ElementInline;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Rect;

import javax.annotation.Nullable;
import java.util.List;

public class TemplateElement extends ElementInline
{
    int index;
    private AttributeGetter attributes;

    public TemplateElement(boolean isFirstElement, boolean isLastElement)
    {
        super(isFirstElement, isLastElement);
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect page)
    {
        throw new IllegalStateException("Template elements must not be used directly");
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter originalAttributes)
    {
        super.parse(context, originalAttributes);

        this.attributes = AttributeGetter.copyOf(originalAttributes);

        String attr = this.attributes.getAttribute("index");
        if (attr != null)
        {
            index = Ints.tryParse(attr);

            this.attributes.removeAttribute("index");
        }
    }

    @Override
    public String toString(boolean complete)
    {
        return "<template .../>";
    }

    @Override
    public ElementInline copy()
    {
        TemplateElement temp = super.copy(new TemplateElement(isFirstElement, isLastElement));
        temp.index = index;
        temp.attributes = attributes;
        return temp;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        throw new IllegalStateException("Template elements must not be used directly");
    }

    @Nullable
    @Override
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        if (index >= sourceElements.size())
            return null;
        Element e = sourceElements.get(index).copy();
        e.parse(context, attributes);
        return e;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
