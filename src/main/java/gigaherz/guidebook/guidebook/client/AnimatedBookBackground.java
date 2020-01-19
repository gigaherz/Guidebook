package gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.client.DumpBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.*;

public class AnimatedBookBackground implements IAnimatedBookBackground
{
    public static final ResourceLocation BOOK_BACKGROUND = GuidebookMod.location("gui/animated_book");

    public static final Random RANDOM = new Random();

    private static final int ANIMATE_TICKS = 8;
    private static final float ANIMATE_ANGLE = 90;
    private static final float ANGLE_PER_TICK = ANIMATE_ANGLE / ANIMATE_TICKS;
    private float progress = ANIMATE_TICKS;

    private boolean closing = false;

    private final GuiGuidebook gui;

    public AnimatedBookBackground(GuiGuidebook gui)
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
            if (progress >= ANIMATE_TICKS)
            {
                return true;
            }
        }
        else if (progress < 0)
        {
            progress = 0;
        }
        return false;
    }

    /*private static void enableStandardItemLighting()
    {
        RenderSystem.enableLighting();
        RenderSystem.enableLight(0);
        RenderSystem.enableLight(1);
        RenderSystem.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        RenderSystem.enableColorMaterial();
        RenderSystem.light(GL11.GL_LIGHT0, GL11.GL_POSITION, setColorBuffer(-5.0f, -5f, 1.0f, 0.0f));
        RenderSystem.light(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
        RenderSystem.light(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.light(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.light(GL11.GL_LIGHT1, GL11.GL_POSITION, setColorBuffer(5.0f, -6f, 5.0f, 0.0f));
        RenderSystem.light(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, setColorBuffer(0.2F, 0.2F, 0.2F, 1.0F));
        RenderSystem.light(GL11.GL_LIGHT1, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.light(GL11.GL_LIGHT1, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.lightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
    }*/

    boolean debug = true;

    @Override
    public void draw(float partialTicks, int bookHeight, float scalingFactor)
    {
        IBakedModel modelBookA, modelBookB;

        if (closing)
            progress += partialTicks;
        else
            progress -= partialTicks;

        float angleX = progress * ANGLE_PER_TICK;

        IBakedModel unbakedModel = Minecraft.getInstance().getModelManager().getModel(BOOK_BACKGROUND);
        if (!(unbakedModel instanceof CompositeModel))
            return;

        CompositeModel parts = (CompositeModel)unbakedModel;
        IBakedModel book00 = parts.getPart("0");
        IBakedModel book30 = parts.getPart("30");
        IBakedModel book60 = parts.getPart("60");
        IBakedModel book90 = parts.getPart("90");

        if (debug)
        {
            debug = false;
            DumpBakedModel.dumpToOBJ(new File("F:/Modding/Guidebook-1.14.x/book0.dump.obj"), "book0", book00);
            DumpBakedModel.dumpToOBJ(new File("F:/Modding/Guidebook-1.14.x/book30.dump.obj"), "book30", book30);
            DumpBakedModel.dumpToOBJ(new File("F:/Modding/Guidebook-1.14.x/book60.dump.obj"), "book60", book60);
            DumpBakedModel.dumpToOBJ(new File("F:/Modding/Guidebook-1.14.x/book90.dump.obj"), "book90", book90);
        }

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

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1,1,1,1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableAlphaTest();

        RenderSystem.pushMatrix();
        RenderSystem.enableRescaleNormal();
        //enableStandardItemLighting();
        RenderSystem.setupGui3DDiffuseLighting();

        RenderSystem.translated(gui.width * 0.5 * (1 + angleX / 130.0f), gui.height * 0.5 * (1 + angleX / 110.0f) + bookHeight * 0.53, 50);
        RenderSystem.rotatef(180, 0, 1, 0);
        RenderSystem.rotatef(-130, 1, 0, 0);
        RenderSystem.scaled(2.16f * scalingFactor, 2.16f * scalingFactor, 2.7f * scalingFactor);

        RenderSystem.rotatef(angleX * 1.1f, 0, 0, 1);

        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        gui.getRenderEngine().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);

        if (blend > 0 && modelBookB != null)
        {
            renderModelInterpolate(modelBookA, modelBookB, blend);
        }
        else
        {
            renderModel(modelBookA);
        }

        RenderHelper.disableStandardItemLighting();
        RenderSystem.disableRescaleNormal();
        RenderSystem.popMatrix();

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
    }

    private static void renderModel(IBakedModel model)
    {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        List<BakedQuad> quads = model.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        for (BakedQuad quad : quads)
        {
            worldrenderer.addVertexData(matrixStack.peek(), quad, 1.0f, 1.0f, 1.0f, 1.0f, 0, 0, true);
        }
        tessellator.draw();
    }

    private static void renderModelInterpolate(IBakedModel modelA, IBakedModel modelB, float blend)
    {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        List<BakedQuad> generalQuadsA = modelA.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);
        List<BakedQuad> generalQuadsB = modelB.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        int length = DefaultVertexFormats.BLOCK.getSize();

        int minSize = Math.min(generalQuadsA.size(),generalQuadsB.size());

        for (int i = 0; i < minSize; i++)
        {
            BakedQuad quadA = generalQuadsA.get(i);
            BakedQuad quadB = generalQuadsB.get(i);

            int[] dataA = quadA.getVertexData();
            int[] dataB = quadB.getVertexData();

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

            BakedQuad bq = new BakedQuad(blended, quadA.getTintIndex(), quadA.getFace(), quadA.getSprite(), quadA.shouldApplyDiffuseLighting());
            worldrenderer.addVertexData(matrixStack.peek(), bq, 1.0f, 1.0f, 1.0f, 1.0f, 0, 0, true);
        }
        tessellator.draw();
    }
}
