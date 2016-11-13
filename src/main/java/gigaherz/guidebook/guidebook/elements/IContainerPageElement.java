package gigaherz.guidebook.guidebook.elements;

import java.util.Collection;

public interface IContainerPageElement extends IPageElement
{
    Collection<IPageElement> getChildren();
}
