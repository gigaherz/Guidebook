package dev.gigaherz.guidebook.example;

import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class GuidebookExample
{
    public static final RegistryObject<GuidebookItem> GUIDEBOOK_ITEM = RegistryObject.of(new ResourceLocation("gbook","guidebook"), ForgeRegistries.ITEMS);
    public static final Lazy<ItemStack> MY_BOOK = Lazy.of(() -> GUIDEBOOK_ITEM.map(item -> {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
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

            if (entity instanceof Player player && !entity.level.isClientSide && !entity.getTags().contains(bookPlayerTag))
            {
                ItemHandlerHelper.giveItemToPlayer(player, MY_BOOK.get().copy());
                entity.addTag(bookPlayerTag);
            }
        }
    }
}
