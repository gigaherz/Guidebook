package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Point2I;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.client.resources.model.Material;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class Element implements IParseable
{
    public enum VerticalAlignment
    {
        TOP,
        MIDDLE,
        BASELINE,
        BOTTOM
    }

    public enum Position
    {
        /* relative to the computed position (offset) */
        RELATIVE,
        /* relative to the containing Panel */
        ABSOLUTE,
        /* relative to the section */
        FIXED
    }
    public static final int VA_MIDDLE = 1;
    public static final int VA_BASELINE = 2;
    public static final int VA_BOTTOM = 3;

    public Position position = Position.RELATIVE;

    public int x = 0;
    public int y = 0;
    public int w = 0;
    public int h = 0;

    public int z = 0;

    // in proportion to the element's calculated height
    public float baseline = 7 / 9f; // vanilla font has a baseline 7 pixels from the bottom, with 9px total height

    /* Vertical align mode -- only applicable within a paragraph */
    public VerticalAlignment verticalAlignment = VerticalAlignment.BASELINE;

    public Predicate<ConditionContext> condition;
    public boolean conditionResult;

    private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s+$");
    protected static boolean isContentNode(Node node)
    {
        if (node.getNodeType() == Node.ELEMENT_NODE)
            return true;
        if (node.getNodeType() == Node.COMMENT_NODE)
            return false;
        if (node.getNodeType() != Node.TEXT_NODE)
            return true;

        var str = node.getTextContent();
        if (str== null || str.length() == 0) return false;

        return !WHITESPACE_ONLY.matcher(str).matches();
    }

    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        return conditionResult != oldValue;
    }

    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return Collections.emptyList();
    }

    public abstract int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect page);

    public void findTextures(Set<Material> textures)
    {
    }

    public abstract Element copy();

    @Nullable
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        return copy();
    }

    public boolean supportsPageLevel()
    {
        return false;
    }

    public boolean supportsSpanLevel()
    {
        return true;
    }

    public Point2I applyPosition(Point2I point, Point2I parent)
    {
        return switch (position)
        {
            case RELATIVE -> new Point2I(point.x() + x, point.y() + y);
            case ABSOLUTE -> new Point2I(parent.x() + x, parent.y() + y);
            case FIXED -> new Point2I(x, y);
        };
    }

    protected <T extends Element> T copy(T other)
    {
        other.position = position;
        other.x = x;
        other.y = y;
        other.z = z;
        other.w = w;
        other.h = h;
        return other;
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        x = IParseable.getAttribute(attributes, "x", x);
        y = IParseable.getAttribute(attributes, "y", y);
        z = IParseable.getAttribute(attributes, "z", z);
        w = IParseable.getAttribute(attributes, "w", w);
        h = IParseable.getAttribute(attributes, "h", h);
        baseline = IParseable.getAttribute(attributes, "baseline", baseline);
        position = IParseable.getAttribute(attributes, "position", position, Position.class);
        verticalAlignment = IParseable.getAttribute(attributes, "verticalAlignment", verticalAlignment, VerticalAlignment.class);

        Node attr = attributes.getNamedItem("condition");
        if (attr != null)
        {
            condition = context.getCondition(attr.getTextContent());
        }
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public abstract String toString(boolean complete);
}
