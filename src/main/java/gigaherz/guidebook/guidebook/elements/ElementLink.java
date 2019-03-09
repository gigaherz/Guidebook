package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.SectionRef;
import gigaherz.guidebook.guidebook.util.LinkHelper;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;
import java.util.stream.Collectors;

public class ElementLink extends ElementSpan
{
    public LinkContext ctx = new LinkContext();

    public ElementLink(boolean isFirstElement, boolean isLastElement, ElementInline... addRuns)
    {
        super(isFirstElement, isLastElement, addRuns);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> texts = super.measure(nav, width, firstLineWidth);
        texts.forEach(e -> {
            if (e instanceof LinkHelper.ILinkable)
            {
                LinkHelper.ILinkable linkable = (LinkHelper.ILinkable) e;
                linkable.setLinkContext(ctx);
            }
        });
        return texts;
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        super.parse(book, attributes);

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();
            ctx.target = SectionRef.fromString(ref);
        }

        attr = attributes.getNamedItem("href");
        if (attr != null)
        {
            ctx.textTarget = attr.getTextContent();
            ctx.textAction = "openUrl";
        }

        attr = attributes.getNamedItem("text");
        if (attr != null)
        {
            ctx.textTarget = attr.getTextContent();
        }

        attr = attributes.getNamedItem("action");
        if (attr != null)
        {
            ctx.textAction = attr.getTextContent();
        }
    }

    @Override
    public ElementInline copy()
    {
        ElementLink link = super.copy(new ElementLink(isFirstElement, isLastElement));
        for(ElementInline run : inlines)
        {
            link.inlines.add(run.copy());
        }

        link.ctx = ctx.copy();

        return link;
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return String.format("<link %s ...>%s</link>", ctx.toString(), inlines.stream().map(Object::toString).collect(Collectors.joining()));
    }
}
