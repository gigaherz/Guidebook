package gigaherz.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.client.AnimatedBookBackground;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus= Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents
{
    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus= Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents
    {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event)
        {
            ModelLoaderRegistry.registerLoader(GuidebookMod.location("book_model"), new BookBakedModel.ModelLoader());

            // Ensures that the OBJ models used by the book GUI background, and all referenced textures, are loaded
            ModelLoader.addSpecialModel(AnimatedBookBackground.BOOK_BACKGROUND);
        }
    }
}
