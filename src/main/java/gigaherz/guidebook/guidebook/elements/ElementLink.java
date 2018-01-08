package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualLink;
import gigaherz.guidebook.guidebook.drawing.VisualText;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public class ElementLink extends ElementSpan
{
    public String webTarget;
    public PageRef target;
    public int colorHover = 0xFF77cc66;

    public ElementLink(String text)
    {
        super(text);
        underline = true;
        color = 0xFF7766cc;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> texts = super.measure(nav, width, firstLineWidth);
        List<VisualElement> links = Lists.newArrayList();
        VisualLink.SharedHoverContext ctx = null;
        for(VisualElement e : texts)
        {
            if (!(e instanceof VisualText))
                continue; // WTF?

            VisualText text = (VisualText)e;

            VisualLink link = new VisualLink(text.text, text.size, scale);
            if (ctx == null) ctx = link.hoverContext;
            else link.hoverContext = ctx;
            link.color = color;
            link.target = target;
            link.webTarget = webTarget;
            link.colorHover = colorHover;
            links.add(link);
        }
        return links;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();
            target = PageRef.fromString(ref);
        }

        attr = attributes.getNamedItem("href");
        if (attr != null)
        {
            webTarget = attr.getTextContent();
        }
    }

    @Override
    public Element copy()
    {
        ElementLink link = super.copy(new ElementLink(text));
        link.color = color;
        link.bold = bold;
        link.italics = italics;
        link.underline = underline;

        link.target = target.copy();
        link.webTarget = webTarget;
        link.colorHover = colorHover;

        return link;
    }
}
