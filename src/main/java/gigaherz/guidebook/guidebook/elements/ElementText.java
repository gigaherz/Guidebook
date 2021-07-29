package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualText;
import gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class ElementText extends ElementInline
{
    public final String text;
    public int color;
    public boolean bold;
    public boolean italics;
    public boolean underline;
    public boolean strikethrough;
    public boolean obfuscated;
    public ResourceLocation font;

    public float scale;

    public ElementText(String text, boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        super(isFirstElement, isLastElement);
        this.text = compactString(text, isFirstElement, isLastElement);
        color = style.color;
        bold = style.bold;
        italics = style.italics;
        underline = style.underline;
        strikethrough = style.strikethrough;
        obfuscated = style.obfuscated;
        font = style.font;
        scale = style.scale;
    }

    private FormattedText getStringWithFormat(FormattedText text)
    {
        return new TextComponent(text.getString()).withStyle(style -> style
                .withBold(bold)
                .withItalic(italics)
                .setUnderlined(underline)
                .setStrikethrough(strikethrough)
                .setObfuscated(obfuscated)
                .withFont(font));
    }

    protected FormattedText getActualString()
    {
        return new TextComponent(text);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> elements = nav.measure(getStringWithFormat(getActualString()), width, firstLineWidth, scale, position, baseline, verticalAlignment);
        for (VisualElement text : elements)
        {
            if (text instanceof VisualText)
            {
                VisualText visualText = (VisualText) text;
                visualText.color = color;
            }
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
        scale = getAttribute(attributes, "scale", scale);
    }

    @Override
    public String toString(boolean complete)
    {
        return text;
    }

    @Override
    public ElementInline copy()
    {
        ElementText span = super.copy(new ElementText(text, false, false, new TextStyle(color, bold, italics, underline, strikethrough, obfuscated, font, scale)));
        span.color = color;
        span.bold = bold;
        span.italics = italics;
        span.underline = underline;
        span.obfuscated = obfuscated;
        span.scale = scale;
        return span;
    }


    public static String compactString(String text, boolean trimLeft, boolean trimRight)
    {
        if (text == null)
            return null;
        String temp = text.replaceAll("[\n\r]+", "").replaceAll("[ \t]+", " ");
        if (trimLeft) temp = temp.replaceAll("^[ \t]+", "");
        if (trimRight) temp = temp.replaceAll("[ \t]+$", "");
        return temp;
    }
}
