package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.util.Point;
import gigaherz.guidebook.guidebook.util.Rect;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public abstract class Element
{
    public static final int VA_TOP = 0;
    public static final int VA_MIDDLE = 1;
    public static final int VA_BASELINE = 2;
    public static final int VA_BOTTOM = 3;

    public static final int POS_RELATIVE = 0;
    public static final int POS_ABSOLUTE = 1;
    public static final int POS_FIXED = 2;

    /* Positioning mode:
     * 0 = "relative" -- relative to the computed position (offset)
     * 1 = "absolute" -- relative to the containing Panel
     * 2 = "fixed" -- relative to the section
     */
    public int position = 0;

    public int x = 0;
    public int y = 0;
    public int w = 0;
    public int h = 0;

    public int z = 0;

    // in proportion to the element's calculated height
    public float baseline = 7 / 9f; // vanilla font has a baseline 7 pixels from the bottom, with 9px total height

    /* Vertical align mode -- only applicable within a paragraph
     * 0 = top
     * 1 = middle
     * 2 = baseline
     * 3 = bottom
     */
    public int verticalAlignment = VA_BASELINE;

    public Predicate<ConditionContext> condition;
    public boolean conditionResult;

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

    public void findTextures(Set<ResourceLocation> textures)
    {
    }

    public abstract Element copy();

    @Nullable
    public Element applyTemplate(IConditionSource book, List<Element> sourceElements)
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

    public Point applyPosition(Point point, Point parent)
    {
        switch (position)
        {
            case POS_RELATIVE:
                return new Point(point.x + x, point.y + y);
            case POS_ABSOLUTE:
                return new Point(parent.x + x, parent.y + y);
            case POS_FIXED:
                return new Point(x, y);
        }

        return new Point(point.x, point.y);
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

    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        x = getAttribute(attributes, "x", x);
        y = getAttribute(attributes, "y", y);
        z = getAttribute(attributes, "z", z);
        w = getAttribute(attributes, "w", w);
        h = getAttribute(attributes, "h", h);

        baseline = getAttribute(attributes, "baseline", baseline);

        Node attr = attributes.getNamedItem("align");
        if (attr != null)
        {
            String a = attr.getTextContent();
            switch (a)
            {
                case "relative":
                    position = 0;
                    break;
                case "absolute":
                    position = 1;
                    break;
                case "fixed":
                    position = 2;
                    break;
            }
        }

        attr = attributes.getNamedItem("vertical-align");
        if (attr != null)
        {
            String a = attr.getTextContent();
            switch (a)
            {
                case "top":
                    verticalAlignment = VA_TOP;
                    break;
                case "middle":
                    verticalAlignment = VA_MIDDLE;
                    break;
                case "baseline":
                    verticalAlignment = VA_BASELINE;
                    break;
                case "bottom":
                    verticalAlignment = VA_BOTTOM;
                    break;
            }
        }

        attr = attributes.getNamedItem("condition");
        if (attr != null)
        {
            condition = book.getCondition(attr.getTextContent());
        }
    }

    public void parseChildNodes(IConditionSource book, Node element)
    {
    }

    protected static String getAttribute(NamedNodeMap attributes, String name, String def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if (text != null)
                return text;
        }
        return def;
    }

    protected static ResourceLocation getAttribute(NamedNodeMap attributes, String name, ResourceLocation def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if (text != null)
                return new ResourceLocation(text);
        }
        return def;
    }

    protected static boolean getAttribute(NamedNodeMap attributes, String name, boolean def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                return true;
            if ("false".equals(text))
                return false;
        }
        return def;
    }

    protected static int getAttribute(NamedNodeMap attributes, String name, int def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            try
            {
                return Integer.parseInt(text);            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    protected static float getAttribute(NamedNodeMap attributes, String name, float def)
    {
        Node attr = attributes.getNamedItem(name);
        if (attr != null)
        {
            String text = attr.getTextContent();
            try
            {
                return Float.parseFloat(text);
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    protected static int getColorAttribute(NamedNodeMap attributes, String name, int def)
    {
        Node attr = attributes.getNamedItem("color");
        if (attr != null)
        {
            String c = attr.getTextContent();

            if (c.startsWith("#"))
                c = c.substring(1);

            try
            {
                if (c.length() <= 6)
                {
                    return 0xFF000000 | Integer.parseInt(c, 16);
                }
                else
                {
                    return Integer.parseInt(c, 16);
                }
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }

        return def;
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public abstract String toString(boolean complete);
}
