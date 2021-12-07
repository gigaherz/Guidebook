package dev.gigaherz.guidebook.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.client.AnimatedBookBackground;
import dev.gigaherz.guidebook.guidebook.client.BookBakedModel;
import dev.gigaherz.guidebook.guidebook.client.SpecialBakedModel;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

import java.io.IOException;
import java.util.function.Function;

public class ClientHandlers
{
    public static void clientInit()
    {
        BasicConditions.register();
        CompositeCondition.register();
        AdvancementCondition.register();

        MinecraftForge.EVENT_BUS.post(new BookRegistryEvent());

        // TODO: ClientCommandHandler.instance.registerCommand(new GbookCommand());

        BookRegistry.initClientResourceListener((ReloadableResourceManager) Minecraft.getInstance().getResourceManager());
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents
    {
        @SubscribeEvent
        public static void construct(FMLConstructModEvent event)
        {
            BookRegistry.injectCustomResourcePack();
        }

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
            ForgeModelBakery.addSpecialModel(AnimatedBookBackground.BOOK_BACKGROUND);
            ForgeModelBakery.addSpecialModel(BookItemRenderer.MODEL_HELPER);
        }

        @SubscribeEvent
        public static void shaderRegistry(RegisterShadersEvent event) throws IOException
        {
            event.registerShader(new ShaderInstance(event.getResourceManager(), new ResourceLocation("gbook:rendertype_bright_solid"), DefaultVertexFormat.NEW_ENTITY), shaderInstance -> {
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
