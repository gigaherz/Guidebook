package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.SectionRef;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class VisualStack extends VisualElement
{
    public final int CYCLE_TIME;

    public ItemStack[] stacks;
    public float scale = 1.0f;
    public int z;

    public VisualStack(NonNullList<ItemStack> stacks, Size size, int positionMode, float baseline, int verticalAlign, float scale, int z, int cycle_time)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.stacks = stacks.toArray(new ItemStack[0]);
        this.scale = scale;
        this.z = z;
        this.CYCLE_TIME = cycle_time;
    }

    public ItemStack getCurrentStack()
    {
        if (stacks == null || stacks.length == 0)
            return ItemStack.EMPTY;
        long time = System.currentTimeMillis();
        return stacks[(int) ((time / CYCLE_TIME) % stacks.length)];
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawItemStack(position.x, position.y, z, stack, 0xFFFFFFFF, scale);
        }
    }

    @Override
    public boolean wantsHover()
    {
        return true;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        ItemStack stack = getCurrentStack();
        if (stack.getCount() > 0)
        {
            nav.drawTooltip(stack, x, y);
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
