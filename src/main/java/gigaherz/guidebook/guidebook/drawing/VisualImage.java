package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.HoverContext;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.LinkContext;
import gigaherz.guidebook.guidebook.util.LinkHelper;
import gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.util.ResourceLocation;

public class VisualImage extends VisualElement implements LinkHelper.ILinkable
{
    public ResourceLocation textureLocation;
    public int tx;
    public int ty;
    public int tw;
    public int th;
    public int w;
    public int h;
    public float scale;

    public LinkContext linkContext = null;

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
        super.draw(nav);
        nav.drawImage(textureLocation, position.x, position.y, tx, ty, w, h, tw, th, scale);
    }

    //public int colorHover = 0xFF77cc66;

    @Override
    public boolean wantsHover()
    {
        return linkContext != null;
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext)
    {
        if (linkContext != null ) {
            linkContext.isHovering = true;
            //Mouse.setNativeCursor(Cursor.)
        }
    }

    @Override
    public void mouseOut(IBookGraphics nav, HoverContext hoverContext)
    {
        if (linkContext != null ) {
            linkContext.isHovering = false;
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        if (linkContext != null)
            LinkHelper.click(nav, linkContext);
    }

    @Override
    public void setLinkContext(LinkContext ctx)
    {
        linkContext = ctx;
    }
}
