package gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.platform.GlStateManager;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static net.minecraft.client.renderer.RenderHelper.setColorBuffer;

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

    private static void enableStandardItemLighting()
    {
        GlStateManager.enableLighting();
        GlStateManager.enableLight(0);
        GlStateManager.enableLight(1);
        GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        GlStateManager.enableColorMaterial();
        GlStateManager.light(GL11.GL_LIGHT0, GL11.GL_POSITION, setColorBuffer(-5.0f, -5f, 1.0f, 0.0f));
        GlStateManager.light(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
        GlStateManager.light(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.light(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.light(GL11.GL_LIGHT1, GL11.GL_POSITION, setColorBuffer(5.0f, -6f, 5.0f, 0.0f));
        GlStateManager.light(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, setColorBuffer(0.2F, 0.2F, 0.2F, 1.0F));
        GlStateManager.light(GL11.GL_LIGHT1, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.light(GL11.GL_LIGHT1, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.lightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
    }

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

        CompositeModel parts = (CompositeModel) unbakedModel;
        IBakedModel book00 = parts.getPart("0");
        IBakedModel book30 = parts.getPart("30");
        IBakedModel book60 = parts.getPart("60");
        IBakedModel book90 = parts.getPart("90");

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

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(1, 1, 1, 1);

        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableAlphaTest();

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        enableStandardItemLighting();

        scalingFactor *= 2.16;

        GlStateManager.translated(gui.width * 0.5 * (1 + angleX / 130.0f), gui.height * 0.5 * (1 + angleX / 110.0f) + bookHeight * 0.53, 250);
        GlStateManager.rotated(180, 0, 1, 0);
        GlStateManager.rotated(-130, 1, 0, 0);
        GlStateManager.scaled(scalingFactor, scalingFactor, scalingFactor);
        GlStateManager.scaled(1, 1, 1.25);

        GlStateManager.rotated(angleX * 1.1f, 0, 0, 1);

        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);

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
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableDepthTest();
    }

    private static void renderModel(IBakedModel model)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        List<BakedQuad> quads = model.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);
        VertexFormat fmt = quads.get(0).getFormat();
        worldrenderer.begin(GL11.GL_QUADS, fmt);
        for (BakedQuad quad : quads)
        {
            worldrenderer.addVertexData(quad.getVertexData());
        }
        tessellator.draw();
    }

    private static void renderModelInterpolate(IBakedModel modelA, IBakedModel modelB, float blend)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        List<BakedQuad> generalQuadsA = modelA.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);
        List<BakedQuad> generalQuadsB = modelB.getQuads(null, null, RANDOM, EmptyModelData.INSTANCE);

        VertexFormat fmt = generalQuadsA.get(0).getFormat();

        worldrenderer.begin(GL11.GL_QUADS, fmt);
        int length = fmt.getSize();

        int minSize = Math.min(generalQuadsA.size(), generalQuadsB.size());

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

            worldrenderer.addVertexData(blended);
        }
        tessellator.draw();
    }
}
