package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Point;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import joptsimple.internal.Strings;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;

public class ElementParagraph extends ElementContainer
{
    public int alignment = 0;
    public int indent = 0; // First line?
    public int space = 2;

    public final List<Element> spans = Lists.newArrayList();

    public ElementParagraph(int defaultPositionMode)
    {
        super(defaultPositionMode);
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, final int left, final int top, int width, int height)
    {
        int currentLineTop = top;
        int currentLineLeft = 0;
        int currentLineHeight = 0;
        final Size spaceSize = nav.measure(" ");

        int firstInLine = paragraph.size();

        for(Element element : spans)
        {
            List<VisualElement> pieces = element.measure(nav, width, width - currentLineLeft - indent);

            if (pieces.size() < 1)
                continue;

            for (int i = 0; i < pieces.size(); i++)
            {
                VisualElement current = pieces.get(i);
                Size size = current.size;

                if (currentLineLeft + size.width > width && currentLineLeft > 0)
                {
                    if (paragraph.size() > firstInLine && alignment != 0)
                        processAlignment(paragraph, width, currentLineLeft, spaceSize, firstInLine);

                    currentLineTop += currentLineHeight;
                    currentLineLeft = 0;
                    currentLineHeight = 0;

                    firstInLine = paragraph.size();
                }

                if (size.height > currentLineHeight)
                    currentLineHeight = size.height;

                current.position = element.applyPosition(new Point(left + currentLineLeft + (i == 0 ? indent : 0), currentLineTop), left, top);

                if (size.width > 0)
                    currentLineLeft += size.width + spaceSize.width;

                paragraph.add(current);
            }
        }

        if (paragraph.size() > firstInLine && alignment != 0)
            processAlignment(paragraph, width, currentLineLeft, spaceSize, firstInLine);

        return currentLineTop + currentLineHeight + space;
    }

    private void processAlignment(List<VisualElement> paragraph, int width, int currentLineLeft, Size spaceSize, int firstInLine)
    {
        int realWidth = currentLineLeft - spaceSize.width;

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
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

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
        ElementParagraph paragraph = super.copy(new ElementParagraph(position));
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for(Element element : spans)
            paragraph.spans.add(element.copy());
        return paragraph;
    }

    @Override
    public Element applyTemplate(List<Element> sourceElements)
    {
        ElementParagraph paragraph = super.copy(new ElementParagraph(position));
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for(Element element : spans)
        {
            Element t = element.applyTemplate(sourceElements);
            paragraph.spans.add(t);
        }
        return paragraph;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    @Override
    public Collection<Element> getChildren()
    {
        return spans;
    }

    public static ElementParagraph of(int defaultPositionMode, String text)
    {
        ElementParagraph p = new ElementParagraph(defaultPositionMode);
        ElementSpan s = new ElementSpan(0, text);
        p.spans.add(s);
        return p;
    }

}

