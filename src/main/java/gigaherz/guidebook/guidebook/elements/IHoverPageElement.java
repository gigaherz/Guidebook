package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;

public interface IHoverPageElement extends IBoundedPageElement
{
    void mouseOver(IBookGraphics info, int x, int y);
}
