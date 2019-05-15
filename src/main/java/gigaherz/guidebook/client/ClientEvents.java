package gigaherz.guidebook.client;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus= Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents
{
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        ModelLoaderRegistry.registerLoader(new BookBakedModel.ModelLoader());

        //registerItemModel(GuidebookMod.guidebook);

        //ModelLoader.registerItemVariants(GuidebookMod.guidebook, BookRegistry.gatherBookModels());
    }

    private static final Set<ResourceLocation> textures = Sets.newHashSet();

    @SubscribeEvent
    public static void textureStitch(TextureStitchEvent.Pre ev)
    {
        IResourceManager mgr = Minecraft.getInstance().getResourceManager();
        for(ResourceLocation loc : textures)
        {
            ev.getMap().registerSprite(mgr, loc);
        }
    }

    @SubscribeEvent
    public static void modelBake(ModelBakeEvent ev)
    {
        ModelLoader ldr = ev.getModelLoader();

        Set<String> dummy = Sets.newHashSet();
        for(ModelResourceLocation loc : BookRegistry.gatherBookModels())
        {
            IUnbakedModel mdl = ldr.getUnbakedModel(loc);
            textures.addAll(mdl.getTextures(ModelLoader.defaultModelGetter(), dummy));
            IBakedModel bmdl = mdl.bake(ModelLoader.defaultModelGetter(), ModelLoader.defaultTextureGetter(), mdl.getDefaultState(), false, DefaultVertexFormats.ITEM);
            ev.getModelRegistry().put(loc, bmdl);
        }
    }
}
