package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.BookDocumentParser;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.book.SectionRef;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.LinkHelper;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;
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
            if (e instanceof LinkHelper.ILinkable linkable)
            {
                linkable.setLinkContext(ctx);
            }
        });
        return texts;
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        super.parse(context, attributes);

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
    public TextStyle childStyle(ParsingContext context, NamedNodeMap attributes, TextStyle defaultStyle)
    {
        return TextStyle.parse(attributes, TextStyle.LINK);
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        List<ElementInline> elementList = Lists.newArrayList();
        BookDocumentParser.parseRunElements(context, childNodes, elementList, defaultStyle);
        inlines.addAll(elementList);
    }

    @Override
    public ElementInline copy()
    {
        ElementLink link = super.copy(new ElementLink(isFirstElement, isLastElement));
        for (ElementInline run : inlines)
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
