package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDefinition implements AttributeGetter
{
    public final List<Element> elements = Lists.newArrayList();
    public final Map<String, String> attributes = new HashMap<>();

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

    @Override
    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }

    @Nullable
    @Override
    public String getAttribute(String name)
    {
        return attributes.get(name);
    }

    @Override
    public void removeAttribute(String name)
    {
        throw new IllegalStateException("Cannot modify");
    }

    @Override
    public AttributeGetter copy()
    {
        return new MapGetter(new HashMap<>(attributes));
    }
}
