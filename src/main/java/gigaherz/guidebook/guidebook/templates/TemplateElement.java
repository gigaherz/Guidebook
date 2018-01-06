package gigaherz.guidebook.guidebook.templates;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public class TemplateElement extends Element
{
    int index;
    private NamedNodeMap attributes;

    public TemplateElement(int defaultPositionMode)
    {
        super(defaultPositionMode);
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, int left, int top, int width, int height)
    {
        throw new IllegalStateException("Template elements must not be used directly");
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        this.attributes = attributes;

        Node attr = attributes.getNamedItem("index");
        if (attr != null)
        {
            index = Ints.tryParse(attr.getTextContent());
        }

        attributes.removeNamedItem("index");
    }

    @Override
    public Element copy()
    {
        TemplateElement temp = new TemplateElement(position);
        temp.index = index;
        temp.attributes = attributes;
        return temp;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        throw new IllegalStateException("Template elements must not be used directly");
    }

    @Override
    public Element applyTemplate(List<Element> sourceElements)
    {
        Element e = sourceElements.get(index).copy();
        e.parse(attributes);
        return e;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
