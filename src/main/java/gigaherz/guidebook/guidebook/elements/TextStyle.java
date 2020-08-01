package gigaherz.guidebook.guidebook.elements;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import org.w3c.dom.NamedNodeMap;

public class TextStyle
{
    public static final TextStyle DEFAULT = new TextStyle(0xFF000000, false, false, false, false, false, Style.DEFAULT_FONT, 1.0f);
    public static final TextStyle LINK = new TextStyle(0xFF7766cc, false, false, true, false, false, Style.DEFAULT_FONT, 1.0f);
    public static final TextStyle ERROR = new TextStyle(0xFFcc7766, false, false, true, false, false, Style.DEFAULT_FONT, 1.0f);

    public final int color;
    public final boolean bold;
    public final boolean italics;
    public final boolean underline;
    public final boolean strikethrough;
    public final boolean obfuscated;
    public final ResourceLocation font;
    public final float scale;

    public TextStyle(int color, boolean bold, boolean italics, boolean underline, boolean strikethrough, boolean obfuscated, ResourceLocation font, float scale)
    {
        this.color = color;
        this.bold = bold;
        this.italics = italics;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.font = font;
        this.scale = scale;
    }

    public static TextStyle parse(NamedNodeMap attributes, TextStyle defaults)
    {
        int color1 = Element.getColorAttribute(attributes, defaults != null ? defaults.color : DEFAULT.color);
        boolean bold1 = Element.getAttribute(attributes, "bold", defaults != null ? defaults.bold : DEFAULT.bold);
        boolean italics1 = Element.getAttribute(attributes, "italics", defaults != null ? defaults.italics : DEFAULT.italics);
        boolean underline1 = Element.getAttribute(attributes, "underline", defaults != null ? defaults.underline : DEFAULT.underline);
        boolean strikethrough1 = Element.getAttribute(attributes, "strikethrough", defaults != null ? defaults.strikethrough : DEFAULT.strikethrough);
        boolean obfuscated1 = Element.getAttribute(attributes, "obfuscated", defaults != null ? defaults.obfuscated : DEFAULT.obfuscated);
        ResourceLocation font1 = Element.getAttribute(attributes, "font", defaults != null ? defaults.font : DEFAULT.font);
        float scale1 = Element.getAttribute(attributes, "scale", defaults != null ? defaults.scale : DEFAULT.scale);

        return new TextStyle(color1, bold1, italics1, underline1, strikethrough1, obfuscated1, font1, scale1);
    }
}
