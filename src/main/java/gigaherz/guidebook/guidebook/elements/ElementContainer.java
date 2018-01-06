package gigaherz.guidebook.guidebook.elements;

import java.util.Collection;

public abstract class ElementContainer extends Element
{
    protected ElementContainer(int defaultPositionMode)
    {
        super(defaultPositionMode);
    }

    public abstract Collection<Element> getChildren();
}
