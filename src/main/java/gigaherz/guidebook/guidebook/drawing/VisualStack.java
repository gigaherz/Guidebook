package gigaherz.guidebook.guidebook.drawing;

import com.mojang.blaze3d.matrix.MatrixStack;
import gigaherz.guidebook.guidebook.HoverContext;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.SectionRef;
import gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class VisualStack extends VisualElement
{
    public static final int CYCLE_TIME = 1000;//=1s

    public ItemStack[] stacks;
    public float scale = 1.0f;
    public int z;

    public VisualStack(NonNullList<ItemStack> stacks, Size size, int positionMode, float baseline, int verticalAlign, float scale, int z)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.stacks = stacks.toArray(new ItemStack[0]);
        this.scale = scale;
        this.z = z;
    }

    public ItemStack getCurrentStack()
    {
        if (stacks == null || stacks.length == 0)
            return ItemStack.EMPTY;
        long time = System.currentTimeMillis();
        return stacks[(int) ((time / CYCLE_TIME) % stacks.length)];
    }

    @Override
    public void draw(IBookGraphics nav, MatrixStack matrixStack)
    {
        super.draw(nav, matrixStack);
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawItemStack(matrixStack, position.x, position.y, z, stack, 0xFFFFFFFF, scale);
        }
    }

    @Override
    public boolean wantsHover()
    {
        return true;
    }

    @Override
    public void mouseOver(IBookGraphics nav, HoverContext hoverContext, MatrixStack matrixStack)
    {
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawTooltip(matrixStack, stack, hoverContext.mouseX, hoverContext.mouseY);
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        SectionRef ref = nav.getBook().getStackLink(getCurrentStack());
        if (ref != null)
            nav.navigateTo(ref);
    }
}
