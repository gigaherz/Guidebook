package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Template extends Space implements IContainerPageElement
{
    public final List<IPageElement> innerElements;

    public Template()
    {
        this.innerElements = Lists.newArrayList();
    }

    public Template(List<IPageElement> innerElements)
    {
        this.innerElements = Lists.newArrayList(innerElements);
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        int top0 = top;

        for (IPageElement child : innerElements)
        {
            top0 += child.apply(nav, left, top0);
        }

        return super.apply(nav, left, top);
    }

    @Override
    public void findTextures(Set<ResourceLocation> textures)
    {
        for (IPageElement child : innerElements)
        { child.findTextures(textures); }
    }

    @Override
    public IPageElement copy()
    {
        return new Template(innerElements);
    }

    @Override
    public Collection<IPageElement> getChildren()
    {
        return innerElements;
    }
}
