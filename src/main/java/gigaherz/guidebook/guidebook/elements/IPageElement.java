package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;

import java.util.Set;

public interface IPageElement
{
    int apply(IBookGraphics nav, int left, int top, int width);

    default void findTextures(Set<ResourceLocation> textures)
    {
    }

    void parse(NamedNodeMap attributes);

    IPageElement copy();
}
