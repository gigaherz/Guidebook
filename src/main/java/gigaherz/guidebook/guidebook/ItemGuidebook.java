package gigaherz.guidebook.guidebook;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gigaherz.common.ItemRegistered;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.List;
import java.util.Set;

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
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        return showBook(worldIn, stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
        EnumActionResult result = showBook(worldIn, itemStackIn);
        return ActionResult.newResult(result, itemStackIn);
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

    public ItemStack of(String book)
    {
        ItemStack stack = new ItemStack(this);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Book", book);
        stack.setTagCompound(tag);
        return stack;
    }

    private final Set<String> registeredBooks = Sets.newHashSet();
    private final List<ItemStack> registeredStacks = Lists.newArrayList();
    public ItemStack register(String book)
    {
        if (registeredBooks.contains(book))
            throw new KeyAlreadyExistsException("There's already a book registered for this resource location");

        ItemStack stack = of(book);
        registeredBooks.add(book);
        registeredStacks.add(stack);
        return stack;
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems)
    {
        subItems.addAll(registeredStacks);
    }
}
