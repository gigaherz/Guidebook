package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.guidebook.ParseUtils;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.util.vector.Quaternion;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author joazlazer
 * <p>
 * A class to provide loading, rendering, and hoverability of ItemStacks within multiblock structures via the "<stack>" tag
 */
public class ItemComponent extends ParsableMultiblockComponent
{
    // Caches / objects
    private Stack stackObj;
    private AxisAlignedBB cachedBounds;
    private AxisAlignedBB cachedHighlightBounds;
    private TRSRTransformation transformation;

    // Constants
    private static final float ITEM_STACK_SCALE = 1.15f;
    private static final float BOUNDS_SIZE = 0.8f;
    private static final float BOUNDS_OFFSET = 0.1f; // Arbitrary offset needed to correct for (something)
    private static final float GROUND_OFFSET = 0.11f;

    private ItemComponent(Stack stack)
    {
        this.stackObj = stack;
        float lowerBound = (1f - BOUNDS_SIZE) / 2f + BOUNDS_OFFSET;
        float upperBound = (1f + BOUNDS_SIZE) / 2f + BOUNDS_OFFSET;
        cachedBounds = new AxisAlignedBB(lowerBound, lowerBound, lowerBound, upperBound, upperBound, upperBound);
        cachedHighlightBounds = cachedBounds.grow(HIGHLIGHT_EXPAND, HIGHLIGHT_EXPAND, HIGHLIGHT_EXPAND);
    }

    void setTransformation(TRSRTransformation transformation)
    {
        this.transformation = transformation;
    }

    TRSRTransformation getTransformation()
    {
        return transformation;
    }

    /**
     * Renders the component at the specific position and at the specific scale
     * Note: Implementations are responsible for performing the matrix transformations specified via the parameters (in order to support flexibility)
     *
     * @param x     X location in the structure
     * @param y     Y location in the structure
     * @param z     Z location in the structure
     * @param scale Current scale to render at (to support expanding/collapsing)
     * @return A bounding box for mouse ray collision for tooltip rendering
     */
    @Override
    public AxisAlignedBB render(float x, float y, float z, float scale)
    {
        final float offset = this.getOffsetForScale(scale) * 0.666f;
        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.translate(offset, -GROUND_OFFSET, offset);
            GlStateManager.translate(transformation.getTranslation().x, transformation.getTranslation().y, transformation.getTranslation().z);
            GlStateManager.scale(scale, scale, scale);

            renderItem(stackObj.getCurrentStack(), Minecraft.getMinecraft().getRenderItem(), ITEM_STACK_SCALE * transformation.getScale().x, ITEM_STACK_SCALE * transformation.getScale().y, ITEM_STACK_SCALE * transformation.getScale().z, new Quaternion(transformation.getLeftRot().x, transformation.getLeftRot().y, transformation.getLeftRot().z, transformation.getLeftRot().w));
        }
        GlStateManager.popMatrix();
        return cachedBounds.offset((-0.5f + x) * scale + offset, (-0.5f + y) * scale, (-0.5f + z) * scale + offset);
    }

    /**
     * Renders the highlight for the component at the specific position and at the specific scale
     * Note: Implementations are responsible for performing the matrix transformations specified via the parameters (in order to support flexibility)
     *
     * @param x X location in the structure
     * @param y Y location in the structure
     * @param z Z location in the structure
     */
    @Override
    public void renderHighlight(float x, float y, float z, float scale)
    {
        GlStateManager.pushMatrix();
        {
            GlStateManager.disableLighting();

            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);
            final float offset = this.getOffsetForScale(scale);
            GlStateManager.translate(offset, 0f, offset);

            this.renderHighlightBox(cachedHighlightBounds);

            GlStateManager.enableLighting();
        }
        GlStateManager.popMatrix();
    }

    /**
     * Utility method to draw the ItemStack in the current world-space position
     *
     * @param stack        The ItemStack to render
     * @param itemRenderer The RenderItem instance to use
     * @param scaleX       The scale factor for the x-axis
     * @param scaleY       The scale factor for the y-axis
     * @param scaleZ       The scale factor for the z-axis
     * @param quat         A render rotation transformation
     */
    private void renderItem(ItemStack stack, RenderItem itemRenderer, float scaleX, float scaleY, float scaleZ, Quaternion quat)
    {
        if (!stack.isEmpty())
        {
            GlStateManager.pushMatrix();
            {
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                GlStateManager.pushAttrib();
                IBakedModel bakedModel = itemRenderer.getItemModelWithOverrides(stack, null, null);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableRescaleNormal();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.pushMatrix();
                bakedModel = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(bakedModel, ItemCameraTransforms.TransformType.GROUND, false);

                GlStateManager.scale(scaleX, scaleY, scaleZ);
                GlStateManager.rotate(quat);
                itemRenderer.renderItem(stack, bakedModel);

                GlStateManager.cullFace(GlStateManager.CullFace.BACK);
                GlStateManager.popMatrix();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                Minecraft.getMinecraft().getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
                GlStateManager.popAttrib();
            }
            GlStateManager.popMatrix();
        }
    }

    /**
     * Gets the tooltip of the component to draw when hovered over
     *
     * @return A list of Strings that represent each line
     */
    @Override
    public List<String> getTooltip()
    {
        return stackObj.getCurrentStack().getTooltip(null, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
    }

    /**
     * Whether the current multiblock component should be ordered and rendered in another pass
     *
     * @return If the component has translucent features (and needs to support alpha blending)
     */
    @Override
    public boolean isTranslucent()
    {
        return false;
    }

    public static class Parser extends ParsableMultiblockComponent.Parser
    {
        Parser()
        {
            this.setRegistryName("item_component");
        }

        /**
         * Gets the tag mapping for the current parsable multiblock component
         *
         * @return A string to compare to the multiblock's sub-nodes
         */
        @Override
        public String getBaseNodeName()
        {
            return "stack";
        }

        /**
         * Parses the node and creates a new multiblock component from the specified XML parameters
         *
         * @param baseNode The XML node at the base of this component
         * @return A new instance of a ParsableMultiblockComponent
         */
        @Override
        public ParsableMultiblockComponent parse(Node baseNode)
        {
            Stack stack = new Stack();
            stack.parse(baseNode.getAttributes());
            ItemComponent ic = new ItemComponent(stack);

            Node transformationNode = baseNode.getAttributes().getNamedItem("transformation");
            if (transformationNode != null)
            {
                ic.setTransformation(ParseUtils.parseTRSR(transformationNode.getTextContent()));
            }
            if (ic.getTransformation() == null)
            {
                ic.setTransformation(TRSRTransformation.identity());
            }

            return ic;
        }
    }
}
