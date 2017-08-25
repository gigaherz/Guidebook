package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * @author joazlazer
 * <p>
 * MultiblockComponent that supports the rendering of the baked model corresponding to the given blockstate
 */
@SuppressWarnings("WeakerAccess")
public class BlockComponent extends MultiblockComponent
{
    /**
     * The type of block in this particular spot in the structure.
     */
    protected final IBlockState blockState;
    protected final IBakedModel bakedModel;
    protected final List<String> tooltipCache;
    protected final AxisAlignedBB cachedBounds;
    protected final AxisAlignedBB cachedHighlightBounds;

    BlockComponent(IBlockState stateIn, IBlockAccess blockAccess, BlockPos position)
    {
        this.blockState = stateIn;
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        bakedModel = dispatcher.getModelForState(this.blockState);

        // Cache tooltip
        ItemStack is = new ItemStack(Item.getItemFromBlock(blockState.getBlock()), 1, blockState.getBlock().damageDropped(blockState));
        tooltipCache = is.getTooltip(null, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);

        cachedBounds = getBounds(blockState, blockAccess, position);
        cachedHighlightBounds = cachedBounds.grow(HIGHLIGHT_EXPAND, HIGHLIGHT_EXPAND, HIGHLIGHT_EXPAND);
    }

    /**
     * Gets the bounding box of the specified block state
     *
     * @param stateIn     The IBlockState of the object
     * @param blockAccess Access to the block's surrounding blocks
     * @param position    The block's position in the 'world'
     * @return A cached bounding box
     */
    protected AxisAlignedBB getBounds(IBlockState stateIn, IBlockAccess blockAccess, BlockPos position)
    {
        return stateIn.getBoundingBox(blockAccess, position);
    }

    IBlockState getBlockState()
    {
        return blockState;
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
        final float offset = this.getOffsetForScale(scale);
        GlStateManager.pushMatrix();
        {
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.translate(offset, 0f, offset);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            for (EnumFacing enumfacing : EnumFacing.values())
            {
                this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, enumfacing, 0L));
            }
            this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, null, 0L));
            tessellator.draw();
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
     * Gets the tooltip of the component to draw when hovered over
     *
     * @return A list of Strings that represent each line
     */
    @Override
    public List<String> getTooltip()
    {
        return tooltipCache;
    }

    /**
     * Whether the current multiblock component should be ordered and rendered in another pass
     *
     * @return If the component has translucent features (and needs to support alpha blending)
     */
    @Override
    public boolean isTranslucent()
    {
        return blockState.getBlock().getBlockLayer() == BlockRenderLayer.TRANSLUCENT;
    }

    /**
     * Helper method to draw each quad in a list of BakedQuads to the buffer
     *
     * @param bufferBuilder The specified buffer instance
     * @param quads         A list of BakedQuads to render
     */
    protected void renderQuads(BufferBuilder bufferBuilder, List<BakedQuad> quads)
    {
        for (BakedQuad bakedquad : quads)
        {
            bufferBuilder.addVertexData(bakedquad.getVertexData());
        }
    }

    /**
     * A registrable factory-type class that initializes specific implementations of BlockComponent according to certain special Block mappings
     * Default implementations:
     * - BlockFluidComponent which is mapped to BlockFluidBase and BlockLiquid instances
     */
    @SuppressWarnings("unused")
    public static abstract class Factory extends IForgeRegistryEntry.Impl<Factory>
    {
        static IForgeRegistry<Factory> registry;

        /**
         * Handles registry of the Factory<BlockComponent> registry to the meta-registry and register the default vanilla Factory<BlockComponent>'s
         */
        @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
        public static class RegistrationHandler
        {
            @SuppressWarnings("unchecked")
            @SubscribeEvent
            public static void registerRegistries(RegistryEvent.NewRegistry event)
            {
                RegistryBuilder rb = new RegistryBuilder<Factory>();
                rb.setType(Factory.class);
                rb.setName(new ResourceLocation(GuidebookMod.MODID, "block_component_factory"));
                registry = rb.create();
            }

            @SubscribeEvent
            public static void registerDefaults(RegistryEvent.Register<Factory> event)
            {
                event.getRegistry().registerAll(new BlockFluidComponent.Factory());
            }
        }

        /**
         * Gets the class mappings that the factory will be used with
         *
         * @return An array of Class objects that each should extend Block
         */
        public abstract Class<?>[] getMappings();

        /**
         * Initializes a new instance of the factor's target type
         *
         * @param blockState  The block's block state
         * @param blockAccess Access to the block's neighbors
         * @param position    The block's position in the multiblock
         * @return The custom BlockComponent implementation
         */
        public abstract BlockComponent create(IBlockState blockState, IBlockAccess blockAccess, BlockPos position);
    }
}