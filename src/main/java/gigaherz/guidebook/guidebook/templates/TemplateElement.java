package gigaherz.guidebook.guidebook.templates;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.IPageElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.List;

public class TemplateElement implements IPageElement
{
    int index;
    private NamedNodeMap attributes;

    @Nullable
    public IPageElement apply(List<IPageElement> sourceElements)
    {
        IPageElement e = sourceElements.get(index).copy();
        e.parse(attributes);
        return e;
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top, int width)
    {
        throw new IllegalStateException("Template elements must not be used directly");
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        this.attributes = attributes;

        Node attr = attributes.getNamedItem("index");
        if (attr != null)
        {
            index = Ints.tryParse(attr.getTextContent());
        }

        attributes.removeNamedItem("index");
    }

    @Override
    public IPageElement copy()
    {
        throw new IllegalStateException("Template elements can not be nested");
    }
}
