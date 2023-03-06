package dev.gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDefinition implements NamedNodeMap
{
    public final List<Element> elements = Lists.newArrayList();
    public final Map<String, Node> attributes = new HashMap<>();

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
    public Node getNamedItem(String name)
    {
        return attributes.get(name);
    }

    @Override
    public Node setNamedItem(Node arg) throws DOMException
    {
        throw new IllegalStateException("Cannot modify");
    }

    @Override
    public Node removeNamedItem(String name) throws DOMException
    {
        throw new IllegalStateException("Cannot modify");
    }

    @Override
    public Node item(int index)
    {
        throw new IllegalStateException("Cannot get by index");
    }

    @Override
    public int getLength()
    {
        return attributes.size();
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException
    {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Node setNamedItemNS(Node arg) throws DOMException
    {
        throw new IllegalStateException("Cannot modify");
    }

    @Override
    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException
    {
        throw new IllegalStateException("Cannot modify");
    }
}
