package gigaherz.guidebook.guidebook.drawing;

import net.minecraft.util.ResourceLocation;

public class SizedImage
{
    public Point position;
    public Size size;

    public ResourceLocation textureLocation;
    public int tx;
    public int ty;
    public int tw;
    public int th;

    public SizedImage(Size size, ResourceLocation textureLocation, int tx, int ty, int tw, int th)
    {
        this.textureLocation = textureLocation;
        this.tx = tx;
        this.ty = ty;
        this.tw = tw;
        this.th = th;
        this.position= new Point();
        this.size=size;
    }
}
