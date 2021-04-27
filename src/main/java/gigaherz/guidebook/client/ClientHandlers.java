package gigaherz.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.client.AnimatedBookBackground;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import gigaherz.guidebook.guidebook.client.SpecialBakedModel;
import gigaherz.guidebook.guidebook.conditions.AdvancementCondition;
import gigaherz.guidebook.guidebook.conditions.BasicConditions;
import gigaherz.guidebook.guidebook.conditions.CompositeCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientHandlers
{
    public static void clientInit()
    {
        BasicConditions.register();
        CompositeCondition.register();
        AdvancementCondition.register();

        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());

        // TODO: ClientCommandHandler.instance.registerCommand(new GbookCommand());

        BookRegistry.initClientResourceListener((IReloadableResourceManager) Minecraft.getInstance().getResourceManager());
    }

    public static ItemStackTileEntityRenderer createBookItemRenderer()
    {
        return new BookItemRenderer();
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents
    {
        @SubscribeEvent
        public static void clientInit(ParticleFactoryRegisterEvent event)
        {
            ClientHandlers.clientInit();
        }

        @SubscribeEvent
        public static void modelRegistry(ModelRegistryEvent event)
        {
            ModelLoaderRegistry.registerLoader(GuidebookMod.location("special_model"), new SpecialBakedModel.ModelLoader());
            ModelLoaderRegistry.registerLoader(GuidebookMod.location("book_model"), new BookBakedModel.ModelLoader());

            // Ensures that the OBJ models used by the book GUI background, and all referenced textures, are loaded
            ModelLoader.addSpecialModel(AnimatedBookBackground.BOOK_BACKGROUND);
            ModelLoader.addSpecialModel(BookItemRenderer.MODEL_HELPER);
        }
    }
}
