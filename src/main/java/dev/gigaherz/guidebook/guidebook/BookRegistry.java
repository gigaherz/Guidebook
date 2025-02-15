package dev.gigaherz.guidebook.guidebook;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import dev.gigaherz.guidebook.GuidebookMod;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class BookRegistry
{
    private static final Logger LOGGER = LogUtils.getLogger();

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
        registerBook(ResourceLocation.fromNamespaceAndPath("gbook","xml/guidebook.xml"));
    }

    @Nullable
    public static BookDocument get(ResourceLocation loc)
    {
        return getLoadedBooks().get(loc);
    }

    @Nullable
    public static BookDocument get(ItemStack stack)
    {
        var loc = GuidebookItem.getBookLocation(stack);
        return loc == null ? null : get(loc);
    }

    public static void parseAllBooks(ResourceManager manager)
    {
        booksLoaded = true;

        LOADED_BOOKS.clear();

        Set<ResourceLocation> toLoad = Sets.newHashSet(REGISTRY);

        for (String domain : manager.getNamespaces())
        {
            try
            {
                List<Resource> resources = manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(domain, "books.json"));

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

        var lang = Minecraft.getInstance().getLanguageManager().getSelected();
        
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
        try (InputStream stream = resource.open())
        {
            List<String> yourList = new Gson().fromJson(new InputStreamReader(stream), listType);
            toLoad.addAll(yourList.stream().map(ResourceLocation::parse).collect(Collectors.toList()));
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
            ResourceLocation localizedLoc = ResourceLocation.fromNamespaceAndPath(domain, localizedPath);

            Resource bookResource;
            try
            {
                bookResource = manager.getResourceOrThrow(localizedLoc);
            }
            catch (IOException e)
            {
                bookResource = null;
            }

            if (bookResource == null)
            {
                bookResource = manager.getResourceOrThrow(bookLocation);
            }
            try (InputStream stream = bookResource.open())
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
                ResourceLocation loc = GuidebookMod.location(relativePath(booksFolder, f));

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

        Path resourcesFolder = mc.gameDirectory.toPath().resolve("config/books/resources");

        if (!Files.exists(resourcesFolder))
        {
            try
            {
                Files.createDirectories(resourcesFolder);
            }
            catch (IOException e)
            {
                return;
            }
        }

        if (!Files.exists(resourcesFolder) || !Files.isDirectory(resourcesFolder))
        {
            return;
        }

        final String id = "guidebook_config_folder_resources";
        final Component name = Component.literal("Guidebook Config Folder Resources");
        final String description = "For config/books/resources folder";
        try(final PackResources pack = new AbstractPackResources(new PackLocationInfo("special:guidebook_config_folder", name, PackSource.FEATURE, Optional.empty()))
        {
            final Path root = resourcesFolder;

            @org.jetbrains.annotations.Nullable
            @Override
            public IoSupplier<InputStream> getRootResource(String... paths)
            {
                if (paths.length == 1 && "pack.mcmeta".equals(paths[0]))
                {
                    //noinspection deprecation
                    return () -> new ByteArrayInputStream(("{\"pack\":{\"description\": \""+description+"\",\"pack_format\": "+ SharedConstants.RESOURCE_PACK_FORMAT +"}}").getBytes(StandardCharsets.UTF_8));
                }

                if (paths.length >= 2 && paths[0].equals("assets") && paths[1].equals(GuidebookMod.MODID))
                {
                    FileUtil.validatePath(paths);
                    var paths1 = Arrays.copyOfRange(paths, 2, paths.length);
                    Path path = FileUtil.resolvePath(this.root, List.of(paths1));
                    return Files.exists(path) ? IoSupplier.create(path) : null;
                }
                return null;
            }

            @org.jetbrains.annotations.Nullable
            @Override
            public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation resourceLocation)
            {
                if (packType == PackType.CLIENT_RESOURCES && resourceLocation.getNamespace().equals(GuidebookMod.MODID))
                {
                    var path = root.resolve(resourceLocation.getPath());
                    return Files.exists(path) ? IoSupplier.create(path) : null;
                }
                return null;
            }

            private static final Joiner PATH_JOINER = Joiner.on("/");
            @Override
            public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput)
            {
                if (type == PackType.CLIENT_RESOURCES && namespace.equals(GuidebookMod.MODID))
                {
                    try(var stream = Files.find(root.resolve(path), Integer.MAX_VALUE, (file, attributes) -> attributes.isRegularFile()))
                    {
                        stream.forEach(file -> {
                            var s = PATH_JOINER.join(root.relativize(file));
                            var rl = ResourceLocation.tryBuild(namespace, s);
                            if (rl != null)
                            {
                                resourceOutput.accept(rl, IoSupplier.create(file));
                            }
                        });
                    }
                    catch(FileNotFoundException | NoSuchFileException e)
                    {
                        // ignore
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public Set<String> getNamespaces(PackType type)
            {
                return Collections.singleton(GuidebookMod.MODID);
            }

            @Override
            public void close()
            {
            }
        })
        {
            Minecraft.getInstance().getResourcePackRepository().addPackFinder(infoConsumer -> infoConsumer.accept(
                    Pack.readMetaAndCreate(new PackLocationInfo(id, name, PackSource.BUILT_IN, Optional.empty()),
                            BuiltInPackSource.fixedResources(pack), PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.BOTTOM, true))
            ));
        }
    }

    public static ResourceLocation[] gatherStandaloneBookModels()
    {
        return getLoadedBooks().values().stream().map(BookDocument::getModelStandalone).filter(Objects::nonNull).distinct().toArray(ResourceLocation[]::new);
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
