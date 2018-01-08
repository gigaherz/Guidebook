package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Rect;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualText;
import net.minecraft.util.text.TextFormatting;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public class ElementSpan extends Element
{
    public final String text;
    public int color = 0xFF000000;
    public boolean bold;
    public boolean italics;
    public boolean underline;

    public ElementSpan(String text)
    {
        this.text = compactString(text);
    }

    private String getStringWithFormat()
    {
        String textWithFormat = text;
        if (bold) textWithFormat = TextFormatting.BOLD + textWithFormat;
        if (italics) textWithFormat = TextFormatting.ITALIC + textWithFormat;
        if (underline) textWithFormat = TextFormatting.UNDERLINE + textWithFormat;
        return textWithFormat;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> elements = nav.measure(getStringWithFormat(), width, firstLineWidth);
        for(VisualElement text : elements)
        {
            if (!(text instanceof VisualText))
                continue; // WTF?
            ((VisualText)text).color = color;
        }
        return elements;
    }

    private ElementParagraph temporaryParagraph = null;

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        if (temporaryParagraph == null)
        {
            temporaryParagraph = new ElementParagraph();
            temporaryParagraph.spans.add(this);
        }
        return temporaryParagraph.reflow(paragraph, nav, bounds, page);
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("bold");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                bold = true;
        }

        attr = attributes.getNamedItem("italics");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                italics = true;
        }

        attr = attributes.getNamedItem("underline");
        if (attr != null)
        {
            String text = attr.getTextContent();
            if ("".equals(text) || "true".equals(text))
                underline = true;
        }

        attr = attributes.getNamedItem("color");
        if (attr != null)
        {
            String c = attr.getTextContent();

            if (c.startsWith("#"))
                c = c.substring(1);

            try
            {
                if (c.length() <= 6)
                {
                    color = 0xFF000000 | Integer.parseInt(c, 16);
                }
                else
                {
                    color = Integer.parseInt(c, 16);
                }
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
    }

    @Override
    public Element copy()
    {
        ElementSpan span = super.copy(new ElementSpan(text));
        span.color = color;
        span.bold = bold;
        span.italics = italics;
        span.underline = underline;
        return span;
    }


    public static String compactString(String text)
    {
        String temp = text.replaceAll("[\n\r]+", "").replaceAll("[ \t]+", " ");
        return " ".equals(temp) ? " " : temp.trim();
    }
}
