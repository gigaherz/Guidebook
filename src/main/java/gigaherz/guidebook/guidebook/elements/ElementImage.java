package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.*;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementImage extends Element
{
    public ResourceLocation textureLocation;
    public int tx = 0;
    public int ty = 0;
    public int tw = 0;
    public int th = 0;

    private Size getVisualSize()
    {
        int width = (int) (w * scale);
        int height = (int) (h * scale);
        return new Size(width,height);
    }

    private VisualImage getVisual()
    {
        return new VisualImage(getVisualSize(), textureLocation, tx, ty, tw, th);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return Collections.singletonList(getVisual());
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        VisualImage element = getVisual();
        element.position = applyPosition(bounds.position, bounds.position);
        paragraph.add(element);
        if (position != 0)
            return bounds.position.y;
        return bounds.position.y + element.size.height;
    }

    @Override
    public void findTextures(Set<ResourceLocation> textures)
    {
        // No need to require them, since they are used dynamically and not stitched.
        //textures.add(textureLocation);
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("tx");
        if (attr != null)
        {
            tx = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("ty");
        if (attr != null)
        {
            ty = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("tw");
        if (attr != null)
        {
            tw = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("th");
        if (attr != null)
        {
            th = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("src");
        if (attr != null)
        {
            textureLocation = new ResourceLocation(attr.getTextContent());
        }
    }

    @Override
    public Element copy()
    {
        ElementImage img = super.copy(new ElementImage());

        img.textureLocation = new ResourceLocation(textureLocation.toString());
        img.tx = tx;
        img.ty = ty;
        img.tw = tw;
        img.th = th;
        return img;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
