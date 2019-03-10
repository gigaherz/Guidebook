package gigaherz.guidebook.guidebook.client;

import gigaherz.common.client.ModelHandle;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

import static net.minecraft.client.renderer.RenderHelper.setColorBuffer;

public class AnimatedBookBackground implements IAnimatedBookBackground
{
    private static float angleSpeed = (1 / 0.35f) / 20;
    private float angleT = 1;

    private boolean closing = false;

    private final ModelHandle book00 = ModelHandle.of(GuidebookMod.location("gui/book.obj")).vertexFormat(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
    private final ModelHandle book30 = ModelHandle.of(GuidebookMod.location("gui/book30.obj")).vertexFormat(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
    private final ModelHandle book60 = ModelHandle.of(GuidebookMod.location("gui/book60.obj")).vertexFormat(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
    private final ModelHandle book90 = ModelHandle.of(GuidebookMod.location("gui/book90.obj")).vertexFormat(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

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
        return angleT == 0;
    }

    @Override
    public boolean update()
    {
        if (closing)
        {
            angleT += angleSpeed;
            if (angleT >= 1)
            {
                return true;
            }
        }
        else if (angleT > 0)
        {
            angleT = Math.max(0, angleT - angleSpeed);
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
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, setColorBuffer(-5.0f, -5f, 1.0f, 0.0f));
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, setColorBuffer(5.0f, -6f, 5.0f, 0.0f));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, setColorBuffer(0.2F, 0.2F, 0.2F, 1.0F));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
    }

    @Override
    public void draw(float partialTicks, int bookHeight, float scalingFactor)
    {
        IBakedModel modelBookA, modelBookB;

        float angleX;

        if (closing)
            angleX = (angleT + partialTicks * angleSpeed) * 90;
        else
            angleX = (angleT - partialTicks * angleSpeed) * 90;

        float blend;
        if (angleX <= 0)
        {
            angleX = 0;
            modelBookA = book00.get();
            modelBookB = null;
            blend = 0;
        }
        else if (angleX < 30)
        {
            modelBookA = book00.get();
            modelBookB = book30.get();
            blend = (angleX) / 30.0f;
        }
        else if (angleX < 60)
        {
            modelBookA = book30.get();
            modelBookB = book60.get();
            blend = (angleX - 30) / 30.0f;
        }
        else if (angleX < 90)
        {
            modelBookA = book60.get();
            modelBookB = book90.get();
            blend = (angleX - 60) / 30.0f;
        }
        else
        {
            angleX = 90;
            modelBookA = book90.get();
            modelBookB = null;
            blend = 0;
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        enableStandardItemLighting();

        GlStateManager.translate(gui.width * 0.5 * (1 + angleX / 130.0f), gui.height * 0.5 * (1 + angleX / 110.0f) + bookHeight * 0.53, 50);
        GlStateManager.rotate(180, 0, 1, 0);
        GlStateManager.rotate(-130, 1, 0, 0);
        GlStateManager.scale(2.16f * scalingFactor, 2.16f * scalingFactor, 2.7f * scalingFactor);

        GlStateManager.rotate(angleX * 1.1f, 0, 0, 1);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        gui.getRenderEngine().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        if (modelBookB != null)
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
        GlStateManager.disableDepth();
    }

    private static void renderModel(IBakedModel model)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        for (BakedQuad quad : model.getQuads(null, null, 0))
        {
            worldrenderer.addVertexData(quad.getVertexData());
        }
        tessellator.draw();
    }

    private static void renderModelInterpolate(IBakedModel modelA, IBakedModel modelB, float blend)
    {
        VertexFormat fmt = DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder worldrenderer = tessellator.getBuffer();
        worldrenderer.begin(GL11.GL_QUADS, fmt);
        List<BakedQuad> generalQuadsA = modelA.getQuads(null, null, 0);
        List<BakedQuad> generalQuadsB = modelB.getQuads(null, null, 0);

        int length = fmt.getSize();

        for (int i = 0; i < generalQuadsA.size(); i++)
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
