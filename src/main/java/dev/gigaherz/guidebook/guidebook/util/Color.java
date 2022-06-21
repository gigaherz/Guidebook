package dev.gigaherz.guidebook.guidebook.util;

import net.minecraft.world.item.DyeColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Color(int argb)
{
    // For an exploded description of this regex, see: https://regex101.com/r/4wQobv/2/
    private static final Pattern COLOR_PARSE = Pattern.compile("^(?:#?(?:(?<c3>[0-9a-f]{3})|(?<c4>[0-9a-f]{4})|(?<c6>[0-9a-f]{6})|(?<c8>[0-9a-f]{8}))|(?<rgb>rgb\\((?<r>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}),(?<g>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}),(?<b>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})\\))|(?<rgba>rgba\\((?<r2>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}),(?<g2>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}),(?<b2>25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}),(?<a2>[0-9]{1,3})\\))|(?<hsl>hsl\\((?<h>360|3[0-5][0-9]|[1-2]?[0-9]{1,2}),(?<s>[1-9][0-9]?|100)%?,(?<l>[1-9][0-9]?|100)%?\\))|(?<hsla>hsla\\((?<h2>360|3[0-5][0-9]|[1-2]?[0-9]{1,2}),(?<s2>[1-9][0-9]?|100)%?,(?<l2>[1-9][0-9]?|100)%?,(?<a3>0|1|1\\.0|0\\.[0-9]+)\\)))$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Color> DEFAULT_COLORS = new HashMap<>();

    static
    {
        for (DyeColor value : DyeColor.values())
        {
            DEFAULT_COLORS.put(value.getName(), fromRGB(value.getTextColor()));
        }
    }

    public int rgb()
    {
        return argb() & 0xFFFFFF;
    }

    public int alpha()
    {
        return (argb() >> 24) & 0xFF;
    }

    public int red()
    {
        return (argb() >> 16) & 0xFF;
    }

    public int green()
    {
        return (argb() >> 8) & 0xFF;
    }

    public int blue()
    {
        return argb() & 0xFF;
    }

    @Override
    public String toString()
    {
        return '#' + Integer.toHexString(argb()).toUpperCase();
    }

    public static Color fromARGB(int argb)
    {
        return new Color(argb);
    }

    public static Color fromRGB(int rgb)
    {
        return fromARGB(0xFF000000 | rgb);
    }

    public static Color fromRGBA(int r, int g, int b, int a)
    {
        if (r > 255) throw new IllegalArgumentException("Red value too big. Expected range: 0 to 255");
        if (r < 0) throw new IllegalArgumentException("Red value too small. Expected range: 0 to 255");
        if (g > 255) throw new IllegalArgumentException("Green value too big. Expected range: 0 to 255");
        if (g < 0) throw new IllegalArgumentException("Green value too small. Expected range: 0 to 255");
        if (b > 255) throw new IllegalArgumentException("Blue value too big. Expected range: 0 to 255");
        if (b < 0) throw new IllegalArgumentException("Blue value too small. Expected range: 0 to 255");
        if (a > 255) throw new IllegalArgumentException("Alpha value too big. Expected range: 0 to 255");
        if (a < 0) throw new IllegalArgumentException("Alpha value too small. Expected range: 0 to 255");
        return fromARGB(a << 24 | (r << 16) | (g << 8) | b);
    }

    public static Color fromRGB(int r, int g, int b)
    {
        return fromRGBA(r, g, b, 255);
    }

    public static Color fromRGBA(int r, int g, int b, float a)
    {
        return fromRGBA(r, g, b, (int) (a * 255.0f));
    }

    public static Color fromRGB(float r, float g, float b)
    {
        return fromRGB((int) (r * 255.0f), (int) (g * 255.0f), (int) (b * 255.0f));
    }

    public static Color fromRGBA(float r, float g, float b, float a)
    {
        return fromRGBA((int) (r * 255.0f), (int) (g * 255.0f), (int) (b * 255.0f), (int) (a * 255.0f));
    }

    public static Color fromHSLA(int h, int s, int l, float a)
    {
        if (h > 360) throw new IllegalArgumentException("Hue value too big. Expected range: 0 to 360");
        if (h < 0) throw new IllegalArgumentException("Hue value too small. Expected range: 0 to 360");
        if (s > 100) throw new IllegalArgumentException("Saturation value too big. Expected range: 0 to 100");
        if (s < 0) throw new IllegalArgumentException("Saturation value too small. Expected range: 0 to 100");
        if (l > 100) throw new IllegalArgumentException("Lightness value too big. Expected range: 0 to 100");
        if (l < 0) throw new IllegalArgumentException("Lightness value too small. Expected range: 0 to 100");
        if (a > 1.0f) throw new IllegalArgumentException("Alpha value too big. Expected range: 0 to 1");
        if (a < 0.0f) throw new IllegalArgumentException("Alpha value too small. Expected range: 0 to 1");
        return fromARGB(hslToRgb(h, s, l, a));
    }

    public static Color fromHSL(int h, int s, int l)
    {
        return fromHSLA(h, s, l, 1.0f);
    }

    public static Color parse(String c) throws ColorParseException
    {
        Matcher m = COLOR_PARSE.matcher(c);

        if (!m.matches())
        {
            if (DEFAULT_COLORS.containsKey(c))
            {
                return DEFAULT_COLORS.get(c);
            }
            throw new ColorParseException(c);
        }

        try
        {
            String value;
            if ((value = m.group("c3")) != null)
            {
                String s = new String(new char[]{value.charAt(0), value.charAt(0), value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2)});
                return fromRGB(Integer.parseInt(s, 16));
            }
            else if ((value = m.group("c4")) != null)
            {
                String s = new String(new char[]{value.charAt(0), value.charAt(0), value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2), value.charAt(3), value.charAt(3)});
                return fromARGB(Integer.parseUnsignedInt(s, 16));
            }
            else if ((value = m.group("c6")) != null)
            {
                return fromRGB(Integer.parseInt(value, 16));
            }
            else if ((value = m.group("c8")) != null)
            {
                return fromARGB(Integer.parseUnsignedInt(value, 16));
            }
            else if (m.group("rgb") != null)
            {
                int r = Integer.parseUnsignedInt(m.group("r"));
                int g = Integer.parseUnsignedInt(m.group("g"));
                int b = Integer.parseUnsignedInt(m.group("b"));
                return fromRGB(r, g, b);
            }
            else if (m.group("rgba") != null)
            {
                int r = Integer.parseUnsignedInt(m.group("r2"));
                int g = Integer.parseUnsignedInt(m.group("g2"));
                int b = Integer.parseUnsignedInt(m.group("b2"));
                int a = Integer.parseUnsignedInt(m.group("a2"));
                return fromRGBA(r, g, b, a);
            }
            else if (m.group("hsl") != null)
            {
                int h = Integer.parseUnsignedInt(m.group("h"));
                int s = Integer.parseUnsignedInt(m.group("s"));
                int l = Integer.parseUnsignedInt(m.group("l"));
                return fromHSL(h, s, l);
            }
            else if (m.group("hsla") != null)
            {
                int h = Integer.parseUnsignedInt(m.group("h2"));
                int s = Integer.parseUnsignedInt(m.group("s2"));
                int l = Integer.parseUnsignedInt(m.group("l2"));
                float a = Float.parseFloat(m.group("a3"));
                return fromHSLA(h, s, l, a);
            }
            else
            {
                throw new ColorParseException(c);
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new ColorParseException(c, e);
        }
    }

    static int hslToRgb(int h, int s, int l, float a)
    {
        float hf = h / 360.0f;
        float sf = s / 100.0f;
        float lf = l / 100.0f;

        float q;

        if (lf < 0.5)
            q = lf * (1 + sf);
        else
            q = (lf + sf) - (sf * l);

        float p = 2 * lf - q;

        float r = hueToRgb(p, q, hf + (1.0f / 3.0f));
        float g = hueToRgb(p, q, hf);
        float b = hueToRgb(p, q, hf - (1.0f / 3.0f));

        return ((int) (a * 255.0f) << 24) | ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | ((int) (b * 255.0f));
    }

    static float hueToRgb(float p, float q, float h)
    {
        if (h < 0)
            h += 1;

        if (h > 1)
            h -= 1;

        if (6 * h < 1)
        {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1)
        {
            return q;
        }

        if (3 * h < 2)
        {
            return p + ((q - p) * 6 * ((2.0f / 3.0f) - h));
        }

        return p;
    }

    public static class ColorParseException extends Exception
    {
        private final String colorString;

        public ColorParseException(String colorString)
        {
            super();
            this.colorString = colorString;
        }

        public ColorParseException(String colorString, Throwable cause)
        {
            super(cause);
            this.colorString = colorString;
        }

        @Override
        public String getMessage()
        {
            return "Invalid color string: " + getColorString();
        }

        public String getColorString()
        {
            return colorString;
        }
    }
}
