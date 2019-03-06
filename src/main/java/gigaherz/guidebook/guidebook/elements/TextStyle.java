package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IConditionSource;
import org.w3c.dom.NamedNodeMap;

public class TextStyle
{
    public static final TextStyle DEFAULT = new TextStyle(0xFF000000, false, false, false);
    public static final TextStyle LINK = new TextStyle(0xFF7766cc, false, false, true);
    public static final TextStyle ERROR = new TextStyle(0xFFcc7766, false, false, true);

    public int color;
    public boolean bold;
    public boolean italics;
    public boolean underline;

    public TextStyle(int color, boolean bold, boolean italics, boolean underline)
    {
        this.color = color;
        this.bold = bold;
        this.italics = italics;
        this.underline = underline;
    }

    public static TextStyle parse(NamedNodeMap attributes, TextStyle defaults)
    {
        boolean bold1 = Element.getAttribute(attributes, "bold", defaults != null ? defaults.bold : DEFAULT.bold);

        boolean italics1 = Element.getAttribute(attributes, "italics", defaults != null ? defaults.italics : DEFAULT.italics);

        boolean underline1 = Element.getAttribute(attributes, "underline", defaults != null ? defaults.underline : DEFAULT.underline);

        int color1 = Element.getColorAttribute(attributes, "color", defaults != null ? defaults.color : DEFAULT.color);

        return new TextStyle(color1, bold1, italics1, underline1);
    }
}
