package gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import gigaherz.common.ItemRegistered;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class ItemGuidebook extends ItemRegistered
{
    public ItemGuidebook(String name)
    {
        super(name);
        setMaxStackSize(1);
        setUnlocalizedName(GuidebookMod.MODID + ".guidebook");
        setCreativeTab(GuidebookMod.tabMagic);
        setHasSubtypes(true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        return showBook(worldIn, playerIn.getHeldItem(hand));
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

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey("Book", Constants.NBT.TAG_STRING))
            return EnumActionResult.FAIL;

        GuidebookMod.proxy.displayBook(nbt.getString("Book"));

        return EnumActionResult.SUCCESS;
    }

    public ItemStack of(ResourceLocation book)
    {
        ItemStack stack = new ItemStack(this);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Book", book.toString());
        stack.setTagCompound(tag);
        return stack;
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> subItems)
    {
        for (ResourceLocation resourceLocation : GuidebookMod.proxy.getBooksList())
        {
            subItems.add(of(resourceLocation));
        }
    }

    @Nullable
    public String getBookLocation(ItemStack stack)
    {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null)
        {
            return tag.getString("Book");
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced)
    {
        super.addInformation(stack, playerIn, tooltip, advanced);

        if (advanced)
        {
            String book = getBookLocation(stack);
            if (!Strings.isNullOrEmpty(book))
            {
                tooltip.add(String.format("Book: " + book));
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        String book = getBookLocation(stack);
        if (!Strings.isNullOrEmpty(book))
        {
            return GuidebookMod.proxy.getBookName(book);
        }

        return super.getItemStackDisplayName(stack);
    }
}
