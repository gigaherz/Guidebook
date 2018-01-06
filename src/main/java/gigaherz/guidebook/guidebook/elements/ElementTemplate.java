package gigaherz.guidebook.guidebook.elements;

import java.util.List;

public class ElementTemplate extends ElementPanel
{
    public ElementTemplate(int defaultPositionMode)
    {
        super(defaultPositionMode);
    }

    public ElementTemplate(int defaultPositionMode, List<Element> innerElements)
    {
        super(defaultPositionMode, innerElements);
    }

    @Override
    public Element copy()
    {
        return super.copy(new ElementTemplate(position, innerElements));
    }

    @Override
    public Element applyTemplate(List<Element> sourceElements)
    {
        ElementTemplate paragraph = super.copy(new ElementTemplate(position));
        paragraph.space = space;
        for(Element element : innerElements)
        {
            paragraph.innerElements.add(element.applyTemplate(sourceElements));
        }
        return paragraph;
    }
}
