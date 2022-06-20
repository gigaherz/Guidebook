package dev.gigaherz.guidebook.guidebook.book;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPage;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PageData implements IParseable
{
    public final SectionRef ref;
    public String id;
    public Predicate<ConditionContext> condition;
    public boolean conditionResult;

    public final List<Element> elements = Lists.newArrayList();

    public PageData(SectionRef ref)
    {
        this.ref = ref;
    }

    public List<VisualPage> reflow(IBookGraphics rendering, Size pageSize)
    {
        VisualPage page = new VisualPage(ref);
        Rect pageBounds = new Rect(new Point(), pageSize);

        int top = 0;
        for (Element element : elements)
        {
            if (element.conditionResult)
            {
                top = element.reflow(page.children, rendering, new Rect(new Point(0, top), pageSize), pageBounds);
            }
        }

        return Collections.singletonList(page);
    }

    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for (Element element : elements)
        {
            anyChanged |= element.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    public boolean isEmpty()
    {
        return elements.stream().noneMatch(e -> e.conditionResult);
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        Node n = attributes.getNamedItem("id");
        if (n != null)
        {
            id = n.getTextContent();
            context.document().sectionsByName.put(id, ref);
            context.chapter().sectionsByName.put(id, ref.section);
        }

        n = attributes.getNamedItem("condition");
        if (n != null)
        {
            condition = context.getCondition(n.getTextContent());
        }
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        BookDocumentParser.parseChildElements(context, childNodes, elements, templates, true, defaultStyle);
    }
}
