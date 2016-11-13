package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Link extends Paragraph implements IClickablePageElement
{
    public PageRef target;
    public int colorHover = 0xFF77cc66;

    public boolean isHovering;
    public Rectangle bounds;

    public Link(String text)
    {
        super(text);
        underline = true;
        color = 0xFF7766cc;
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }

    @Override
    public void click(IBookGraphics nav)
    {
        nav.navigateTo(target);
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        bounds = nav.getStringBounds(text, left, top);

        return nav.addStringWrapping(left + indent, top, text, isHovering ? colorHover : color, alignment) + space;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();

            if (ref.indexOf(':') >= 0)
            {
                String[] parts = ref.split(":");
                target = new PageRef(parts[0], parts[1]);
            }
            else
            {
                target = new PageRef(ref, null);
            }
        }
    }

    @Override
    public IPageElement copy()
    {
        Link link = new Link(text);
        link.alignment = alignment;
        link.color = color;
        link.indent = indent;
        link.space = space;
        link.bold = bold;
        link.italics = italics;
        link.underline = underline;

        link.target = target.copy();
        link.colorHover = colorHover;

        return link;
    }
}
