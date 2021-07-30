package dev.gigaherz.guidebook.guidebook;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.templates.TemplateLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
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
        if (booksLoaded)
            throw new IllegalStateException("Books must be registered before resource loading start, preferably in the BookRegistryEvent.");
        REGISTRY.add(loc);
    }

    static
    {
        registerBook(new ResourceLocation("gbook:xml/guidebook.xml"));
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

    public static void parseAllBooks(ResourceManager manager)
    {
        booksLoaded = true;

        TemplateLibrary.clear();

        LOADED_BOOKS.clear();

        Set<ResourceLocation> toLoad = Sets.newHashSet(REGISTRY);

        for (String domain : manager.getNamespaces())
        {
            try
            {
                List<Resource> resources = manager.getResources(new ResourceLocation(domain, "books.json"));

                for (Resource res : resources)
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

        String lang = ObfuscationReflectionHelper.getPrivateValue(LanguageManager.class, lm, "currentCode");
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

    private static void loadBooksData(Set<ResourceLocation> toLoad, Resource resource) throws IOException
    {
        try (InputStream stream = resource.getInputStream())
        {
            List<String> yourList = new Gson().fromJson(new InputStreamReader(stream), listType);
            toLoad.addAll(yourList.stream().map(ResourceLocation::new).collect(Collectors.toList()));
        }
    }

    @Nullable
    private static BookDocument parseBook(ResourceManager manager, ResourceLocation location, String lang)
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

            Resource bookResource;
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
        catch (Exception e)
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
        File booksFolder = new File(new File(Minecraft.getInstance().gameDirectory, "config"), "books");

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

    @SuppressWarnings("ConstantConditions")
    public static void injectCustomResourcePack()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc == null)
            return;

        File resourcesFolder = new File(new File(new File(mc.gameDirectory, "config"), "books"), "resources");

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

        final String id = "guidebook_config_folder_resources";
        final Component name = new TextComponent("Guidebook Config Folder Virtual Resource Pack");
        final Component description = new TextComponent("Provides book resources placed in the config/books/resources folder");
        final PackResources pack = new FolderPackResources(resourcesFolder)
        {
            String prefix = "assets/" + GuidebookMod.MODID + "/";

            @Override
            protected InputStream getResource(String name) throws IOException
            {
                if ("pack.mcmeta".equals(name))
                {
                    return new ByteArrayInputStream(("{\"pack\":{\"description\": \"dummy\",\"pack_format\": 5}}").getBytes(StandardCharsets.UTF_8));
                }
                if (!name.startsWith(prefix))
                    throw new FileNotFoundException(name);
                return super.getResource(name.substring(prefix.length()));
            }

            @Override
            protected boolean hasResource(String name)
            {
                if ("pack.mcmeta".equals(name))
                    return true;
                if (!name.startsWith(prefix))
                    return false;
                return super.hasResource(name.substring(prefix.length()));
            }

            @Override
            public Set<String> getNamespaces(PackType type)
            {
                return Collections.singleton(GuidebookMod.MODID);
            }
        };

        Minecraft.getInstance().getResourcePackRepository().addPackFinder(new RepositorySource()
        {
            @Override
            public void loadPacks(Consumer<Pack> infoConsumer, Pack.PackConstructor infoFactory)
            {
                infoConsumer.accept(
                        new Pack(id, true, () -> pack,
                                name, description, PackCompatibility.COMPATIBLE, Pack.Position.BOTTOM,
                                true, text -> text, true)
                );
            }
        });
    }

    public static ResourceLocation[] gatherBookModels()
    {
        return getLoadedBooks().values().stream().map(BookDocument::getModel).filter(Objects::nonNull).distinct().toArray(ResourceLocation[]::new);
    }

    public static ResourceLocation[] gatherBookCovers()
    {
        return getLoadedBooks().values().stream().map(BookDocument::getCover).filter(Objects::nonNull).distinct().toArray(ResourceLocation[]::new);
    }

    public static void initClientResourceListener(ReloadableResourceManager clientResourceManager)
    {
        clientResourceManager.registerReloadListener((ResourceManagerReloadListener) ((resourceManager) -> {
            booksLoaded = false;
        }));
    }

    public static Collection<ResourceLocation> getBooksList()
    {
        return getLoadedBooks().keySet();
    }
}
