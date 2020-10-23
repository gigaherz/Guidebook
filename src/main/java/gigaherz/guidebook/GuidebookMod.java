package gigaherz.guidebook;

import gigaherz.guidebook.client.ClientEvents;
import gigaherz.guidebook.client.ClientProxy;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GuidebookMod.MODID)
public class GuidebookMod
{
    public static final String MODID = "gbook";

    public static GuidebookMod instance;

    public static final IModProxy proxy = DistExecutor.unsafeRunForDist(() -> () -> new ClientProxy(), () -> () -> new IModProxy()
    {
    });

    // Items
    @ObjectHolder("gbook:guidebook")
    public static GuidebookItem guidebook;

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

            for (ResourceLocation resourceLocation : GuidebookMod.proxy.getBooksList())
            {
                items.add(guidebook.of(resourceLocation));
            }
        }
    };

    public GuidebookMod()
    {
        instance = this;

        DeferredWorkQueue.runLater(() -> {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> BookRegistry::injectCustomResourcePack);
        });

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addGenericListener(Item.class, this::registerItems);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::modConfig);

        MinecraftForge.EVENT_BUS.addListener(this::playerLogIn);

        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ConfigValues.CLIENT_SPEC);
    }

    private void modConfig(ModConfig.ModConfigEvent event)
    {
        ModConfig config = event.getConfig();
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
                new GuidebookItem(new Item.Properties()
                        .maxStackSize(1)
                        .group(GuidebookMod.tabGuidebooks)
                        .setISTER(() -> ClientEvents::createBookItemRenderer)
                ).setRegistryName("guidebook")
        );
    }

    private void playerLogIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        PlayerEntity e = event.getPlayer();
        if (!e.world.isRemote)
        {
            for (String g : ConfigValues.giveOnFirstJoin)
            {
                String tag = String.format("%s:givenBook:%s", MODID, g);
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

