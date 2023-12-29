package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPanel;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.resources.model.Material;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ElementPanel extends Element
{
    public final List<Element> innerElements;
    public boolean asPercent;
    public Integer space;
    public PanelMode mode = PanelMode.DEFAULT;

    public enum PanelMode
    {
        DEFAULT,
        FLOW;
    }

    public ElementPanel()
    {
        this.innerElements = Lists.newArrayList();
    }

    public ElementPanel(List<Element> innerElements)
    {
        this.innerElements = Lists.newArrayList(innerElements);
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        super.parse(context, attributes);

        String attr = attributes.getAttribute("height");
        if (attr != null)
        {
            String t = attr;
            if (t.endsWith("%"))
            {
                asPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            space = Ints.tryParse(t);
        }

        attr = attributes.getAttribute("mode");
        if (attr != null)
        {
            String t = attr;
            try
            {
                mode = PanelMode.valueOf(t.toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                mode = PanelMode.DEFAULT;
            }
        }
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        BookDocument.parseChildElements(context, childNodes, innerElements, templates, true, defaultStyle);
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return "<panel ...>" + innerElements.stream().map(Object::toString).collect(Collectors.joining()) + "</panel>";
    }

    @Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for (Element element : innerElements)
        {
            anyChanged |= element.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        List<VisualElement> visuals = Lists.newArrayList();

        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

        int top = adjustedPosition.y();
        if (mode == PanelMode.DEFAULT)
        {
            for (Element element : innerElements)
            {
                if (element.conditionResult)
                {
                    element.reflow(visuals, nav, adjustedBounds, pageBounds);
                }
            }
            top += adjustedBounds.size.height();
        }
        else
        {
            for (Element element : innerElements)
            {
                if (element.conditionResult)
                {
                    Point tempPos = new Point(adjustedPosition.x(), top);
                    Size tempSize = new Size(adjustedBounds.size.width(), adjustedBounds.size.height() - (top - adjustedPosition.y()));
                    Rect tempBounds = new Rect(tempPos, tempSize);

                    top = element.reflow(visuals, nav, tempBounds, pageBounds);
                }
            }
        }

        int height = 0;
        if (position != POS_RELATIVE)
        {
            top = bounds.position.y();
        }
        else if (space != null)
        {
            height = asPercent ? (space * bounds.size.height() / 100) : space;
            top = adjustedPosition.y() + height;
        }

        if (visuals.size() > 0)
        {
            int x1 = Integer.MAX_VALUE;
            int y1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            int y2 = Integer.MIN_VALUE;
            for(var e : visuals)
            {
                x1 = Math.min(x1, e.position.x());
                y1 = Math.min(y1, e.position.y());
                x2 = Math.max(x2, e.position.x()+e.size.width());
                y2 = Math.max(y2, e.position.y()+e.size.height());
            }

            VisualPanel p = new VisualPanel(new Size(x2-x1,Math.max(y2-y1, height)), position, baseline, verticalAlignment);
            p.position = new Point(x1,y1);

            p.children.addAll(visuals);

            list.add(p);
        }
        else
        {
            // Just space

            VisualPanel p = new VisualPanel(new Size(0,height), position, baseline, verticalAlignment);
            p.position = adjustedPosition;

            list.add(p);
        }

        return top;
    }

    @Override
    public void findTextures(Set<Material> textures)
    {
        for (Element child : innerElements)
        {
            child.findTextures(textures);
        }
    }

    @Override
    public Element copy()
    {
        ElementPanel space = super.copy(new ElementPanel());
        space.asPercent = asPercent;
        space.space = this.space;
        return space;
    }

    @Nullable
    @Override
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        if (innerElements.size() == 0)
            return null;

        ElementPanel panel = super.copy(new ElementPanel());
        panel.space = space;
        panel.asPercent = asPercent;
        for (Element element : innerElements)
        {
            Element t = element.applyTemplate(context, sourceElements);
            if (t != null)
                panel.innerElements.add(t);
        }

        if (panel.innerElements.size() == 0)
            return null;

        return panel;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
