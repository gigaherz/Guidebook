package dev.gigaherz.guidebook.guidebook;

import com.google.common.base.Strings;
import dev.gigaherz.guidebook.client.BookItemRenderer;
import dev.gigaherz.guidebook.client.ClientAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class GuidebookItem extends Item
{
    public GuidebookItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return showBook(context.getLevel(), context.getItemInHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand)
    {
        ItemStack stack = playerIn.getItemInHand(hand);
        return showBook(worldIn, stack);
    }

    private InteractionResultHolder<ItemStack> showBook(Level worldIn, ItemStack stack)
    {
        if (!worldIn.isClientSide)
            return InteractionResultHolder.success(stack);

        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("Book", Tag.TAG_STRING))
            return InteractionResultHolder.fail(stack);

        if (FMLEnvironment.dist == Dist.CLIENT)
            ClientAPI.displayBook(nbt.getString("Book"));

        return InteractionResultHolder.success(stack);
    }

    public ItemStack of(ResourceLocation book)
    {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = new CompoundTag();
        tag.putString("Book", book.toString());
        stack.setTag(tag);
        return stack;
    }

    @Nullable
    public String getBookLocation(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null)
        {
            return tag.getString("Book");
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
    {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        if (flagIn == TooltipFlag.Default.ADVANCED)
        {
            String book = getBookLocation(stack);
            if (!Strings.isNullOrEmpty(book))
            {
                tooltip.add(Component.translatable("text.gbook.tooltip.book",
                        Component.literal(book).withStyle(ChatFormatting.ITALIC)
                ).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public Component getName(ItemStack stack)
    {
        String book = getBookLocation(stack);
        if (!Strings.isNullOrEmpty(book))
        {
            if (FMLEnvironment.dist == Dist.CLIENT && EffectiveSide.get().isClient())
                return Component.literal(ClientAPI.getBookName(book));
        }

        return super.getName(stack);
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

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer)
    {
        consumer.accept(new IItemRenderProperties()
        {
            private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy.of(() -> new BookItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer()
            {
                return ister.get();
            }
        });
    }
}
