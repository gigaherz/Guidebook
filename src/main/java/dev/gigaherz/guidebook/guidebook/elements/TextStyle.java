package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.util.Color;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;

public record TextStyle(Color color, boolean bold, boolean italics, boolean underline, boolean strikethrough,
                        boolean obfuscated, ResourceLocation font, float scale) implements Cloneable
{
    public static final TextStyle DEFAULT = new TextStyle(Color.fromARGB(0xFF000000), false, false, false, false, false, Style.DEFAULT_FONT, 1.0f);
    public static final TextStyle LINK = new TextStyle(Color.fromARGB(0xFF7766cc), false, false, true, false, false, Style.DEFAULT_FONT, 1.0f);
    public static final TextStyle ERROR = new TextStyle(Color.fromARGB(0xFFcc7766), false, false, true, false, false, Style.DEFAULT_FONT, 1.0f);

    public static TextStyle parse(NamedNodeMap attributes, TextStyle defaults)
    {
        if (defaults == null)
        {
            defaults = DEFAULT;
        }
        Color color1 = IParseable.getAttribute(attributes, "color", defaults.color);
        boolean bold1 = IParseable.getAttribute(attributes, "bold", defaults.bold);
        boolean italics1 = IParseable.getAttribute(attributes, "italics", defaults.italics);
        boolean underline1 = IParseable.getAttribute(attributes, "underline", defaults.underline);
        boolean strikethrough1 = IParseable.getAttribute(attributes, "strikethrough", defaults.strikethrough);
        boolean obfuscated1 = IParseable.getAttribute(attributes, "obfuscated", defaults.obfuscated);
        ResourceLocation font1 = IParseable.getAttribute(attributes, "font", defaults.font);
        float scale1 = IParseable.getAttribute(attributes, "scale", defaults.scale);

        return new TextStyle(color1, bold1, italics1, underline1, strikethrough1, obfuscated1, font1, scale1);
    }

    @Override
    public TextStyle clone() throws CloneNotSupportedException
    {
        return (TextStyle) super.clone();
    }
}
