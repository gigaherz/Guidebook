package gigaherz.guidebook;

import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.ItemGuidebook;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
@Mod(modid = GuidebookMod.MODID, version = GuidebookMod.VERSION,
        acceptedMinecraftVersions = "[1.9.4,1.11.0)",
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
    public static ItemGuidebook guidebook;

    public static Logger logger;

    public static CreativeTabs tabMagic = new CreativeTabs(MODID)
    {
        @Override
        public Item getTabIconItem()
        {
            return guidebook;
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        BookRegistry.registerBook(new ResourceLocation("guidebook:xml/guidebook.xml"));

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        registerRecipes();

        proxy.init();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                guidebook = new ItemGuidebook("guidebook")
        );
    }

    private void registerRecipes()
    {
        GameRegistry.addShapelessRecipe(guidebook.of(location("xml/guidebook.xml")), Items.BOOK);
    }

    public static ResourceLocation location(String location)
    {
        return new ResourceLocation(MODID, location);
    }
}
