package gigaherz.guidebook.guidebook.elements;

import java.util.Collection;

public interface IContainerParagraphElement extends IPageElement
{
    Collection<IParagraphElement> getChildren();
}
