package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Maps;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static net.minecraftforge.fml.common.LoaderState.INITIALIZATION;

public class BookRegistry
{
    public static final Map<ResourceLocation, BookDocument> REGISTRY = Maps.newHashMap();

    public static void registerBook(ResourceLocation loc)
    {
        if (Loader.instance().hasReachedState(INITIALIZATION))
            throw new IllegalStateException("Books must be registered before init, preferably in the BookRegistryEvent.");
        if (REGISTRY.containsKey(loc))
            throw new KeyAlreadyExistsException("A book with this id has already been registered.");
        BookDocument book = new BookDocument(loc);
        REGISTRY.put(loc, book);
    }

    @Nullable
    public static BookDocument get(ResourceLocation loc)
    {
        return REGISTRY.get(loc);
    }

    public static void parseAllBooks()
    {
        TemplateLibrary.reload();
        REGISTRY.values().forEach(BookRegistry::parseBook);
    }

    private static void parseBook(BookDocument bookDocument)
    {
        try
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(bookDocument.getBookLocation());
            InputStream stream = res.getInputStream();
            bookDocument.parseBook(stream);
        }
        catch (IOException e)
        {
            bookDocument.initializeWithLoadError(e);
        }
    }

    public static void initReloadHandler()
    {
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager)
        {
            ((IReloadableResourceManager) rm).registerReloadListener(__ -> parseAllBooks());
        }
    }

}
