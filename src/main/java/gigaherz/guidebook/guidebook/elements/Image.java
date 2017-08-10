package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Size;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Image implements IParagraphElement
{
    public ResourceLocation textureLocation;
    public int x = 0;
    public int y = 0;
    public int w = 0;
    public int h = 0;
    public int tx = 0;
    public int ty = 0;
    public int tw = 0;
    public int th = 0;
    public float scale = 1.0f;

    public Image()
    {
    }

    @Override
    public List<Size> measure(IBookGraphics nav, int _width, int firstLineWidth)
    {
        int width = (int) (w * scale);
        int height = (int) (h * scale);
        return Collections.singletonList(new Size(width,height));
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top, int width)
    {
        nav.drawImage(textureLocation, left + x, top + y, tx, ty, w, h, tw, th, scale);
        return 0;
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
        Node attr = attributes.getNamedItem("x");
        if (attr != null)
        {
            x = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("y");
        if (attr != null)
        {
            y = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("w");
        if (attr != null)
        {
            w = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("h");
        if (attr != null)
        {
            h = Ints.tryParse(attr.getTextContent());
        }
        attr = attributes.getNamedItem("tx");
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

        attr = attributes.getNamedItem("scale");
        if (attr != null)
        {
            Float f = Floats.tryParse(attr.getTextContent());
            scale = f != null ? f : scale;
        }
    }

    @Override
    public IParagraphElement copy()
    {
        Image img = new Image();

        img.textureLocation = new ResourceLocation(textureLocation.toString());
        img.x = x;
        img.y = y;
        img.w = w;
        img.h = h;
        img.tx = tx;
        img.ty = ty;
        img.tw = tw;
        img.th = th;
        return img;
    }
}
