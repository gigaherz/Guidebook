package gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.elements.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class TemplateDefinition
{
    public final List<Element> elements = Lists.newArrayList();
    public NamedNodeMap attributes;

    public List<Element> applyTemplate(List<Element> sourceElements)
    {
        List<Element> output = Lists.newArrayList();
        for (Element element : elements)
        {
            Element t = element.applyTemplate(sourceElements);
            if (t != null)
                output.add(t);
        }
        return output;
    }
}
