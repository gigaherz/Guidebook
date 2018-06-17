package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import gigaherz.guidebook.guidebook.drawing.Point;
import gigaherz.guidebook.guidebook.drawing.Rect;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.List;

public class ElementParagraph extends Element
{
    public int alignment = 0;
    public int indent = 0; // First line?
    public int indentFirstLine = 0; // First line?
    public int space = 2;

    public final List<Element> spans = Lists.newArrayList();

    @Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for (Element element : spans)
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

        int firstInLine = paragraph.size();

        for (Element element : spans)
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

                if ((currentLineLeft + size.width > bounds.size.width && currentLineLeft > 0))
                {
                    if (paragraph.size() > firstInLine && alignment != 0)
                        processAlignment(paragraph, bounds.size.width - indent, currentLineLeft, firstInLine);

                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;

                    firstInLine = paragraph.size();
                }

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

        if (paragraph.size() > firstInLine && alignment != 0)
            processAlignment(paragraph, bounds.size.width, currentLineLeft, firstInLine);

        if (position != 0)
            return bounds.position.y;
        return currentLineTop + currentLineHeight + space;
    }

    private void processAlignment(List<VisualElement> paragraph, int width, int currentLineLeft, int firstInLine)
    {
        int realWidth = currentLineLeft;

        int leftOffset = width - realWidth;
        if (alignment == 1) // center
        {
            leftOffset /= 2;
        }
        for (int i = firstInLine; i < paragraph.size(); i++)
        {
            VisualElement e = paragraph.get(i);
            e.position = new Point(e.position.x + leftOffset, e.position.y);
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
                    alignment = 0;
                    break;
                case "center":
                    alignment = 1;
                    break;
                case "right":
                    alignment = 2;
                    break;
            }
        }

        attr = attributes.getNamedItem("indent");
        if (attr != null)
        {
            indent = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("space");
        if (attr != null)
        {
            space = Ints.tryParse(attr.getTextContent());
        }
    }

    @Override
    public Element copy()
    {
        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (Element element : spans)
        { paragraph.spans.add(element.copy()); }
        return paragraph;
    }

    @Nullable
    @Override
    public Element applyTemplate(IConditionSource book, List<Element> sourceElements)
    {
        if (spans.size() == 0)
            return null;

        ElementParagraph paragraph = super.copy(new ElementParagraph());
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for (Element element : spans)
        {
            Element t = element.applyTemplate(book, sourceElements);
            if (t != null)
                paragraph.spans.add(t);
        }

        if (paragraph.spans.size() == 0)
            return null;

        return paragraph;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    public static ElementParagraph of(String text)
    {
        ElementParagraph p = new ElementParagraph();
        ElementSpan s = new ElementSpan(text, true, true);
        p.spans.add(s);
        return p;
    }
}

