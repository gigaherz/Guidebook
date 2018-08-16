package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.util.ResourceLocation;

public class VisualImage extends VisualElement
{
    public ResourceLocation textureLocation;
    public int tx;
    public int ty;
    public int tw;
    public int th;
    public int w;
    public int h;
    public float scale;

    public VisualImage(Size size, int positionMode, float baseline, int verticalAlign, ResourceLocation textureLocation,
                       int tx, int ty, int tw, int th, int w, int h, float scale)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.textureLocation = textureLocation;
        this.tx = tx;
        this.ty = ty;
        this.tw = tw;
        this.th = th;
        this.w = w;
        this.h = h;
        this.scale = scale;
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.drawImage(textureLocation, position.x, position.y, tx, ty, w, h, tw, th, scale);
    }
}
