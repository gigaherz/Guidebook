package dev.gigaherz.guidebook.guidebook.book;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualChapter;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Size;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ChapterData implements IParseable
{
    public final int num;
    public String id;
    public Predicate<ConditionContext> condition;
    public boolean conditionResult;

    public final List<PageData> sections = Lists.newArrayList();
    public final Map<String, Integer> sectionsByName = Maps.newHashMap();

    ChapterData(int num)
    {
        this.num = num;
    }

    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for (PageData section : sections)
        {
            anyChanged |= section.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    public void reflow(IBookGraphics rendering, VisualChapter ch, Size pageSize)
    {
        for (PageData section : sections)
        {
            if (!section.conditionResult || section.isEmpty())
            {
                continue;
            }

            if (!Strings.isNullOrEmpty(section.id))
            {
                ch.pagesByName.put(section.id, ch.pages.size());
            }

            ch.pages.addAll(section.reflow(rendering, pageSize));
        }
    }

    public boolean isEmpty()
    {
        return sections.stream().noneMatch(s -> s.conditionResult && !s.isEmpty());
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        Node n = attributes.getNamedItem("id");
        if (n != null)
        {
            id = n.getTextContent();
            context.document().chaptersByName.put(id, num);
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

    }
}
