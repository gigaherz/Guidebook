package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualText;
import dev.gigaherz.guidebook.guidebook.util.Color;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;

import java.util.List;

public class ElementText extends ElementInline
{
    public final String text;
    public Color color;
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
        color = style.color();
        bold = style.bold();
        italics = style.italics();
        underline = style.underline();
        strikethrough = style.strikethrough();
        obfuscated = style.obfuscated();
        font = style.font();
        scale = style.scale();
    }

    private FormattedText getStringWithFormat(FormattedText text)
    {
        MutableComponent mutable;
        if (text instanceof Component)
        {
            mutable = ((Component) text).copy();
        }
        else
        {
            mutable = Component.literal(text.getString());
        }
        return mutable.withStyle(style -> style
                .withBold(bold)
                .withItalic(italics)
                .withUnderlined(underline)
                .withStrikethrough(strikethrough)
                .withObfuscated(obfuscated)
                .withFont(font));
    }

    protected FormattedText getActualString()
    {
        return Component.literal(text);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        List<VisualElement> elements = nav.measure(getStringWithFormat(getActualString()), width, firstLineWidth, scale, position, baseline, verticalAlignment);
        for (VisualElement text : elements)
        {
            if (text instanceof VisualText visualText)
            {
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
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        super.parse(context, attributes);
        scale = IParseable.getAttribute(attributes, "scale", scale);
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
