package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.SizedSegment;

import java.util.List;

public interface IParagraphElement extends IPageElement
{
    IParagraphElement copy();

    List<SizedSegment> measure(IBookGraphics nav, int width, int firstLineWidth);
}
