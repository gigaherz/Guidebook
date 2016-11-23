package gigaherz.guidebook.client;

import com.google.common.base.Charsets;
import gigaherz.common.client.ModelHandle;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import gigaherz.guidebook.guidebook.client.BookRegistry;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy implements IModProxy
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        ModelLoaderRegistry.registerLoader(new BookBakedModel.ModelLoader());

        registerItemModel(GuidebookMod.guidebook);
    }

    @Override
    public void preInit(File modConfigurationDirectory)
    {
        injectCustomResourcePack(modConfigurationDirectory);

        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());
    }

    private static Field _defaultResourcePacks = ReflectionHelper.findField(Minecraft.class, "field_110449_ao", "defaultResourcePacks");

    @SuppressWarnings("unchecked")
    private void injectCustomResourcePack(File modConfigurationDirectory)
    {
        File booksFolder = new File(modConfigurationDirectory, "books");

        if (!booksFolder.exists() || !booksFolder.isDirectory())
            return;

        try
        {
            List<IResourcePack> rp = (List<IResourcePack>) _defaultResourcePacks.get(Minecraft.getMinecraft());

            rp.add(new FolderResourcePack(booksFolder)
            {
                String prefix = "assets/" + GuidebookMod.MODID + "/";

                @Override
                protected InputStream getInputStreamByName(String name) throws IOException
                {
                    if ("pack.mcmeta".equals(name))
                    {
                        return new ByteArrayInputStream(("{\"pack\":{\"description\": \"dummy\",\"pack_format\": 2}}").getBytes(Charsets.UTF_8));
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

    @Override
    public void init()
    {
        ModelHandle.init();

        BookRegistry.initReloadHandler();
    }

    @Override
    public void displayBook(String book)
    {
        ResourceLocation loc = new ResourceLocation(book);
        BookDocument br = BookRegistry.get(loc);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getMinecraft().displayGuiScreen(new GuiGuidebook(loc));
    }
}
