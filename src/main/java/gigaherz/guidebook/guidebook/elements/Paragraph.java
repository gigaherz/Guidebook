package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.util.text.TextFormatting;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Created by gigaherz on 13/11/2016.
 */
public class Paragraph implements IPageElement
{
    public final String text;
    public int alignment = 0;
    public int color = 0xFF000000;
    public int indent = 0;
    public int space = 2;
    public boolean bold;
    public boolean italics;
    public boolean underline;

    public Paragraph(String text)
    {
        this.text = text;
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        String textWithFormat = text;
        if (bold) textWithFormat = TextFormatting.BOLD + textWithFormat;
        if (italics) textWithFormat = TextFormatting.ITALIC + textWithFormat;
        if (underline) textWithFormat = TextFormatting.UNDERLINE + textWithFormat;
        return nav.addStringWrapping(left + indent, top, textWithFormat, color, alignment) + space;
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

        attr = attributes.getNamedItem("bold");
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
    public IPageElement copy()
    {
        Paragraph paragraph = new Paragraph(text);
        paragraph.alignment = alignment;
        paragraph.color = color;
        paragraph.indent = indent;
        paragraph.space = space;
        paragraph.bold = bold;
        paragraph.italics = italics;
        paragraph.underline = underline;
        return paragraph;
    }
}
