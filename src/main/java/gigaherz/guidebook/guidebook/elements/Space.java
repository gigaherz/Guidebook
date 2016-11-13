package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Space implements IPageElement
{
    public boolean asPercent;
    public int space;

    public Space()
    {
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        return asPercent ? nav.getPageHeight() * space / 100 : space;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("height");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                asPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            space = Ints.tryParse(t);
        }
    }

    @Override
    public IPageElement copy()
    {
        Space space = new Space();
        space.asPercent = asPercent;
        space.space = this.space;
        return space;
    }
}
