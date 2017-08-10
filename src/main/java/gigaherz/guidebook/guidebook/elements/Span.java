package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Size;
import net.minecraft.util.text.TextFormatting;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public class Span implements IParagraphElement
{
    public final String text;
    public int color = 0xFF000000;
    public boolean bold;
    public boolean italics;
    public boolean underline;

    public Span(String text)
    {
        this.text = text;
    }

    @Override
    public List<Size> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return nav.measure(getStringWithFormat(), width, firstLineWidth);
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top, int width)
    {
        return nav.addStringWrapping(left, top, getStringWithFormat(), color, 0);
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
    public void parse(NamedNodeMap attributes)
    {
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
    public IParagraphElement copy()
    {
        Span span = new Span(text);
        span.color = color;
        span.bold = bold;
        span.italics = italics;
        span.underline = underline;
        return span;
    }
}
