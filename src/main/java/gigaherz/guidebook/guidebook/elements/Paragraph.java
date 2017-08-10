package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.SizedSegment;
import joptsimple.internal.Strings;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;

public class Paragraph implements IContainerParagraphElement
{
    public int alignment = 0;
    public int indent = 0;
    public int space = 2;

    public final List<IParagraphElement> spans = Lists.newArrayList();

    @Override
    public int apply(IBookGraphics nav, int left, int top, int width)
    {
        Size spaceSize = nav.measure(" ");
        List<IParagraphElement> currentLine = Lists.newArrayList();
        int currentLineLeft = 0;
        int currentLineHeight = 0;
        for(IParagraphElement element : spans)
        {
            List<SizedSegment> sizes = element.measure(nav, width, width - currentLineLeft);

            if (sizes.size() < 1)
                continue;

            int sizesNum = sizes.size();
            for (int i = 0; i< sizesNum; i++)
            {
                SizedSegment first = sizes.get(0);
                Size firstSize = first.size;

                if (firstSize.height > currentLineHeight)
                    currentLineHeight = firstSize.height;

                if ((sizesNum > 1 && i < (sizesNum-1)) || currentLineLeft + firstSize.width > width)
                {
                    // Flush
                    flushLine(nav, currentLine, left, top, width, spaceSize.width);

                    currentLine.clear();
                    currentLine.add(element);
                    top += currentLineHeight;

                    currentLineLeft = 0;
                    currentLineHeight = 0;
                }
                else
                {
                    currentLine.add(element);
                    currentLineLeft += firstSize.width + spaceSize.width;
                }
            }
        }

        // Flush
        if (currentLine.size() > 0)
        {
            flushLine(nav, currentLine, left, top, width, spaceSize.width);
            top += currentLineHeight;
        }

        return top + space;
    }

    private void flushLine(IBookGraphics nav, Iterable<IParagraphElement> currentLine, int startLeft, int top, int width, int spaceWidth)
    {
        int left = startLeft;
        for(IParagraphElement element : currentLine)
        {
            List<SizedSegment> sizes = element.measure(nav, width, width + startLeft - left);

            element.apply(nav, left, top, width + startLeft - left);
            left += size.width + spaceWidth;
        }
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
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
    public IPageElement copy()
    {
        Paragraph paragraph = new Paragraph();
        paragraph.alignment = alignment;
        paragraph.indent = indent;
        paragraph.space = space;
        for(IParagraphElement element : spans)
            paragraph.spans.add(element.copy());
        return paragraph;
    }

    @Override
    public Collection<IParagraphElement> getChildren()
    {
        return spans;
    }

    public Paragraph addTextSpan(Node elementItem)
    {
        String st = elementItem.getTextContent();
        if (!Strings.isNullOrEmpty(st))
        {
            Span s = new Span(st);
            s.underline = true;
            s.italics = true;

            if (elementItem.hasAttributes())
            {
                s.parse(elementItem.getAttributes());
            }

            spans.add(s);
        }

        return this;
    }

    public static Paragraph of(String text)
    {
        Paragraph p = new Paragraph();
        Span s = new Span(text);
        p.spans.add(s);
        return p;
    }
}

