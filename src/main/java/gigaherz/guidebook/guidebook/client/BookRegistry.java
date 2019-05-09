package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class BookRegistry
{
    public static final Set<ResourceLocation> REGISTRY = Sets.newHashSet();

    private static boolean booksLoaded = false;
    private static final Map<ResourceLocation, BookDocument> LOADED_BOOKS = Maps.newHashMap();

    public static Map<ResourceLocation, BookDocument> getLoadedBooks()
    {
        if (!booksLoaded)
        {
            parseAllBooks(Minecraft.getInstance().getResourceManager());
        }
        return Collections.unmodifiableMap(LOADED_BOOKS);
    }

    public static void registerBook(ResourceLocation loc)
    {
        // TODO
        //if (Loader.instance().hasReachedState(INITIALIZATION))
        //    throw new IllegalStateException("Books must be registered before init, preferably in the BookRegistryEvent.");
        REGISTRY.add(loc);
    }

    @Nullable
    public static BookDocument get(ResourceLocation loc)
    {
        return getLoadedBooks().get(loc);
    }

    @Nullable
    public static BookDocument get(ItemStack stack)
    {
        String loc = GuidebookMod.guidebook.getBookLocation(stack);
        return loc == null ? null : get(new ResourceLocation(loc));
    }

    private static boolean initialized = false;

    public static void parseAllBooks(IResourceManager manager)
    {
        booksLoaded = true;

        TemplateLibrary.clear();

        LOADED_BOOKS.clear();

        Set<ResourceLocation> toLoad = Sets.newHashSet(REGISTRY);

        for (String domain : manager.getResourceNamespaces())
        {
            try
            {
                List<IResource> resources = manager.getAllResources(new ResourceLocation(domain, "books.json"));

                for (IResource res : resources)
                {
                    loadBooksData(toLoad, res);
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

        loadRawBookFiles();

        LanguageManager lm = Minecraft.getInstance().getLanguageManager();

        String lang = ObfuscationReflectionHelper.getPrivateValue(LanguageManager.class, lm, "field_135048_c");
        if (lang == null) lang = "en_us";
        for (ResourceLocation loc : toLoad)
        {
            if (!LOADED_BOOKS.containsKey(loc))
            {
                BookDocument book = parseBook(manager, loc, lang);
                if (book != null)
                    LOADED_BOOKS.put(loc, book);
            }
        }
    }

    private static Type listType = new TypeToken<List<String>>()
    {
    }.getType();

    private static void loadBooksData(Set<ResourceLocation> toLoad, IResource resource) throws IOException
    {
        try (InputStream stream = resource.getInputStream())
        {
            List<String> yourList = new Gson().fromJson(new InputStreamReader(stream), listType);
            toLoad.addAll(yourList.stream().map(ResourceLocation::new).collect(Collectors.toList()));
        }
    }

    @Nullable
    private static BookDocument parseBook(IResourceManager manager, ResourceLocation location, String lang)
    {
        BookDocument bookDocument = new BookDocument(location);
        try
        {
            ResourceLocation bookLocation = bookDocument.getLocation();
            String domain = bookLocation.getNamespace();
            String path = bookLocation.getPath();
            String pathWithoutExtension = path;
            String extension = "";
            int ext = path.lastIndexOf('.');
            if (ext >= 0)
            {
                pathWithoutExtension = path.substring(0, ext);
                extension = path.substring(ext);
            }

            String localizedPath = pathWithoutExtension + "." + lang + extension;
            ResourceLocation localizedLoc = new ResourceLocation(domain, localizedPath);

            IResource bookResource;
            try
            {
                bookResource = manager.getResource(localizedLoc);
            }
            catch (IOException e)
            {
                bookResource = null;
            }

            if (bookResource == null)
            {
                bookResource = manager.getResource(bookLocation);
            }
            try (InputStream stream = bookResource.getInputStream())
            {
                if (!bookDocument.parseBook(stream, false))
                    return null;
            }
        }
        catch (IOException e)
        {
            bookDocument.initializeWithLoadError(e.toString());
        }
        return bookDocument;
    }

    @Nullable
    private static BookDocument parseBook(ResourceLocation location, File file)
    {
        BookDocument bookDocument = new BookDocument(location);
        try
        {
            InputStream stream = new FileInputStream(file);
            if (!bookDocument.parseBook(stream, true))
                return null;
        }
        catch (IOException e)
        {
            bookDocument.initializeWithLoadError(e.toString());
        }
        return bookDocument;
    }

    private static void loadRawBookFiles()
    {
        File booksFolder = getBooksFolder();
        if (booksFolder == null)
            return;

        Collection<File> xmlFiles = FileUtils.listFiles(booksFolder, new String[]{"xml"}, true);

        for (File f : xmlFiles)
        {
            if (f.isFile())
            {
                ResourceLocation loc = new ResourceLocation(GuidebookMod.MODID, relativePath(booksFolder, f));

                if (!LOADED_BOOKS.containsKey(loc))
                {
                    BookDocument book = parseBook(loc, f);
                    if (book != null)
                        LOADED_BOOKS.put(loc, book);
                }
            }
        }
    }

    @Nullable
    public static File getBooksFolder()
    {
        File booksFolder = new File(new File(Minecraft.getInstance().gameDir, "config"), "books");

        if (!booksFolder.exists())
        {
            GuidebookMod.logger.info("The books folder does not exist, creating...");
            if (!booksFolder.mkdirs())
            {
                GuidebookMod.logger.info("The books folder could not be created. Books can't be loaded from it.");
                return null;
            }
        }

        if (!booksFolder.exists() || !booksFolder.isDirectory())
        {
            GuidebookMod.logger.info("There's a file called books, but it's not a directory. Books can't be loaded from it.");
            return null;
        }

        return booksFolder;
    }

    private static String relativePath(File base, File sub)
    {
        return base.toURI().relativize(sub.toURI()).getPath();
    }

    //private static Field _defaultResourcePacks = ObfuscationReflectionHelper.findField(Minecraft.class, "field_110449_ao");

    @SuppressWarnings("unchecked")
    public static void injectCustomResourcePack()
    {
        if (initialized) return;
        initialized = true;
/*
        File resourcesFolder = new File(new File(new File(Minecraft.getInstance().gameDir, "config"), "books"), "resources");

        if (!resourcesFolder.exists())
        {
            if (!resourcesFolder.mkdirs())
            {
                return;
            }
        }

        if (!resourcesFolder.exists() || !resourcesFolder.isDirectory())
        {
            return;
        }

        try
        {
            List<IResourcePack> rp = (List<IResourcePack>) _defaultResourcePacks.get(Minecraft.getInstance());

            Minecraft.getInstance().getResourcePackList().addPackFinder(new IPackFinder()
            {
                @Override
                public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory)
                {

                }
            });

            rp.add(new FolderPack(resourcesFolder)
            {
                String prefix = "assets/" + GuidebookMod.MODID + "/";

                @Override
                protected InputStream getInputStream(String name) throws IOException
                {
                    if ("pack.mcmeta".equals(name))
                    {
                        return new ByteArrayInputStream(("{\"pack\":{\"description\": \"dummy\",\"pack_format\": 4}}").getBytes(Charsets.UTF_8));
                    }
                    if (!name.startsWith(prefix))
                        throw new FileNotFoundException(name);
                    return super.getInputStream(name.substring(prefix.length()));
                }

                @Override
                protected boolean resourceExists(String name)
                {
                    if ("pack.mcmeta".equals(name))
                        return true;
                    if (!name.startsWith(prefix))
                        return false;
                    return super.resourceExists(name.substring(prefix.length()));
                }

                @Override
                public Set<String> getResourceNamespaces(ResourcePackType type)
                {
                    return Collections.singleton(GuidebookMod.MODID);
                }
            });
        }
        catch (IllegalAccessException e)
        {
            // Ignore
        }
        */
    }

    public static ResourceLocation[] gatherBookModels()
    {
        return LOADED_BOOKS.values().stream().map(BookDocument::getModel).filter(Objects::nonNull).distinct().toArray(ResourceLocation[]::new);
    }
}
