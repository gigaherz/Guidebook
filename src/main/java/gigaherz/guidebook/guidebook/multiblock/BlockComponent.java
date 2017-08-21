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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * @author joazlazer
 *
 * MultiblockComponent that supports the rendering of the baked model corresponding to the given blockstate
 */
@SuppressWarnings("WeakerAccess")
public class BlockComponent extends MultiblockComponent {
    /** The type of block in this particular spot in the structure. */
    protected final IBlockState blockState;
    protected final IBakedModel bakedModel;
    protected final String tooltipCache;

    BlockComponent(IBlockState stateIn) {
        this.blockState = stateIn;
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        bakedModel = dispatcher.getModelForState(this.blockState);

        // Cache tooltip
        ItemStack is = new ItemStack(Item.getItemFromBlock(blockState.getBlock()), 1, blockState.getBlock().damageDropped(blockState));
        List<String> retrievedTooltip = is.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL);
        StringBuilder formatted = new StringBuilder();
        for(int i = 0; i < retrievedTooltip.size(); ++i) {
            if(i != 0) formatted.append("\n");
            formatted.append(retrievedTooltip.get(i));
        }
        tooltipCache = formatted.toString();

    }

    IBlockState getBlockState() {
        return blockState;
    }

    @Override
    public AxisAlignedBB render(float x, float y, float z, float scale) {
        GlStateManager.pushMatrix(); {
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            GlStateManager.translate(x, y, z);
            GlStateManager.scale(scale, scale, scale);

            final float offset = 0.75f * (1f - MathHelper.clamp(scale, 0f, 1f));
            GlStateManager.translate(offset, 0f, offset);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            for (EnumFacing enumfacing : EnumFacing.values()) {
                this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, enumfacing, 0L));
            }
            this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, null, 0L));
            tessellator.draw();
        } GlStateManager.popMatrix();
        return new AxisAlignedBB(x, y, z, x + 1f, y + 1f, z + 1f);
    }

    @Override
    public void renderHighlight(float x, float y, float z, float scale) {

    }

    @Override
    public String getTooltip() {
        return tooltipCache;
    }

    private void renderQuads(BufferBuilder bufferBuilder, List<BakedQuad> quads) {
        for (BakedQuad bakedquad : quads) {
            bufferBuilder.addVertexData(bakedquad.getVertexData());
        }
    }

    @SuppressWarnings("unused")
    public static abstract class Factory extends IForgeRegistryEntry.Impl<Factory> {
        static IForgeRegistry<Factory> registry;

        /**
         * Handles registry of the Factory<BlockComponent> registry to the meta-registry and register the default vanilla Factory<BlockComponent>'s
         */
        @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
        public static class RegistrationHandler {
            @SuppressWarnings("unchecked")
            @SubscribeEvent
            public static void registerRegistries(RegistryEvent.NewRegistry event) {
                RegistryBuilder rb = new RegistryBuilder<Factory>();
                rb.setType(Factory.class);
                rb.setName(new ResourceLocation(GuidebookMod.MODID, "block_component_factory"));
                registry = rb.create();
            }

            @SubscribeEvent
            public static void registerDefaults(RegistryEvent.Register<Factory> event) {
                event.getRegistry().registerAll(new BlockFluidComponent.Factory());
            }
        }

        public abstract Class<?>[] getMappings();
        public abstract BlockComponent create(IBlockState blockState);
    }
}