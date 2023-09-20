package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualDebugArea;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPanel;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.network.chat.Component;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ElementParagraph extends Element
{
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    public int alignment = ALIGN_LEFT;
    public int indent = 0; // First line?
    public int indentFirstLine = 0; // First line?
    public int space = 2;

    public final List<ElementInline> inlines = Lists.newArrayList();

    @Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for (Element element : inlines)
        {
            anyChanged |= element.reevaluateConditions(ctx);
        }

        return anyChanged;
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        int currentLineTop = adjustedPosition.y();
        int currentLineLeft = indentFirstLine;
        int currentIndent = indentFirstLine;

        var list = new ArrayList<VisualElement>();

        int maxWidth = 0;


        for (Element element : inlines)
        {
            int firstLineWidth = bounds.size.width() - currentLineLeft - indent - indentFirstLine;
            List<VisualElement> pieces = element.measure(nav,
                    bounds.size.width() - indent,
                    firstLineWidth);

            if (pieces.size() < 1)
                continue;

            for (VisualElement current : pieces)
            {
                Size size = current.size;

                boolean isLineBreak = "\n".equals(current.getText().getString());

                if (isLineBreak || (currentLineLeft + size.width() > bounds.size.width() && currentLineLeft > 0))
                {
                    var pos = processAlignment(list, bounds.size.width() - currentIndent, currentLineLeft, currentLineTop, 0);

                    var line = new VisualPanel(new Size(currentLineLeft, pos.y()), 0, 0, 0/*, Component.literal("Paragraph line")*/);
                    line.position = new Point(pos.x() + indent, currentLineTop);
                    line.children.addAll(list);
                    list.clear();

                    paragraph.add(line);

                    maxWidth = Math.max(maxWidth, currentLineLeft + pos.x() + indent);

                    currentLineTop += pos.y();
                    currentLineLeft = 0;
                    currentIndent = indent;
                }

                if (isLineBreak)
                    continue;

                current.position = element.applyPosition(new Point(adjustedPosition.x() + currentLineLeft + indent, currentLineTop), bounds.position);

                if (size.width() > 0)
                    currentLineLeft += size.width();

                if (currentLineLeft > bounds.size.width())
                {
                    var pos = processAlignment(list, bounds.size.width() - currentIndent, currentLineLeft, currentLineTop, 0);

                    var line = new VisualPanel(new Size(currentLineLeft, pos.y()), 0, 0, 0/*, Component.literal("Paragraph line")*/);
                    line.position = new Point(pos.x() + indent, currentLineTop);
                    line.children.addAll(list);
                    list.clear();

                    paragraph.add(line);

                    maxWidth = Math.max(maxWidth, currentLineLeft + pos.x() + indent);

                    currentLineTop += pos.y();
                    currentLineLeft = 0;
                }

                list.add(current);
            }
        }

        var pos = processAlignment(list, bounds.size.width() - currentIndent, currentLineLeft, currentLineTop, 0);

        var line = new VisualPanel(new Size(currentLineLeft, pos.y()), 0, 0, 0/*, Component.literal("Paragraph line")*/);
        line.position = new Point(pos.x() + indent, currentLineTop);
        line.children.addAll(list);
        list.clear();

        paragraph.add(line);

        maxWidth = Math.max(maxWidth, currentLineLeft + pos.x() + indent);

        if (VisualDebugArea.INJECT_DEBUG)
        {
            var area2 = new VisualDebugArea(new Size(maxWidth, currentLineTop + pos.y() - adjustedPosition.y()), 0, 0, 0, Component.literal("Paragraph"));
            area2.position = new Point(adjustedPosition.x(), adjustedPosition.y());

            paragraph.add(area2);
        }

        if (position != POS_RELATIVE)
            return bounds.position.y();
        return currentLineTop + pos.y() + space;
    }

    private Point processAlignment(List<VisualElement> paragraph, int width, int currentLineLeft, int currentLineTop, int firstInLine)
    {
        int leftOffset = switch (alignment)
                {
                    case ALIGN_CENTER -> (width - currentLineLeft) / 2;
                    case ALIGN_RIGHT -> width - currentLineLeft;
                    default -> 0;
                };

        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        int yBaseline = Integer.MAX_VALUE; // the biggest height difference from top to baseline.
        for (int i = firstInLine; i < paragraph.size(); i++)
        {
            VisualElement e = paragraph.get(i);
            if (e.positionMode == 0)
            {
                e.position = new Point(e.position.x() + leftOffset, e.position.y());

                yMin = Math.min(yMin, e.position.y());
                yMax = Math.max(yMax, e.position.y() + e.size.height());
                yBaseline = Math.min(yBaseline, e.position.y() + (int) (e.size.height() * e.baseline));
            }
        }

        final int yHeight = yMax - yMin;
        int yMin2 = Integer.MAX_VALUE;
        for (int i = firstInLine; i < paragraph.size(); i++)
        {
            VisualElement e = paragraph.get(i);
            if (e.positionMode == 0)
            {
                if (e.verticalAlign == VA_MIDDLE)
                {
                    e.position = new Point(e.position.x(), yMin + (yHeight - e.size.height()) / 2);
                }
                else if (e.verticalAlign == VA_BASELINE)
                {
                    e.position = new Point(e.position.x(), yBaseline - (int) (e.size.height() * e.baseline));
                }
                else if (e.verticalAlign == VA_BOTTOM)
                {
                    e.position = new Point(e.position.x(), yMax - e.size.height());
                }

                yMin2 = Math.min(yMin2, e.position.y());
            }
        }

        int yMax2 = yMax;
        if (yMin2 != currentLineTop && yMin2 != Integer.MAX_VALUE)
        {
            yMax2=Integer.MIN_VALUE;
            int yOffset = currentLineTop - yMin2;
            for (int i = firstInLine; i < paragraph.size(); i++)
            {
                VisualElement e = paragraph.get(i);
                if (e.positionMode == 0)
                {
                    e.position = new Point(e.position.x(), e.position.y() + yOffset);
                    yMax2 = Math.max(yMax2, e.position.y() + e.size.height());
                }
            }
        }

        return new Point(leftOffset, yMax2 - currentLineTop);
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        super.parse(context, attributes);

        String attr = attributes.getAttribute("align");
        if (attr != null)
        {
            String a = attr;
            switch (a)
            {
                case "left":
                    alignment = ElementParagraph.ALIGN_LEFT;
                    break;
                case "center":
                    alignment = ElementParagraph.ALIGN_CENTER;
                    break;
                case "right":
                    alignment = ElementParagraph.ALIGN_RIGHT;
                    break;
            }
        }

        indent = attributes.getAttribute("indent", indent);
        space = attributes.getAttribute("space", space);
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return "<p ...>" + inlines.stream().map(Object::toString).collect(Collectors.joining()) + "</p>";
    }

    @Override
    public Element copy()
    {
        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (ElementInline element : inlines)
        {paragraph.inlines.add(element.copy());}
        return paragraph;
    }

    @Nullable
    @Override
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        if (inlines.size() == 0)
            return null;

        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (ElementInline element : inlines)
        {
            Element t = element.applyTemplate(context, sourceElements);
            if (t instanceof ElementInline)
                paragraph.inlines.add((ElementInline) t);
        }

        if (paragraph.inlines.size() == 0)
            return null;

        return paragraph;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    @Override
    public boolean supportsSpanLevel()
    {
        return false;
    }

    public static ElementParagraph of(String text)
    {
        return of(text, TextStyle.DEFAULT);
    }

    public static ElementParagraph of(String text, TextStyle style)
    {
        ElementParagraph p = new ElementParagraph();
        ElementSpan s = ElementSpan.of(text, style);
        p.inlines.add(s);
        return p;
    }
}

