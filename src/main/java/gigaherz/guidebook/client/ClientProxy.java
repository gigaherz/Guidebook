package gigaherz.guidebook.client;

import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import gigaherz.guidebook.guidebook.conditions.AdvancementCondition;
import gigaherz.guidebook.guidebook.conditions.BasicConditions;
import gigaherz.guidebook.guidebook.conditions.CompositeCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;

public class ClientProxy implements IModProxy
{
    public ClientProxy()
    {
        //BookRegistry.injectCustomResourcePack();
    }

    public static void initialize()
    {
        BasicConditions.register();
        CompositeCondition.register();
        AdvancementCondition.register();

        // TODO: if (ModList.get.isLoaded("gamestages"))
        //GameStageCondition.register();
        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());

        // TODO: ClientCommandHandler.instance.registerCommand(new GbookCommand());

        BookRegistry.initClientResourceListener((IReloadableResourceManager) Minecraft.getInstance().getResourceManager());
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
        return "Missing Book";
    }
}
