package dev.gigaherz.guidebook.example;

import gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class GuidebookExample
{
    public static final RegistryObject<GuidebookItem> GUIDEBOOK_ITEM = RegistryObject.of(new ResourceLocation("gbook","guidebook"), ForgeRegistries.ITEMS);
    public static final Lazy<ItemStack> MY_BOOK = Lazy.of(() -> GUIDEBOOK_ITEM.map(item -> {
        ItemStack stack = new ItemStack(item);
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putString("Book", "my:book.xml");
        return stack;
    }).orElse(ItemStack.EMPTY));

    public void test()
    {
    }

    public static void checkGbookGiven(EntityJoinWorldEvent event)
    {
        if (MY_BOOK.get().getCount() > 0)
        {
            final Entity entity = event.getEntity();
            final String bookPlayerTag = "MODID:someTagLikeGbookGiven";

            if (entity instanceof PlayerEntity && !entity.getEntityWorld().isRemote && !entity.getTags().contains(bookPlayerTag))
            {
                ItemHandlerHelper.giveItemToPlayer((PlayerEntity) entity, MY_BOOK.get().copy());
                entity.addTag(bookPlayerTag);
            }
        }
    }
}
