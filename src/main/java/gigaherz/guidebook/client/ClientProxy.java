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

@Deprecated
public class ClientProxy implements IModProxy
{
    public ClientProxy()
    {
        //BookRegistry.injectCustomResourcePack();
    }

    @Deprecated
    public static void initialize()
    {
    }

    @Deprecated
    @Override
    public void registerBook(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }

    @Override
    public Collection<ResourceLocation> getBooksList()
    {
        return BookRegistry.getBooksList();
    }

    @Override
    public void displayBook(String book)
    {
        ClientAPI.displayBook(book);
    }

    @Override
    public String getBookName(String book)
    {
        return ClientAPI.getBookName(book);
    }
}
