package gigaherz.guidebook.guidebook.client;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        LOADED_BOOKS.clear();

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

        loadRawBookFiles(manager);

        for(ResourceLocation loc : toLoad)
        {
            if (!LOADED_BOOKS.containsKey(loc))
                LOADED_BOOKS.put(loc, parseBook(manager, loc));
        }
    }

    private static Type listType = new TypeToken<List<String>>() {}.getType();

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

    private static BookDocument parseBook(IResourceManager manager, ResourceLocation location, File file)
    {
        BookDocument bookDocument = new BookDocument(location);
        try
        {
            InputStream stream = new FileInputStream(file);
            bookDocument.parseBook(stream);
        }
        catch (IOException e)
        {
            bookDocument.initializeWithLoadError(e);
        }
        return bookDocument;
    }

    private static void loadRawBookFiles(IResourceManager manager)
    {
        File booksFolder = GuidebookMod.booksDirectory;

        if (!booksFolder.exists())
        {
            GuidebookMod.logger.info("The books folder does not exist, creating...");
            if (!booksFolder.mkdirs())
            {
                GuidebookMod.logger.info("The books folder could not be created. Books can't be loaded from it.");
                return;
            }
        }

        if (!booksFolder.exists() || !booksFolder.isDirectory())
        {
            GuidebookMod.logger.info("There's a file called books, but it's not a directory. Books can't be loaded from it.");
            return;
        }

        Collection<File> xmlFiles = FileUtils.listFiles(booksFolder, new String[]{"xml"}, true);

        for (File f : xmlFiles)
        {
            if (f.isFile())
            {
                ResourceLocation loc = new ResourceLocation(GuidebookMod.MODID, relativePath(booksFolder, f));

                if (!LOADED_BOOKS.containsKey(loc))
                    LOADED_BOOKS.put(loc, parseBook(manager, loc, f));
            }
        }
    }

    private static String relativePath(File base, File sub)
    {
        return base.toURI().relativize(sub.toURI()).getPath();
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

    private static Field _defaultResourcePacks = ReflectionHelper.findField(Minecraft.class, "field_110449_ao", "defaultResourcePacks");

    @SuppressWarnings("unchecked")
    public static void injectCustomResourcePack()
    {
        File resourcesFolder = new File(GuidebookMod.booksDirectory, "resources");

        if (!resourcesFolder.exists())
        {
            GuidebookMod.logger.info("The resources folder does not exist, creating...");
            if (!resourcesFolder.mkdirs())
            {
                GuidebookMod.logger.info("The resources folder could not be created, and it won't be injected as a resource pack folder.");
                return;
            }
        }

        if (!resourcesFolder.exists() || !resourcesFolder.isDirectory())
        {
            GuidebookMod.logger.info("There's a file called books, but it's not a directory, so it won't be injected as a resource pack folder.");
            return;
        }

        try
        {
            List<IResourcePack> rp = (List<IResourcePack>) _defaultResourcePacks.get(Minecraft.getMinecraft());

            rp.add(new FolderResourcePack(resourcesFolder)
            {
                String prefix = "assets/" + GuidebookMod.MODID + "/";

                @Override
                protected InputStream getInputStreamByName(String name) throws IOException
                {
                    if ("pack.mcmeta".equals(name))
                    {
                        return new ByteArrayInputStream(("{\"pack\":{\"description\": \"dummy\",\"pack_format\": 3}}").getBytes(Charsets.UTF_8));
                    }
                    if (!name.startsWith(prefix))
                        throw new FileNotFoundException(name);
                    return super.getInputStreamByName(name.substring(prefix.length()));
                }

                @Override
                protected boolean hasResourceName(String name)
                {
                    if ("pack.mcmeta".equals(name))
                        return true;
                    if (!name.startsWith(prefix))
                        return false;
                    return super.hasResourceName(name.substring(prefix.length()));
                }

                @Override
                public Set<String> getResourceDomains()
                {
                    return Collections.singleton(GuidebookMod.MODID);
                }
            });
        }
        catch (IllegalAccessException e)
        {
            // Ignore
        }
    }
}
