package gigaherz.guidebook.client;

import gigaherz.common.client.ModelHandle;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collection;

import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy implements IModProxy
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        BookRegistry.injectCustomResourcePack();

        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        ModelLoaderRegistry.registerLoader(new BookBakedModel.ModelLoader());

        registerItemModel(GuidebookMod.guidebook);
    }

    @Override
    public void preInit()
    {
        ModelHandle.init();

        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());
    }

    @Override
    public Collection<ResourceLocation> getBooksList()
    {
        return BookRegistry.LOADED_BOOKS.keySet();
    }

    @Override
    public void displayBook(String book)
    {
        ResourceLocation loc = new ResourceLocation(book);
        BookDocument br = BookRegistry.get(loc);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getMinecraft().displayGuiScreen(new GuiGuidebook(loc));
    }

    @Override
    public String getBookName(String book)
    {
        BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
        if (bookDocument != null)
        {
            String name = bookDocument.getBookName();
            if (name != null)
                return name;
        }
        return String.format("Guidebook - %s unknown", book);
    }
}
