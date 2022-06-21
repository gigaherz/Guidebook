package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.BookDocumentParser;
import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Point2I;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElementParagraph extends Element
{
    public Alignment alignment = Alignment.LEFT;
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
        Point2I adjustedPosition = applyPosition(bounds.position, bounds.position);
        int currentLineTop = adjustedPosition.y();
        int currentLineLeft = indentFirstLine;
        int currentLineHeight = 0;
        int currentIndent = indentFirstLine;

        int firstInLine = paragraph.size();

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
                    processAlignment(paragraph, bounds.size.width() - currentIndent, currentLineLeft, firstInLine);

                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;
                    currentIndent = indent;

                    firstInLine = paragraph.size();
                }

                if (isLineBreak)
                    continue;

                if (size.height() > currentLineHeight)
                    currentLineHeight = size.height();

                current.position = element.applyPosition(new Point2I(adjustedPosition.x() + currentLineLeft + indent, currentLineTop), bounds.position);

                if (size.width() > 0)
                    currentLineLeft += size.width();

                if (currentLineLeft > bounds.size.width())
                {
                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;

                    firstInLine = paragraph.size();
                }

                paragraph.add(current);
            }
        }

        processAlignment(paragraph, bounds.size.width() - currentIndent, currentLineLeft, firstInLine);

        if (position != POS_RELATIVE)
            return bounds.position.y();
        return currentLineTop + currentLineHeight + space;
    }

    private void processAlignment(List<VisualElement> paragraph, int width, int currentLineLeft, int firstInLine)
    {
        if (paragraph.size() <= firstInLine)
            return;

        int leftOffset = switch (alignment)
        {
            case CENTER -> (width - currentLineLeft) / 2;
            case RIGHT -> width - currentLineLeft;
            default -> 0;
        };

        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        int yBaseline = Integer.MIN_VALUE; // the biggest height difference from top to baseline.
        for (int i = firstInLine; i < paragraph.size(); i++)
        {
            VisualElement e = paragraph.get(i);
            if (e.positionMode == 0)
            {
                e.position = new Point2I(e.position.x() + leftOffset, e.position.y());

                yMin = Math.min(yMin, e.position.y());
                yMax = Math.min(yMax, e.position.y() + e.size.height()); // TODO check if this is correct
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
                    e.position = new Point2I(e.position.x(), yMin + (yHeight - e.size.height()) / 2);
                }
                else if (e.verticalAlign == VA_BASELINE)
                {
                    e.position = new Point2I(e.position.x(), yBaseline - (int) (e.size.height() * e.baseline));
                }
                else if (e.verticalAlign == VA_BOTTOM)
                {
                    e.position = new Point2I(e.position.x(), yMax - e.size.height());
                }

                yMin2 = Math.min(yMin2, e.position.y());
            }
        }

        if (yMin2 != yMin && yMin2 != Integer.MAX_VALUE)
        {
            int yOffset = yMin - yMin2;
            for (int i = firstInLine; i < paragraph.size(); i++)
            {
                VisualElement e = paragraph.get(i);
                if (e.positionMode == 0)
                {
                    e.position = new Point2I(e.position.x(), e.position.y() + yOffset);
                }
            }
        }
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        super.parse(context, attributes);

        alignment = IParseable.getAttribute(attributes, "align", alignment, Alignment.class);
        indent = IParseable.getAttribute(attributes, "indent", indent);
        space = IParseable.getAttribute(attributes, "space", space);
    }

    @Override
    public TextStyle childStyle(ParsingContext context, NamedNodeMap attributes, TextStyle defaultStyle)
    {
        return TextStyle.parse(attributes, defaultStyle);
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        for (int q = 0; q < childNodes.getLength(); q++)
        {
            Node childNode = childNodes.item(q);
            ElementInline parsedChild = BookDocumentParser.parseParagraphElement(context, childNode, defaultStyle);

            if (parsedChild == null)
            {
                GuidebookMod.logger.warn("Unrecognized tag: {}", childNode.getNodeName());
            }
            else
            {
                inlines.add(parsedChild);
            }
        }
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

    public enum Alignment
    {
        LEFT,
        CENTER,
        RIGHT
    }
}

