package dev.gigaherz.guidebook.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.client.AnimatedBookBackground;
import dev.gigaherz.guidebook.guidebook.client.BookBakedModel;
import dev.gigaherz.guidebook.guidebook.conditions.AdvancementCondition;
import dev.gigaherz.guidebook.guidebook.conditions.BasicConditions;
import dev.gigaherz.guidebook.guidebook.conditions.CompositeCondition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.function.Function;

public class ClientHandlers
{
    public static void clientInit()
    {
        BasicConditions.register();
        CompositeCondition.register();
        AdvancementCondition.register();
        /*if (ModList.get().isLoaded("gamestages"))
            GameStageCondition.register();*/

        BookRegistry.initClientResourceListener((ReloadableResourceManager) Minecraft.getInstance().getResourceManager());
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents
    {
        @SubscribeEvent
        public static void construct(FMLConstructModEvent event)
        {
            BookRegistry.injectCustomResourcePack();
        }

        @SubscribeEvent
        public static void clientInit(RegisterParticleProvidersEvent event)
        {
            ClientHandlers.clientInit();
        }

        @SubscribeEvent
        public static void modelRegistry(ModelEvent.RegisterGeometryLoaders event)
        {
            event.register(GuidebookMod.location("book_model"), new BookBakedModel.ModelLoader());
        }

        @SubscribeEvent
        public static void specialModels(ModelEvent.RegisterAdditional event)
        {
            // Ensures that the OBJ models used by the book GUI background, and all referenced textures, are loaded
            event.register(AnimatedBookBackground.BOOK_BACKGROUND0);
            event.register(AnimatedBookBackground.BOOK_BACKGROUND30);
            event.register(AnimatedBookBackground.BOOK_BACKGROUND60);
            event.register(AnimatedBookBackground.BOOK_BACKGROUND90);
            event.register(BookItemRenderer.MODEL_HELPER);
        }

        @SubscribeEvent
        public static void shaderRegistry(RegisterShadersEvent event) throws IOException
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation("gbook:rendertype_bright_solid"), DefaultVertexFormat.NEW_ENTITY), shaderInstance -> {
                CustomRenderTypes.brightSolidShader = shaderInstance;
            });
        }
    }

    public static RenderType brightSolid(ResourceLocation texture)
    {
        return CustomRenderTypes.BRIGHT_SOLID.apply(texture);
    }

    private static class CustomRenderTypes extends RenderType
    {
        private static ShaderInstance brightSolidShader;

        private static final ShaderStateShard RENDERTYPE_BRIGHT_SOLID_SHADER = new ShaderStateShard(() -> brightSolidShader);

        private CustomRenderTypes(String s, VertexFormat v, VertexFormat.Mode m, int i, boolean b, boolean b2, Runnable r, Runnable r2)
        {
            super(s, v, m, i, b, b2, r, r2);
            throw new IllegalStateException("This class is not meant to be constructed!");
        }

        public static Function<ResourceLocation, RenderType> BRIGHT_SOLID = Util.memoize(CustomRenderTypes::brightSolid);

        private static RenderType brightSolid(ResourceLocation locationIn)
        {
            RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_BRIGHT_SOLID_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .createCompositeState(true);
            return create("gbook_bright_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, rendertype$state);
        }
    }
}
