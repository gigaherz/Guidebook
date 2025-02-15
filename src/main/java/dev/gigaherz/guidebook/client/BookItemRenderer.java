package dev.gigaherz.guidebook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import dev.gigaherz.guidebook.guidebook.client.BookBakedModel;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookItemRenderer implements SpecialModelRenderer<BookItemRenderer.BookModelData>
{
    static final ResourceLocation MODEL_HELPER = GuidebookMod.location("item/guidebook_helper");

    private final List<Direction> sides = Util.make(new ArrayList<>(), c -> {
        Collections.addAll(c, Direction.values());
        c.add(null);
    });

    @Override
    public void render(@Nullable BookModelData data, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                       MultiBufferSource multiBufferSource, int combinedLight, int combinedOverlay, boolean b)
    {
        BakedModel model = Minecraft.getInstance().getModelManager().getStandaloneModel(MODEL_HELPER);
        BakedModel bookModel = model instanceof BookBakedModel bbm ? bbm.getActualModel(data.location()) : model;
        if (bookModel == null)
            bookModel = model;

        boolean leftHand = (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                || (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        model.applyTransform(itemDisplayContext, poseStack, leftHand);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        VertexConsumer buffer = multiBufferSource.getBuffer(NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
        RandomSource rnd = RandomSource.create();
        for (Direction side : sides)
        {
            rnd.setSeed(42);
            for (BakedQuad quad : bookModel.getQuads(null, side, rnd, ModelData.EMPTY, null))
            {
                buffer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, combinedLight, combinedOverlay);
            }
        }

        poseStack.popPose();
    }

    @Nullable
    @Override
    public BookModelData extractArgument(ItemStack itemStack)
    {
        return new BookModelData(GuidebookItem.getBookLocation(itemStack));
    }

    public record BookModelData(@Nullable ResourceLocation location)
    {
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked
    {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        private Unbaked(){}

        @Nullable
        @Override
        public SpecialModelRenderer<?> bake(EntityModelSet modelSet)
        {
            return new BookItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type()
        {
            return CODEC;
        }
    }
}
