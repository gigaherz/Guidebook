package gigaherz.guidebook;

import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.ItemGuidebook;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
@Mod(modid = GuidebookMod.MODID, version = GuidebookMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2,1.13.0)",
        dependencies = "required-after:forge@[14.23.5.2779,)",
        updateJSON = "https://raw.githubusercontent.com/gigaherz/guidebook/master/update.json")
public class GuidebookMod
{
    public static final String MODID = "gbook";
    public static final String VERSION = "@VERSION@";

    // The instance of your mod that Forge uses.
    @Mod.Instance(GuidebookMod.MODID)
    public static GuidebookMod instance;

    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide = "gigaherz.guidebook.client.ClientProxy", serverSide = "gigaherz.guidebook.server.ServerProxy")
    public static IModProxy proxy;

    // Items
    @GameRegistry.ObjectHolder(MODID + ":guidebook")
    public static ItemGuidebook guidebook;

    public static Logger logger;

    public static CreativeTabs tabGuidebooks = new CreativeTabs(MODID)
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(guidebook);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        proxy.preInit();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                new ItemGuidebook().setRegistryName("guidebook")
                        .setTranslationKey(GuidebookMod.MODID + ".guidebook")
                        .setHasSubtypes(true)
                        .setMaxStackSize(1)
                        .setCreativeTab(GuidebookMod.tabGuidebooks)
        );
    }

    @SubscribeEvent
    public static void entityJoinWorld(EntityJoinWorldEvent event)
    {
        Entity e = event.getEntity();
        if (e instanceof EntityPlayer && !e.getEntityWorld().isRemote)
        {
            for (String g : ConfigValues.giveOnFirstJoin)
            {
                String tag = MODID + ":givenBook:" + g;
                if (!e.getTags().contains(tag))
                {
                    ItemHandlerHelper.giveItemToPlayer((EntityPlayer) e, guidebook.of(new ResourceLocation(g)));
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
