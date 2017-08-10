package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Space implements IContainerPageElement
{
    public final List<IPageElement> innerElements;
    public boolean asPercent;
    public int space;

    public Space()
    {
        this.innerElements = Lists.newArrayList();
    }

    public Space(List<IPageElement> innerElements)
    {
        this.innerElements = Lists.newArrayList(innerElements);
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top, int width)
    {
        int top0 = top;

        for (IPageElement child : innerElements)
        {
            top0 += child.apply(nav, left, top0, width);
        }

        return asPercent ? nav.getPageHeight() * space / 100 : space;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
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
    public void findTextures(Set<ResourceLocation> textures)
    {
        for (IPageElement child : innerElements)
        {
            child.findTextures(textures);
        }
    }

    @Override
    public Collection<IPageElement> getChildren()
    {
        return innerElements;
    }

    @Override
    public IPageElement copy()
    {
        Space space = new Space();
        space.asPercent = asPercent;
        space.space = this.space;
        return space;
    }
}
