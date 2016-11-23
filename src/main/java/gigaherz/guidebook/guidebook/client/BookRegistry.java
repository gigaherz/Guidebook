package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraftforge.fml.common.LoaderState.INITIALIZATION;

public class BookRegistry
{
    public static final Set<ResourceLocation> REGISTRY = Sets.newHashSet();
    public static final Map<ResourceLocation, BookDocument> LOADED_BOOKS = Maps.newHashMap();

    public static void registerBook(ResourceLocation loc)
    {
        if (Loader.instance().hasReachedState(INITIALIZATION))
            throw new IllegalStateException("Books must be registered before init, preferably in the BookRegistryEvent.");
        REGISTRY.add(loc);
    }

    @Nullable
    public static BookDocument get(ResourceLocation loc)
    {
        return LOADED_BOOKS.get(loc);
    }

    public static void parseAllBooks(IResourceManager manager)
    {
        TemplateLibrary.clear();

        Set<ResourceLocation> toLoad = Sets.newHashSet(REGISTRY);

        for (String domain : manager.getResourceDomains())
        {
            try
            {
                List<IResource> resources = manager.getAllResources(new ResourceLocation(domain, "books.json"));

                for (IResource res : resources)
                {
                    loadBooksData(toLoad, res.getInputStream());
                }
            }
            catch (FileNotFoundException e)
            {
                // IGNORE, it just means nothing was found
            }
            catch (IOException e)
            {
                GuidebookMod.logger.error("Error loading books from resourcepacks", e);
            }
        }

        LOADED_BOOKS.putAll(Maps.asMap(toLoad, b -> parseBook(manager, b)));
    }

    private static Type listType = new TypeToken<List<String>>()
    {
    }.getType();

    private static void loadBooksData(Set<ResourceLocation> toLoad, InputStream stream)
    {
        List<String> yourList = new Gson().fromJson(new InputStreamReader(stream), listType);
        toLoad.addAll(yourList.stream().map(ResourceLocation::new).collect(Collectors.toList()));
    }

    private static BookDocument parseBook(IResourceManager manager, ResourceLocation location)
    {
        BookDocument bookDocument = new BookDocument(location);
        try
        {
            IResource res = manager.getResource(bookDocument.getBookLocation());
            InputStream stream = res.getInputStream();
            bookDocument.parseBook(stream);
        }
        catch (IOException e)
        {
            bookDocument.initializeWithLoadError(e);
        }
        return bookDocument;
    }

    private static boolean initialized = false;

    public static void initReloadHandler()
    {
        if (initialized)
            return;

        initialized = true;

        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager)
        {
            ((IReloadableResourceManager) rm).registerReloadListener(BookRegistry::parseAllBooks);
        }
    }
}
