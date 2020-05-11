package gigaherz.guidebook.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.client.AnimatedBookBackground;
import gigaherz.guidebook.guidebook.client.BookBakedModel;
import gigaherz.guidebook.guidebook.client.SpecialBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents
{
    private static final ResourceLocation MODEL_HELPER = GuidebookMod.location("item/guidebook_helper");

    public static ItemStackTileEntityRenderer createBookItemRenderer()
    {
        return new BoolItemRenderer();
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GuidebookMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents
    {
        @SubscribeEvent
        public static void registerModels(FMLClientSetupEvent event)
        {
            ModelLoaderRegistry.registerLoader(GuidebookMod.location("special_model"), new SpecialBakedModel.ModelLoader());
            ModelLoaderRegistry.registerLoader(GuidebookMod.location("book_model"), new BookBakedModel.ModelLoader());

            // Ensures that the OBJ models used by the book GUI background, and all referenced textures, are loaded
            ModelLoader.addSpecialModel(AnimatedBookBackground.BOOK_BACKGROUND);

            ModelLoader.addSpecialModel(MODEL_HELPER);
        }
    }

    private static class BoolItemRenderer extends ItemStackTileEntityRenderer
    {
        private final List<Direction> sides = Arrays.stream(Direction.values()).collect(Collectors.toList());

        {
            sides.add(null);
        }

        @Override
        public void render(ItemStack stack, MatrixStack matrixStack, IRenderTypeBuffer buffers, int combinedLight, int combinedOverlay)
        {
            IBakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL_HELPER);
            IBakedModel bookModel = model.getOverrides().getModelWithOverrides(model, stack, null, null);
            if (bookModel == null)
                bookModel = model;

            boolean leftHand = (SpecialBakedModel.cameraTransformType == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND)
                    || (SpecialBakedModel.cameraTransformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND);

            matrixStack.push();
            if (SpecialBakedModel.cameraTransformType != null)
            {
                matrixStack.translate(0.5D, 0.5D, 0.5D);
                bookModel = ForgeHooksClient.handleCameraTransforms(matrixStack, bookModel, SpecialBakedModel.cameraTransformType, leftHand);
                matrixStack.translate(-0.5D, -0.5D, -0.5D);
                SpecialBakedModel.cameraTransformType = null;
            }

            IVertexBuilder buffer = buffers.getBuffer(CustomRenderTypes.ENTITY_TRANSLUCENT_UNSORTED_BLOCKATLAS);
            for(Direction side : sides)
            {
                Random rnd = new Random();
                rnd.setSeed(42);
                for (BakedQuad quad : bookModel.getQuads(null, side, rnd, EmptyModelData.INSTANCE))
                {
                    buffer.addQuad(matrixStack.getLast(), quad, 1.0f, 1.0f, 1.0f, combinedLight, combinedOverlay);
                }
            }

            matrixStack.pop();
        }
    }

    private static class CustomRenderTypes extends RenderType
    {
        private CustomRenderTypes(String p_i225992_1_, VertexFormat p_i225992_2_, int p_i225992_3_, int p_i225992_4_, boolean p_i225992_5_, boolean p_i225992_6_, Runnable p_i225992_7_, Runnable p_i225992_8_)
        {
            super(p_i225992_1_, p_i225992_2_, p_i225992_3_, p_i225992_4_, p_i225992_5_, p_i225992_6_, p_i225992_7_, p_i225992_8_);
        }

        public static RenderType entityTranslucentUnsorted(ResourceLocation texture, boolean doOverlay) {
            RenderType.State rendertype$state = RenderType.State.getBuilder()
                    .texture(new RenderState.TextureState(texture, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .diffuseLighting(DIFFUSE_LIGHTING_ENABLED)
                    .alpha(DEFAULT_ALPHA).cull(CULL_DISABLED)
                    .lightmap(LIGHTMAP_ENABLED)
                    .overlay(OVERLAY_ENABLED)
                    .build(doOverlay);
            return makeType("entity_translucent_unsorted", DefaultVertexFormats.ENTITY, 7, 256, true, false/*no sorting*/, rendertype$state);
        }

        public static RenderType entityTranslucentUnsorted(ResourceLocation texture) {
            return entityTranslucentUnsorted(texture, true);
        }

        @SuppressWarnings("deprecation")
        public static final RenderType ENTITY_TRANSLUCENT_UNSORTED_BLOCKATLAS = entityTranslucentUnsorted(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
    }
}
