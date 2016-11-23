package gigaherz.guidebook.guidebook.templates;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.elements.IPageElement;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class TemplateDefinition
{
    public final List<IPageElement> elements = Lists.newArrayList();
    public NamedNodeMap attributes;

    public List<IPageElement> applyTemplate(List<IPageElement> sourceElements)
    {
        List<IPageElement> output = Lists.newArrayList();
        for (IPageElement element : elements)
        {
            if (element instanceof TemplateElement)
            {
                IPageElement result = ((TemplateElement) element).apply(sourceElements);
                if (result != null)
                    output.add(result);
            }
            else
            {
                output.add(element.copy());
            }
        }
        return output;
    }
}
