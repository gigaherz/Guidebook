package gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.client.ClientAPI;
import gigaherz.guidebook.client.ClientHandlers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.List;

public class GuidebookItem extends Item
{
    public GuidebookItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        return showBook(context.getWorld(), context.getItem()).getType();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand hand)
    {
        ItemStack stack = playerIn.getHeldItem(hand);
        return showBook(worldIn, stack);
    }

    private ActionResult<ItemStack> showBook(World worldIn, ItemStack stack)
    {
        if (!worldIn.isRemote)
            return ActionResult.resultSuccess(stack);

        CompoundNBT nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Book", Constants.NBT.TAG_STRING))
            return ActionResult.resultFail(stack);

        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientAPI.displayBook(nbt.getString("Book"));

        return ActionResult.resultSuccess(stack);
    }

    public ItemStack of(ResourceLocation book)
    {
        ItemStack stack = new ItemStack(this);
        CompoundNBT tag = new CompoundNBT();
        tag.putString("Book", book.toString());
        stack.setTag(tag);
        return stack;
    }

    @Nullable
    public String getBookLocation(ItemStack stack)
    {
        CompoundNBT tag = stack.getTag();
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
                tooltip.add(new TranslationTextComponent("text.gbook.tooltip.book",
                        new StringTextComponent(book).mergeStyle(TextFormatting.ITALIC)
                ).mergeStyle(TextFormatting.GRAY));
            }
        }
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack)
    {
        String book = getBookLocation(stack);
        if (!Strings.isNullOrEmpty(book))
        {
            if (FMLEnvironment.dist == Dist.CLIENT && EffectiveSide.get().isClient())
                return new StringTextComponent(ClientAPI.getBookName(book));
        }

        return super.getDisplayName(stack);
    }

    public static String getSubtype(ItemStack stack)
    {
        if (stack.getItem() instanceof GuidebookItem)
        {
            String bookLocation = ((GuidebookItem) stack.getItem()).getBookLocation(stack);
            return bookLocation == null ? "" : bookLocation;
        }
        return "";
    }
}
