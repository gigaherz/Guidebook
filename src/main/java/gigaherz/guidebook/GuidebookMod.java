package gigaherz.guidebook;

import gigaherz.guidebook.client.ClientHandlers;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.GuidebookItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
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

    // Items
    @ObjectHolder("gbook:guidebook")
    public static GuidebookItem guidebook;

    public static final Logger logger = LogManager.getLogger(MODID);

    public static final CreativeModeTab tabGuidebooks = new CreativeModeTab(MODID)
    {
        @Override
        public ItemStack makeIcon()
        {
            return new ItemStack(guidebook);
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items)
        {
            //super.fill(items);

            for (ResourceLocation resourceLocation : BookRegistry.getBooksList())
            {
                items.add(guidebook.of(resourceLocation));
            }
        }
    };

    public GuidebookMod()
    {
        instance = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addGenericListener(Item.class, this::registerItems);
        modEventBus.addListener(this::modConfig);

        MinecraftForge.EVENT_BUS.addListener(this::playerLogIn);

        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ConfigValues.SERVER_SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ConfigValues.CLIENT_SPEC);
    }

    private void modConfig(ModConfigEvent event)
    {
        ModConfig config = event.getConfig();
        if (config.getSpec() == ConfigValues.CLIENT_SPEC)
            ConfigValues.refreshClient();
        else if (config.getSpec() == ConfigValues.SERVER_SPEC)
            ConfigValues.refreshServer();
    }

    private void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                new GuidebookItem(new Item.Properties()
                        .stacksTo(1)
                        .tab(GuidebookMod.tabGuidebooks)
                ).setRegistryName("guidebook")
        );
    }

    private void playerLogIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        Player e = event.getPlayer();
        if (!e.level.isClientSide)
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

