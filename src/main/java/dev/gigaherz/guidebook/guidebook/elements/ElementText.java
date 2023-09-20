package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualText;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        return Component.literal(text.getString()).withStyle(style -> style
                .withBold(bold)
                .withItalic(italics)
                .withUnderlined(underline)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated)
                .withColor(color)
                .withFont(font));
    }

    protected FormattedText getActualString()
    {
        return Component.literal(text);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return nav.measure(getStringWithFormat(getActualString()), width, firstLineWidth, scale, position, baseline, verticalAlignment);
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
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        super.parse(context, attributes);
        scale = attributes.getAttribute("scale", scale);
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
        String temp = text.replaceAll("[ \t\n\r]+", " ");
        if (trimLeft)
            temp = temp.replaceAll("^[ \t\n\r]+", "");
        if (trimRight)
            temp = temp.replaceAll("[ \t\n\r]+$", "");
        return temp;
    }
}
