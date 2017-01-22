package gigaherz.guidebook;

import com.google.common.collect.Lists;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.ItemGuidebook;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.Logger;

import java.util.List;

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

    // Config
    public static int bookGUIScale;

    public static List<String> giveOnFirstJoin;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        String[] books = config.get("Books", "BookList", new String[]{GuidebookMod.location("xml/guidebook.xml").toString()}).getStringList();
        String[] give = config.get("Books", "GiveOnFirstJoin", new String[0]).getStringList();
        bookGUIScale = config.get("general", "BookGUIScale", -1, "-1 for same as GUI scale, 0 for auto, 1+ for small/medium/large").getInt();
        config.save();

        for (String book : books)
        { BookRegistry.registerBook(new ResourceLocation(book)); }

        giveOnFirstJoin = Lists.newArrayList(give);

        proxy.preInit(event.getModConfigurationDirectory());
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

    @SubscribeEvent
    public static void entityJoinWorld(EntityJoinWorldEvent event)
    {
        Entity e = event.getEntity();
        if (e instanceof EntityPlayer && !e.getEntityWorld().isRemote)
        {
            for (String g : giveOnFirstJoin)
            {
                String tag = MODID + ":givenBook:" + g;
                if (!e.getTags().contains(tag))
                {
                    ItemHandlerHelper.giveItemToPlayer((EntityPlayer)e, guidebook.of(new ResourceLocation(g)));
                    e.addTag(tag);
                }
            }
        }
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
