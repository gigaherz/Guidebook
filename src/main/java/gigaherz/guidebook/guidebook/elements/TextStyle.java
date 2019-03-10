package gigaherz.guidebook.guidebook.elements;

import org.w3c.dom.NamedNodeMap;

public class TextStyle
{
    public static final TextStyle DEFAULT = new TextStyle(0xFF000000, false, false, false, 1.0f);
    public static final TextStyle LINK = new TextStyle(0xFF7766cc, false, false, true, 1.0f);
    public static final TextStyle ERROR = new TextStyle(0xFFcc7766, false, false, true, 1.0f);

    public int color;
    public boolean bold;
    public boolean italics;
    public boolean underline;
    public float scale;

    public TextStyle(int color, boolean bold, boolean italics, boolean underline, float scale)
    {
        this.color = color;
        this.bold = bold;
        this.italics = italics;
        this.underline = underline;
        this.scale = scale;
    }

    public static TextStyle parse(NamedNodeMap attributes, TextStyle defaults)
    {
        boolean bold1 = Element.getAttribute(attributes, "bold", defaults != null ? defaults.bold : DEFAULT.bold);

        boolean italics1 = Element.getAttribute(attributes, "italics", defaults != null ? defaults.italics : DEFAULT.italics);

        boolean underline1 = Element.getAttribute(attributes, "underline", defaults != null ? defaults.underline : DEFAULT.underline);

        int color1 = Element.getColorAttribute(attributes, "color", defaults != null ? defaults.color : DEFAULT.color);

        float scale = Element.getAttribute(attributes, "scale", DEFAULT.scale);

        return new TextStyle(color1, bold1, italics1, underline1, scale);
    }
}
