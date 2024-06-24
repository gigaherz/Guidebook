package dev.gigaherz.guidebook.guidebook.elements;

import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualImage;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementImage extends ElementInline
{
    public ResourceLocation textureLocation;
    public int tx = 0;
    public int ty = 0;
    public int tw = 0;
    public int th = 0;

    public float scale = 1.0f;

    public ElementImage(boolean isFirstElement, boolean isLastElement)
    {
        super(isFirstElement, isLastElement);
    }

    private Size getVisualSize()
    {
        int width = (int) ((w > 0 ? w : tw) * scale);
        int height = (int) ((h > 0 ? h : th) * scale);
        return new Size(width, height);
    }

    private VisualImage getVisual()
    {
        return new VisualImage(getVisualSize(), position, baseline, verticalAlignment, textureLocation, tx, ty, tw, th, (w > 0 ? w : tw), (h > 0 ? h : th), scale);
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
        if (position != POS_RELATIVE)
            return bounds.position.y();
        return bounds.position.y() + element.size.height();
    }

    @Override
    public void findTextures(Set<Material> textures)
    {
        // No need to require them, since they are used dynamically and not stitched.
        //textures.add(textureLocation);
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        super.parse(context, attributes);

        tx = attributes.getAttribute("tx", tx);
        ty = attributes.getAttribute("ty", ty);
        tw = attributes.getAttribute("tw", tw);
        th = attributes.getAttribute("th", th);
        textureLocation = attributes.getAttribute("src", textureLocation);
        scale = attributes.getAttribute("scale", scale);
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return String.format("<img src=\"%s\" .../>", textureLocation);
    }

    @Override
    public ElementInline copy()
    {
        ElementImage img = super.copy(new ElementImage(isFirstElement, isLastElement));

        img.textureLocation = ResourceLocation.parse(textureLocation.toString());
        img.tx = tx;
        img.ty = ty;
        img.tw = tw;
        img.th = th;
        img.scale = scale;
        return img;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
