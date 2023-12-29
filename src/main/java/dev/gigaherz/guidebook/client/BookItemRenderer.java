package dev.gigaherz.guidebook.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.gigaherz.guidebook.GuidebookMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookItemRenderer extends BlockEntityWithoutLevelRenderer
{
    static final ResourceLocation MODEL_HELPER = GuidebookMod.location("item/guidebook_helper");

    private final List<Direction> sides = Util.make(new ArrayList<>(), c -> {
        Collections.addAll(c, Direction.values());
        c.add(null);
    });

    public BookItemRenderer(BlockEntityRenderDispatcher p_172550_, EntityModelSet p_172551_)
    {
        super(p_172550_, p_172551_);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource buffers, int combinedLight, int combinedOverlay)
    {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL_HELPER);
        BakedModel bookModel = model.getOverrides().resolve(model, stack, null, null, 0);
        if (bookModel == null)
            bookModel = model;

        boolean leftHand = (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                || (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);

        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        bookModel = ClientHooks.handleCameraTransforms(matrixStack, bookModel, transformType, leftHand);
        matrixStack.translate(-0.5D, -0.5D, -0.5D);

        VertexConsumer buffer = buffers.getBuffer(NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
        RandomSource rnd = RandomSource.create();
        for (Direction side : sides)
        {
            rnd.setSeed(42);
            for (BakedQuad quad : bookModel.getQuads(null, side, rnd, ModelData.EMPTY, null))
            {
                buffer.putBulkData(matrixStack.last(), quad, 1.0f, 1.0f, 1.0f, combinedLight, combinedOverlay);
            }
        }

        matrixStack.popPose();
    }
}
