package gigaherz.guidebook.guidebook.drawing;

import net.minecraft.item.ItemStack;

public class SizedStack
{
    public Point position;
    public Size size;
    public ItemStack stack;

    public SizedStack(ItemStack stack, Size size)
    {
        this.position= new Point();
        this.size=size;
        this.stack = stack;
    }
}
