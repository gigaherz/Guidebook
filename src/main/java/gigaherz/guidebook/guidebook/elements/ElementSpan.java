package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.drawing.Rect;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualText;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

public class ElementSpan extends Element
{
    public String text;
    public int color = 0xFF000000;
    public boolean bold;
    public boolean italics;
    public boolean underline;

    public String translationKey = null;

    public ElementSpan(String text, boolean isFirstElement, boolean isLastElement)
    {
        this.text = compactString(text, isFirstElement, isLastElement);
    }

    private String getStringWithFormat()
    {
        String textWithFormat;
        if (translationKey != null)
        {
            textWithFormat = I18n.format(translationKey);
        }
        else
        {
            textWithFormat = text;
        }
        if (bold) textWithFormat = TextFormatting.BOLD + textWithFormat;
        if (italics) textWithFormat = TextFormatting.ITALIC + textWithFormat;
        if (underline) textWithFormat = TextFormatting.UNDERLINE + textWithFormat;
        return textWithFormat;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> elements = nav.measure(getStringWithFormat(), width, firstLineWidth, scale, position, baseline, verticalAlignment);
        for (VisualElement text : elements)
        {
            if (text instanceof VisualText)
                ((VisualText) text).color = color;
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
            temporaryParagraph.inlines.add(this);
        }
        return temporaryParagraph.reflow(paragraph, nav, bounds, page);
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        super.parse(book, attributes);

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

        attr = attributes.getNamedItem("i18n");
        if (attr != null)
        {
            translationKey = attr.getTextContent();
        }
    }

    @Override
    public Element copy()
    {
        ElementSpan span = super.copy(new ElementSpan(text, false, false));
        span.color = color;
        span.bold = bold;
        span.italics = italics;
        span.underline = underline;
        return span;
    }


    public static String compactString(String text, boolean trimLeft, boolean trimRight)
    {
        String temp = text.replaceAll("[\n\r]+", "").replaceAll("[ \t]+", " ");
        if (trimLeft) temp = temp.replaceAll("^[ \t]+", "");
        if (trimRight) temp = temp.replaceAll("[ \t]+$", "");
        return temp;
    }
}
