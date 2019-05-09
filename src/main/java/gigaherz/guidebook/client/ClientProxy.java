package gigaherz.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import gigaherz.guidebook.guidebook.client.ModelHandle;
import gigaherz.guidebook.guidebook.conditions.AdvancementCondition;
import gigaherz.guidebook.guidebook.conditions.BasicConditions;
import gigaherz.guidebook.guidebook.conditions.CompositeCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus= Mod.EventBusSubscriber.Bus.FORGE)
public class ClientProxy implements IModProxy
{
    public ClientProxy()
    {
        BookRegistry.injectCustomResourcePack();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        ModelLoaderRegistry.registerLoader(new BookBakedModel.ModelLoader());

        //registerItemModel(GuidebookMod.guidebook);

        //ModelLoader.registerItemVariants(GuidebookMod.guidebook, BookRegistry.gatherBookModels());
    }

    /*@SubscribeEvent
    public static void colors(ColorHandlerEvent.Item event)
    {
    }*/

    @Override
    public void preInit()
    {
        ModelHandle.init();

        BasicConditions.register();
        CompositeCondition.register();
        AdvancementCondition.register();

        // TODO: if (ModList.get.isLoaded("gamestages"))
            //GameStageCondition.register();
        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());

        // TODO: ClientCommandHandler.instance.registerCommand(new GbookCommand());
    }

    @Override
    public void registerBook(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }

    @Override
    public Collection<ResourceLocation> getBooksList()
    {
        return BookRegistry.getLoadedBooks().keySet();
    }

    @Override
    public void displayBook(String book)
    {
        ResourceLocation loc = new ResourceLocation(book);
        BookDocument br = BookRegistry.get(loc);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getInstance().displayGuiScreen(new GuiGuidebook(loc));
    }

    @Override
    public String getBookName(String book)
    {
        BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
        if (bookDocument != null)
        {
            String name = bookDocument.getName();
            if (name != null)
                return name;
        }
        return String.format("Guidebook - %s unknown", book);
    }
}
