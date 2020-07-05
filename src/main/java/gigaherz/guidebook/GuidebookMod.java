package gigaherz.guidebook;

import gigaherz.guidebook.client.ClientEvents;
import gigaherz.guidebook.client.ClientProxy;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.ItemGuidebook;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

@Mod(GuidebookMod.MODID)
public class GuidebookMod
{
    public static final String MODID = "gbook";

    public static GuidebookMod instance;

    public static final IModProxy proxy = DistExecutor.runForDist(() -> () -> new ClientProxy(), () -> () -> new IModProxy()
    {
    });

    // Items
    @ObjectHolder("gbook:guidebook")
    public static ItemGuidebook guidebook;

    public static final Logger logger = LogManager.getLogger(MODID);

    public static final ItemGroup tabGuidebooks = new ItemGroup(MODID)
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(guidebook);
        }

        @Override
        public void fill(NonNullList<ItemStack> items)
        {
            //super.fill(items);

            for (var resourceLocation : GuidebookMod.proxy.getBooksList())
            {
                items.add(guidebook.of(resourceLocation));
            }
        }
    };

    public GuidebookMod()
    {
        instance = this;

        DistExecutor.runWhenOn(Dist.CLIENT, () -> BookRegistry::injectCustomResourcePack);

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addGenericListener(Item.class, this::registerItems);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::modConfig);
        modEventBus.addListener(this::serverStarting);

        MinecraftForge.EVENT_BUS.addListener(this::playerLogIn);

        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ConfigValues.CLIENT_SPEC);
    }

    private void serverStarting(FMLServerStartingEvent event)
    {
        BookRegistry.initServerResourceListener(event.getServer());
    }

    private void modConfig(ModConfig.ModConfigEvent event)
    {
        var config = event.getConfig();
        if (config.getSpec() == ConfigValues.CLIENT_SPEC)
            ConfigValues.refreshClient();
        else if (config.getSpec() == ConfigValues.SERVER_SPEC)
            ConfigValues.refreshServer();
    }


    private void clientSetup(FMLClientSetupEvent event)
    {
        ClientProxy.initialize();
    }

    private void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                new ItemGuidebook(new Item.Properties()
                        .maxStackSize(1)
                        .group(GuidebookMod.tabGuidebooks)
                        .setISTER(() -> ClientEvents::createBookItemRenderer)
                ).setRegistryName("guidebook")
        );
    }

    private void playerLogIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        var e = event.getPlayer();
        if (!e.world.isRemote)
        {
            for (var g : ConfigValues.giveOnFirstJoin)
            {
                var tag = String.format("%s:givenBook:%s", MODID, g);
                if (!e.getTags().contains(tag))
                {
                    ItemHandlerHelper.giveItemToPlayer(e, guidebook.of(new ResourceLocation(g)));
                    e.addTag(tag);
                }
            }
        }
    }

    public static ResourceLocation location(String location)
    {
        return new ResourceLocation(MODID, location);
    }
}

