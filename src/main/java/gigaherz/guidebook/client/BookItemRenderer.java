package gigaherz.guidebook.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.client.SpecialBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

class BookItemRenderer extends ItemStackTileEntityRenderer
{
    static final ResourceLocation MODEL_HELPER = GuidebookMod.location("item/guidebook_helper");

    private final List<Direction> sides = Arrays.stream(Direction.values()).collect(Collectors.toList());

    {
        sides.add(null);
    }

    @Override
    public void func_239207_a_(ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack matrixStack, IRenderTypeBuffer buffers, int combinedLight, int combinedOverlay)
    {
        IBakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL_HELPER);
        IBakedModel bookModel = model.getOverrides().getOverrideModel(model, stack, null, null);
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

        IVertexBuilder buffer = buffers.getBuffer(ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
        for (Direction side : sides)
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
