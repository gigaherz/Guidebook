package dev.gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.client.ClientHandlers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

public class AnimatedBookBackground implements IAnimatedBookBackground
{
    public static final ResourceLocation BOOK_BACKGROUND0 = GuidebookMod.location("gui/animated_book0");
    public static final ResourceLocation BOOK_BACKGROUND30 = GuidebookMod.location("gui/animated_book30");
    public static final ResourceLocation BOOK_BACKGROUND60 = GuidebookMod.location("gui/animated_book60");
    public static final ResourceLocation BOOK_BACKGROUND90 = GuidebookMod.location("gui/animated_book90");

    public static final RandomSource RANDOM = RandomSource.create();

    private static final int ANIMATE_TICKS = 8;
    private static final float ANIMATE_ANGLE = 90;
    private static final float ANGLE_PER_TICK = ANIMATE_ANGLE / ANIMATE_TICKS;
    private float progress = ANIMATE_TICKS;

    private boolean closing = false;

    private final GuidebookScreen gui;

    public AnimatedBookBackground(GuidebookScreen gui)
    {
        this.gui = gui;
    }

    @Override
    public void startClosing()
    {
        closing = true;
    }

    @Override
    public boolean isFullyOpen()
    {
        return progress <= 0;
    }

    @Override
    public boolean update()
    {
        if (closing)
        {
            return progress >= ANIMATE_TICKS;
        }
        else if (progress < 0)
        {
            progress = 0;
        }
        return false;
    }

    @Override
    public void draw(GuiGraphics graphics, float partialTicks, int bookHeight, float scalingFactor)
    {
        BakedModel modelBookA, modelBookB;

        if (closing)
            progress += partialTicks;
        else
            progress -= partialTicks;

        float angleX = progress * ANGLE_PER_TICK;

        BakedModel book00 = Minecraft.getInstance().getModelManager().getStandaloneModel(BOOK_BACKGROUND0);
        BakedModel book30 = Minecraft.getInstance().getModelManager().getStandaloneModel(BOOK_BACKGROUND30);
        BakedModel book60 = Minecraft.getInstance().getModelManager().getStandaloneModel(BOOK_BACKGROUND60);
        BakedModel book90 = Minecraft.getInstance().getModelManager().getStandaloneModel(BOOK_BACKGROUND90);

        float blend;
        if (angleX <= 0)
        {
            angleX = 0;
            modelBookA = book00;
            modelBookB = null;
            blend = 0;
        }
        else if (angleX < 30)
        {
            modelBookA = book00;
            modelBookB = book30;
            blend = (angleX) / 30.0f;
        }
        else if (angleX < 60)
        {
            modelBookA = book30;
            modelBookB = book60;
            blend = (angleX - 30) / 30.0f;
        }
        else if (angleX < 90)
        {
            modelBookA = book60;
            modelBookB = book90;
            blend = (angleX - 60) / 30.0f;
        }
        else
        {
            angleX = 90;
            modelBookA = book90;
            modelBookB = null;
            blend = 0;
        }

        graphics.flush();

        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        Lighting.setupForEntityInInventory();

        var pose = graphics.pose();
        pose.pushPose();
        {
            pose.translate(gui.width * 0.5 * (1 + angleX / 130.0f), gui.height * 0.5 * (1 + angleX / 110.0f) + bookHeight * 0.53, -200);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.XP.rotationDegrees(-130));
            float scaleValue = 2.6f;//2.16f;
            pose.scale(2.46f * scalingFactor, 2.0f * scalingFactor, 2.9f * scalingFactor);

            pose.mulPose(Axis.ZP.rotationDegrees(angleX * 1.1f));

            MultiBufferSource.BufferSource src = Minecraft.getInstance().renderBuffers().bufferSource();
            if (blend > 0 && modelBookB != null)
            {
                renderModelInterpolate(src, pose, modelBookA, modelBookB, blend);
            }
            else
            {
                renderModel(src, pose, modelBookA);
            }

            src.endBatch();
        }
        pose.popPose();

        Lighting.setupForFlatItems();

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
    }

    private static void renderModel(MultiBufferSource.BufferSource src, PoseStack poseStack, BakedModel model)
    {
        RenderType renderType = ClientHandlers.brightSolid(TextureAtlas.LOCATION_BLOCKS);
        VertexConsumer worldrenderer = src.getBuffer(renderType);

        List<BakedQuad> quads = model.getQuads(null, null, RANDOM, ModelData.EMPTY, null);
        for (BakedQuad quad : quads)
        {
            worldrenderer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, 0x00F000F0, OverlayTexture.NO_OVERLAY, true);
        }
    }

    private static void renderModelInterpolate(MultiBufferSource.BufferSource src, PoseStack poseStack, BakedModel modelA, BakedModel modelB, float blend)
    {
        RenderType renderType = ClientHandlers.brightSolid(TextureAtlas.LOCATION_BLOCKS);
        VertexConsumer worldrenderer = src.getBuffer(renderType);

        List<BakedQuad> generalQuadsA = modelA.getQuads(null, null, RANDOM, ModelData.EMPTY, null);
        List<BakedQuad> generalQuadsB = modelB.getQuads(null, null, RANDOM, ModelData.EMPTY, null);

        int length = DefaultVertexFormat.BLOCK.getVertexSize();

        int minSize = Math.min(generalQuadsA.size(), generalQuadsB.size());

        for (int i = 0; i < minSize; i++)
        {
            BakedQuad quadA = generalQuadsA.get(i);
            BakedQuad quadB = generalQuadsB.get(i);

            int[] dataA = quadA.getVertices();
            int[] dataB = quadB.getVertices();

            int[] blended = Arrays.copyOf(dataA, dataA.length);

            for (int j = 0; j < 4; j++)
            {
                int o = (length / 4) * j;
                for (int k = 0; k < 3; k++)
                {
                    float ax = Float.intBitsToFloat(dataA[o + k]);
                    float bx = Float.intBitsToFloat(dataB[o + k]);
                    blended[o + k] = Float.floatToRawIntBits(ax + blend * (bx - ax));
                }
            }

            int lightEmission = Mth.lerpInt(blend, quadA.getLightEmission(), quadB.getLightEmission());

            BakedQuad bq = new BakedQuad(blended, quadA.getTintIndex(), quadA.getDirection(), quadA.getSprite(), quadA.isShade(), lightEmission);
            worldrenderer.putBulkData(poseStack.last(), bq, 1.0f, 1.0f, 1.0f, 1.0f, 0x00F000F0, OverlayTexture.NO_OVERLAY, true);
        }
    }
}
