package dev.gigaherz.guidebook.example;

import dev.gigaherz.guidebook.data.BookProvider;
import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = "gbook")
public class GuidebookExample
{
    public static final RegistryObject<GuidebookItem> GUIDEBOOK_ITEM = RegistryObject.create(new ResourceLocation("gbook","guidebook"), ForgeRegistries.ITEMS);
    public static final Lazy<ItemStack> MY_BOOK = Lazy.of(() -> GUIDEBOOK_ITEM.map(item -> {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("Book", "my:book.xml");
        return stack;
    }).orElse(ItemStack.EMPTY));

    public void test()
    {
    }

    @SubscribeEvent
    public static void testDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(event.includeClient(), new BookProvider(generator, event.getExistingFileHelper(), "gbooktest") {

            @Override
            public String getName()
            {
                return "Guidebook Test";
            }

            @Override
            protected void createBooks()
            {
                book("testbook")
                        .chapter("testchapter1", chapterBuilder -> chapterBuilder.page("testpage1", pageBuilder -> pageBuilder.text("testtext1")))
                        .chapter("testchapter2", chapterBuilder -> {});
                book("extracted-testbook", true, true, true)
                        .chapter("testchapter1", chapterBuilder -> chapterBuilder.page("testpage1", pageBuilder -> pageBuilder.text("testtext1")))
                        .chapter("testchapter2", chapterBuilder -> {});
            }
        });
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
