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

    public VisualImage(Size size, int positionMode, float baseline, int verticalAlign, ResourceLocation textureLocation, int tx, int ty, int tw, int th)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.textureLocation = textureLocation;
        this.tx = tx;
        this.ty = ty;
        this.tw = tw;
        this.th = th;
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.drawImage(textureLocation, position.x, position.y, tx, ty, size.width, size.height, tw, th, 1.0f);
    }
}
