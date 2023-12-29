package dev.gigaherz.guidebook.example;

import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public class GuidebookExample
{
    public static final DeferredItem<GuidebookItem> GUIDEBOOK_ITEM = DeferredItem.createItem(new ResourceLocation("gbook","guidebook"));
    public static final Lazy<ItemStack> MY_BOOK = Lazy.of(() -> GUIDEBOOK_ITEM.asOptional().map(item -> {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("Book", "my:book.xml");
        return stack;
    }).orElse(ItemStack.EMPTY));

    public void test()
    {
    }

    public static void checkGbookGiven(EntityJoinLevelEvent event)
    {
        if (MY_BOOK.get().getCount() > 0)
        {
            final Entity entity = event.getEntity();
            final String bookPlayerTag = "MODID:someTagLikeGbookGiven";

            if (entity instanceof Player player && !entity.level().isClientSide && !entity.getTags().contains(bookPlayerTag))
            {
                ItemHandlerHelper.giveItemToPlayer(player, MY_BOOK.get().copy());
                entity.addTag(bookPlayerTag);
            }
        }
    }
}
