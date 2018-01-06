package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ElementPanel extends ElementContainer
{
    public final List<Element> innerElements;
    public boolean asPercent;
    public int space;

    public ElementPanel(int defaultPositionMode)
    {
        super(defaultPositionMode);
        this.innerElements = Lists.newArrayList();
    }

    public ElementPanel(int defaultPositionMode, List<Element> innerElements)
    {
        super(defaultPositionMode);
        this.innerElements = Lists.newArrayList(innerElements);
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("height");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                asPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            space = Ints.tryParse(t);
        }
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, int left, int top, int width, int height)
    {
        for(Element element : innerElements)
        {
            element.reflow(list, nav, left, top, width, height);
        }
        return top+space;
    }

    @Override
    public void findTextures(Set<ResourceLocation> textures)
    {
        for (Element child : innerElements)
        {
            child.findTextures(textures);
        }
    }

    @Override
    public Collection<Element> getChildren()
    {
        return innerElements;
    }

    @Override
    public Element copy()
    {
        ElementPanel space = super.copy(new ElementPanel(position));
        space.asPercent = asPercent;
        space.space = this.space;
        return space;
    }

    @Override
    public Element applyTemplate(List<Element> sourceElements)
    {
        ElementPanel paragraph = super.copy(new ElementPanel(position));
        paragraph.space = space;
        for(Element element : innerElements)
        {
            paragraph.innerElements.add(element.applyTemplate(sourceElements));
        }
        return paragraph;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
