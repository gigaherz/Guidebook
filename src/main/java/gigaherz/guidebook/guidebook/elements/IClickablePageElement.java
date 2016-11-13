package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;

public interface IClickablePageElement extends IBoundedPageElement
{
    void click(IBookGraphics nav);
}
