package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class TemplateDefinition
{
    public final List<Element> elements = Lists.newArrayList();
    public NamedNodeMap attributes;

    public List<Element> applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        List<Element> output = Lists.newArrayList();
        for (Element element : elements)
        {
            Element t = element.applyTemplate(context, sourceElements);
            if (t != null)
                output.add(t);
        }
        return output;
    }
}
