package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Element implements IParseable
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

    public Point applyPosition(Point point, Point parent)
    {
        return switch (position)
        {
            case POS_RELATIVE -> new Point(point.x + x, point.y + y);
            case POS_ABSOLUTE -> new Point(parent.x + x, parent.y + y);
            case POS_FIXED -> new Point(x, y);
            default -> new Point(point.x, point.y);
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
        x = getAttribute(attributes, "x", x);
        y = getAttribute(attributes, "y", y);
        z = getAttribute(attributes, "z", z);
        w = getAttribute(attributes, "w", w);
        h = getAttribute(attributes, "h", h);

        baseline = getAttribute(attributes, "baseline", baseline);

        Node attr = attributes.getNamedItem("align");
        if (attr != null)
        {
            position = switch (attr.getTextContent())
            {
                case "relative" -> 0;
                case "absolute" -> 1;
                case "fixed" -> 2;
                default -> position;
            };
        }

        attr = attributes.getNamedItem("vertical-align");
        if (attr != null)
        {
            verticalAlignment = switch (attr.getTextContent())
            {
                case "top" -> VA_TOP;
                case "middle" -> VA_MIDDLE;
                case "baseline" -> VA_BASELINE;
                case "bottom" -> VA_BOTTOM;
                default -> verticalAlignment;
            };
        }

        attr = attributes.getNamedItem("condition");
        if (attr != null)
        {
            condition = context.getCondition(attr.getTextContent());
        }
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
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
                return Integer.parseInt(text);
            }
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

    // For an exploded description of this regex, see: https://regex101.com/r/qXfiDc/1/
    private static final Pattern COLOR_PARSE = Pattern.compile("^#?(?:(?<c3>[0-9a-f]{3})|(?<c6>[0-9a-f]{6})|(?<c8>[0-9a-f]{8})|(?<rgb>rgb\\((?<r>[0-9]{1,3}),(?<g>[0-9]{1,3}),(?<b>[0-9]{1,3})\\))|(?<rgba>rgba\\((?<r2>[0-9]{1,3}),(?<g2>[0-9]{1,3}),(?<b2>[0-9]{1,3}),(?<a2>[0-9]{1,3})\\)))$");

    protected static int getColorAttribute(NamedNodeMap attributes, int def)
    {
        Node attr = attributes.getNamedItem("color");
        if (attr != null)
        {
            String c = attr.getTextContent();

            Matcher m = COLOR_PARSE.matcher(c);
            if (m.matches())
            {
                try
                {
                    String value;
                    if ((value = m.group("c3")) != null)
                    {
                        String s = new String(new char[]{value.charAt(0), value.charAt(0), value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2)});
                        return 0xFF000000 | Integer.parseInt(s, 16);
                    }
                    else if ((value = m.group("c6")) != null)
                    {
                        return 0xFF000000 | Integer.parseInt(value, 16);
                    }
                    else if ((value = m.group("c8")) != null)
                    {
                        return Integer.parseUnsignedInt(value, 16);
                    }
                    else if (m.group("rgb") != null)
                    {
                        int r = Integer.parseUnsignedInt(m.group("r"));
                        int g = Integer.parseUnsignedInt(m.group("g"));
                        int b = Integer.parseUnsignedInt(m.group("b"));
                        if (r > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (g > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (b > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        return 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                    else if (m.group("rgba") != null)
                    {
                        int r = Integer.parseUnsignedInt(m.group("r2"));
                        int g = Integer.parseUnsignedInt(m.group("g2"));
                        int b = Integer.parseUnsignedInt(m.group("b2"));
                        int a = Integer.parseUnsignedInt(m.group("a2"));
                        if (r > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (g > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (b > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (a > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }

                    GuidebookMod.logger.warn("Unrecognized color value {}, ignored.", c);
                }
                catch (NumberFormatException e)
                {
                    GuidebookMod.logger.warn("Color value not valid {}", c, e);
                    // ignored
                }
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
