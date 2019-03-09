package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.util.Point;
import gigaherz.guidebook.guidebook.util.Rect;
import gigaherz.guidebook.guidebook.util.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
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
        int currentLineTop = adjustedPosition.y;
        int currentLineLeft = indentFirstLine;
        int currentLineHeight = 0;
        int currentIndent = indentFirstLine;

        int firstInLine = paragraph.size();

        for (Element element : inlines)
        {
            int firstLineWidth = bounds.size.width - currentLineLeft - indent - indentFirstLine;
            List<VisualElement> pieces = element.measure(nav,
                    bounds.size.width - indent,
                    firstLineWidth);

            if (pieces.size() < 1)
                continue;

            for (int i = 0; i < pieces.size(); i++)
            {
                VisualElement current = pieces.get(i);
                Size size = current.size;

                boolean isLineBreak = "\n".equals(current.getText());

                if (isLineBreak || (currentLineLeft + size.width > bounds.size.width && currentLineLeft > 0))
                {
                    processAlignment(paragraph, bounds.size.width - currentIndent, currentLineLeft, firstInLine);

                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;
                    currentIndent = indent;

                    firstInLine = paragraph.size();
                }

                if (isLineBreak)
                    continue;

                if (size.height > currentLineHeight)
                    currentLineHeight = size.height;

                current.position = element.applyPosition(new Point(adjustedPosition.x + currentLineLeft + indent, currentLineTop), bounds.position);

                if (size.width > 0)
                    currentLineLeft += size.width;

                if (currentLineLeft > bounds.size.width)
                {
                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;

                    firstInLine = paragraph.size();
                }

                paragraph.add(current);
            }
        }

        processAlignment(paragraph, bounds.size.width-currentIndent, currentLineLeft, firstInLine);

        if (position != POS_RELATIVE)
            return bounds.position.y;
        return currentLineTop + currentLineHeight + space;
    }

    private void processAlignment(List<VisualElement> paragraph, int width, int currentLineLeft, int firstInLine)
    {
        if (paragraph.size() <= firstInLine)
            return;

        int realWidth = currentLineLeft;
        int leftOffset = 0;
        switch (alignment)
        {
            case ALIGN_CENTER:
                leftOffset = (width - realWidth) / 2;
                break;
            case ALIGN_RIGHT:
                leftOffset = width - realWidth;
                break;
        }

        int yMin = Integer.MAX_VALUE;
        int yMax = Integer.MIN_VALUE;
        int yBaseline = Integer.MIN_VALUE; // the biggest height difference from top to baseline.
        for (int i = firstInLine; i < paragraph.size(); i++)
        {
            VisualElement e = paragraph.get(i);
            if (e.positionMode == 0)
            {
                e.position = new Point(e.position.x + leftOffset, e.position.y);

                yMin = Math.min(yMin, e.position.y);
                yMax = Math.min(yMax, e.position.y + e.size.height);
                yBaseline = Math.min(yBaseline, e.position.y + (int) (e.size.height * e.baseline));
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
                    e.position = new Point(e.position.x, yMin + (yHeight - e.size.height) / 2);
                }
                else if (e.verticalAlign == VA_BASELINE)
                {
                    e.position = new Point(e.position.x, yBaseline - (int) (e.size.height * e.baseline));
                }
                else if (e.verticalAlign == VA_BOTTOM)
                {
                    e.position = new Point(e.position.x, yMax - e.size.height);
                }

                yMin2 = Math.min(yMin2, e.position.y);
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
                    e.position = new Point(e.position.x, e.position.y + yOffset);
                }
            }
        }
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        super.parse(book, attributes);

        Node attr = attributes.getNamedItem("align");
        if (attr != null)
        {
            String a = attr.getTextContent();
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

        indent = getAttribute(attributes, "indent", indent);
        space = getAttribute(attributes, "space", space);
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return "<p ...>" + inlines.stream().map(Object::toString).collect(Collectors.joining())  + "</p>";
    }

    @Override
    public Element copy()
    {
        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (ElementInline element : inlines)
        { paragraph.inlines.add(element.copy()); }
        return paragraph;
    }

    @Nullable
    @Override
    public Element applyTemplate(IConditionSource book, List<Element> sourceElements)
    {
        if (inlines.size() == 0)
            return null;

        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (ElementInline element : inlines)
        {
            Element t = element.applyTemplate(book, sourceElements);
            if (t instanceof ElementInline)
                paragraph.inlines.add((ElementInline)t);
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

