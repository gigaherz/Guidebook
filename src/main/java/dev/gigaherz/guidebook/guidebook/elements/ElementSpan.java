package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ElementSpan extends ElementInline
{
    public final List<ElementInline> inlines = Lists.newArrayList();

    public ElementSpan(boolean isFirstElement, boolean isLastElement, ElementInline... addRuns)
    {
        super(isFirstElement, isLastElement);
        Collections.addAll(inlines, addRuns);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> visuals = Lists.newArrayList();

        for (ElementInline run : inlines)
        {
            visuals.addAll(run.measure(nav, width, firstLineWidth));
        }
        return visuals;
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        super.parse(context, attributes);
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return "<span ...>" + inlines.stream().map(Object::toString).collect(Collectors.joining()) + "</span>";
    }

    @Override
    public ElementInline copy()
    {
        ElementSpan span = super.copy(new ElementSpan(isFirstElement, isLastElement));
        for (ElementInline run : inlines)
        {
            span.inlines.add(run.copy());
        }
        return span;
    }

    public static ElementSpan of(String text, boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        ElementText inline = new ElementText(text, isFirstElement, isLastElement, style);
        return new ElementSpan(isFirstElement, isLastElement, inline);
    }

    public static ElementSpan of(String text, TextStyle style)
    {
        return of(text, true, true, style);
    }
}
