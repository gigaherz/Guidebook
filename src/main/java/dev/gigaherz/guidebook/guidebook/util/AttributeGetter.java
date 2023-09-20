package dev.gigaherz.guidebook.guidebook.util;

import dev.gigaherz.guidebook.GuidebookMod;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface AttributeGetter
{
    boolean hasAttribute(String name);

    @Nullable
    String getAttribute(String name);

    void removeAttribute(String name);

    AttributeGetter copy();


    default String getAttribute(String name, String def)
    {
        if (hasAttribute(name))
        {
            String text = getAttribute(name);
            if (text != null)
                return text;
        }
        return def;
    }

    default ResourceLocation getAttribute(String name, ResourceLocation def)
    {
        if (hasAttribute(name))
        {
            String text = getAttribute(name);
            if (text != null)
                return new ResourceLocation(text);
        }
        return def;
    }

    default boolean getAttribute(String name, boolean def)
    {
        if (hasAttribute(name))
        {
            String text = getAttribute(name);
            if ("".equals(text) || "true".equals(text))
                return true;
            if ("false".equals(text))
                return false;
        }
        return def;
    }

    default int getAttribute(String name, int def)
    {
        if (hasAttribute(name))
        {
            String text = getAttribute(name);
            try
            {
                return Integer.parseInt(text);
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    default float getAttribute(String name, float def)
    {
        if (hasAttribute(name))
        {
            String text = getAttribute(name);
            try
            {
                return Float.parseFloat(text);
            }
            catch (NumberFormatException e)
            {
                // ignored
            }
        }
        return def;
    }

    // For an exploded description of this regex, see: https://regex101.com/r/qXfiDc/1/
    static final Pattern COLOR_PARSE = Pattern.compile("^#?(?:(?<c3>[0-9a-f]{3})|(?<c6>[0-9a-f]{6})|(?<c8>[0-9a-f]{8})|(?<rgb>rgb\\((?<r>[0-9]{1,3}),(?<g>[0-9]{1,3}),(?<b>[0-9]{1,3})\\))|(?<rgba>rgba\\((?<r2>[0-9]{1,3}),(?<g2>[0-9]{1,3}),(?<b2>[0-9]{1,3}),(?<a2>[0-9]{1,3})\\)))$");

    default int getColorAttribute(int def)
    {
        String attr = getAttribute("color");
        if (attr != null)
        {
            Matcher m = COLOR_PARSE.matcher(attr);
            if (m.matches())
            {
                try
                {
                    String value;
                    if ((value = m.group("c3")) != null)
                    {
                        String s = new String(new char[]{value.charAt(0), value.charAt(0), value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2)});
                        return 0xFF000000 | Integer.parseInt(s, 16);
                    }
                    else if ((value = m.group("c6")) != null)
                    {
                        return 0xFF000000 | Integer.parseInt(value, 16);
                    }
                    else if ((value = m.group("c8")) != null)
                    {
                        return Integer.parseUnsignedInt(value, 16);
                    }
                    else if (m.group("rgb") != null)
                    {
                        int r = Integer.parseUnsignedInt(m.group("r"));
                        int g = Integer.parseUnsignedInt(m.group("g"));
                        int b = Integer.parseUnsignedInt(m.group("b"));
                        if (r > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (g > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (b > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        return 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                    else if (m.group("rgba") != null)
                    {
                        int r = Integer.parseUnsignedInt(m.group("r2"));
                        int g = Integer.parseUnsignedInt(m.group("g2"));
                        int b = Integer.parseUnsignedInt(m.group("b2"));
                        int a = Integer.parseUnsignedInt(m.group("a2"));
                        if (r > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (g > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (b > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        if (a > 255) throw new NumberFormatException("Number too big. Expected range: 0 to 255");
                        return (a << 24) | (r << 16) | (g << 8) | b;
                    }

                    GuidebookMod.logger.warn("Unrecognized color value {}, ignored.", attr);
                }
                catch (NumberFormatException e)
                {
                    GuidebookMod.logger.warn("Color value not valid {}", attr, e);
                    // ignored
                }
            }
        }

        return def;
    }


    static AttributeGetter copyOf(NamedNodeMap attributes)
    {
        Map<String, String> innerMap = new HashMap<>();
        for(int i=0;i<attributes.getLength(); i++)
        {
            var attr = attributes.item(i);
            var name = attr.getNodeName();
            var value = attr.getTextContent();
            innerMap.put(name, value);
        }
        return new MapGetter(innerMap);
    }

    static AttributeGetter copyOf(AttributeGetter attributes)
    {
        return attributes.copy();
    }

    static AttributeGetter wrap(NamedNodeMap attributes)
    {
        return new WrapperGetter(attributes);
    }

    static AttributeGetter of(Node node)
    {
        return wrap(node.getAttributes());
    }

    class MapGetter implements AttributeGetter
    {
        private final Map<String, String> innerMap;

        public MapGetter(Map<String, String> innerMap)
        {
            this.innerMap = innerMap;
        }

        @Override
        public boolean hasAttribute(String name)
        {
            return innerMap.containsKey(name);
        }

        @Override
        public String getAttribute(String name)
        {
            return innerMap.get(name);
        }

        @Override
        public void removeAttribute(String name)
        {
            innerMap.remove(name);
        }

        @Override
        public AttributeGetter copy()
        {
            return new MapGetter(new HashMap<>(innerMap));
        }
    }

    class WrapperGetter implements AttributeGetter
    {
        private final NamedNodeMap innerMap;

        public WrapperGetter(NamedNodeMap innerMap)
        {
            this.innerMap = innerMap;
        }

        @Override
        public boolean hasAttribute(String name)
        {
            return innerMap.getNamedItem(name) != null;
        }

        @Override
        public String getAttribute(String name)
        {
            var node = innerMap.getNamedItem(name);
            if (node == null)
                return null;
            return node.getTextContent();
        }

        @Override
        public void removeAttribute(String name)
        {
            innerMap.removeNamedItem(name);
        }

        @Override
        public AttributeGetter copy()
        {
            return copyOf(innerMap);
        }

    }
}
