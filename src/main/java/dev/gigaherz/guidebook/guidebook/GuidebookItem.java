package dev.gigaherz.guidebook.guidebook;

import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.client.ClientAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.EffectiveSide;

import javax.annotation.Nullable;
import java.util.List;

public class GuidebookItem extends Item
{
    public GuidebookItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return showBook(context.getLevel(), context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand hand)
    {
        ItemStack stack = playerIn.getItemInHand(hand);
        return showBook(worldIn, stack);
    }

    private InteractionResult showBook(Level worldIn, ItemStack stack)
    {
        if (!worldIn.isClientSide)
            return InteractionResult.SUCCESS;

        var book = stack.get(GuidebookMod.BOOK_ID);
        if (book == null)
            return InteractionResult.FAIL;

        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientAPI.displayBook(book);

        return InteractionResult.SUCCESS;
    }

    public ItemStack of(ResourceLocation book)
    {
        ItemStack stack = new ItemStack(this);
        stack.set(GuidebookMod.BOOK_ID, book);
        return stack;
    }

    @Nullable
    public static ResourceLocation getBookLocation(ItemStack stack)
    {
        return stack.get(GuidebookMod.BOOK_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn)
    {
        super.appendHoverText(stack, context, tooltip, flagIn);

        if (flagIn == TooltipFlag.Default.ADVANCED)
        {
            var book = getBookLocation(stack);
            if (book != null)
            {
                tooltip.add(Component.translatable("text.gbook.tooltip.book",
                        Component.literal(book.toString()).withStyle(ChatFormatting.ITALIC)
                ).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Component getName(ItemStack stack)
    {
        var book = getBookLocation(stack);
        if (book != null)
        {
            if (FMLEnvironment.dist == Dist.CLIENT && EffectiveSide.get().isClient())
                return Component.literal(ClientAPI.getBookName(book));
        }

        return super.getName(stack);
    }
}
