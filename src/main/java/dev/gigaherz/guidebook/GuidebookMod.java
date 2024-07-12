package dev.gigaherz.guidebook;

import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(GuidebookMod.MODID)
public class GuidebookMod
{
    public static final String MODID = "gbook";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(MODID);

    public static final DeferredItem<GuidebookItem> GUIDEBOOK_ITEM = ITEMS.register("guidebook", () -> new GuidebookItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<DataComponentType<ResourceLocation>>
            BOOK_ID = DATA_COMPONENTS.register("book_id", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUIDEBOOKS = TABS
            .register("guidebook_books", () -> new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0)
                    .icon(() -> new ItemStack(bookItem()))
                    .title(Component.translatable("itemGroup.gbook"))
                    .displayItems((featureFlags, output) -> {
                        for (ResourceLocation resourceLocation : BookRegistry.getBooksList())
                        {
                            output.accept(bookItem() .of(resourceLocation));
                        }
                    }).build());

    public static final Logger logger = LogManager.getLogger(MODID);

    public static GuidebookItem bookItem()
    {
        return GUIDEBOOK_ITEM.get();
    }

    public GuidebookMod(ModContainer container, IEventBus modEventBus)
    {
        modEventBus.addListener(this::modConfigLoad);
        modEventBus.addListener(this::modConfigReload);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::playerLogIn);

        container.registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ConfigValues.CLIENT_SPEC);
    }

    public void modConfigLoad(ModConfigEvent.Loading event)
    {
        refreshConfig(event);
    }

    public void modConfigReload(ModConfigEvent.Reloading event)
    {
        refreshConfig(event);
    }

    private static void refreshConfig(ModConfigEvent event)
    {
        ModConfig config = event.getConfig();
        if (config.getSpec() == ConfigValues.CLIENT_SPEC)
            ConfigValues.refreshClient();
        else if (config.getSpec() == ConfigValues.SERVER_SPEC)
            ConfigValues.refreshServer();
    }


    private void playerLogIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        Player e = event.getEntity();
        if (!e.level().isClientSide)
        {
            for (String g : ConfigValues.giveOnFirstJoin)
            {
                String tag = String.format("%s:givenBook:%s", MODID, g);
                if (!e.getTags().contains(tag))
                {
                    ItemHandlerHelper.giveItemToPlayer(e, bookItem().of(ResourceLocation.parse(g)));
                    e.addTag(tag);
                }
            }
        }
    }

    public static ResourceLocation location(String location)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, location);
    }
}

