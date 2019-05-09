package gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class ItemGuidebook extends Item
{
    public ItemGuidebook(Properties properties)
    {
        super(properties);
    }

    @Override
    public EnumActionResult onItemUse(ItemUseContext context)
    {
        return showBook(context.getWorld(), context.getItem());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        ItemStack stack = playerIn.getHeldItem(hand);
        EnumActionResult result = showBook(worldIn, stack);
        return ActionResult.newResult(result, stack);
    }

    private EnumActionResult showBook(World worldIn, ItemStack stack)
    {
        if (!worldIn.isRemote)
            return EnumActionResult.FAIL;

        NBTTagCompound nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Book", Constants.NBT.TAG_STRING))
            return EnumActionResult.FAIL;

        GuidebookMod.proxy.displayBook(nbt.getString("Book"));

        return EnumActionResult.SUCCESS;
    }

    public ItemStack of(ResourceLocation book)
    {
        ItemStack stack = new ItemStack(this);
        NBTTagCompound tag = new NBTTagCompound();
        tag.putString("Book", book.toString());
        stack.setTag(tag);
        return stack;
    }

    @Nullable
    public String getBookLocation(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTag();
        if (tag != null)
        {
            return tag.getString("Book");
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        if (flagIn == ITooltipFlag.TooltipFlags.ADVANCED)
        {
            String book = getBookLocation(stack);
            if (!Strings.isNullOrEmpty(book))
            {
                tooltip.add(new TextComponentTranslation("text.guidebook.tooltip.book", new TextComponentString(book)));
            }
        }
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack)
    {
        String book = getBookLocation(stack);
        if (!Strings.isNullOrEmpty(book))
        {
            return new TextComponentString(GuidebookMod.proxy.getBookName(book));
        }

        return super.getDisplayName(stack);
    }
}
