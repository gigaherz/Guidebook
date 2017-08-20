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

    BlockComponent(IBlockState stateIn) {
        this.blockState = stateIn;
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        bakedModel = dispatcher.getModelForState(this.blockState);
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
            bufferbuilder.begin(7, DefaultVertexFormats.ITEM);

            for (EnumFacing enumfacing : EnumFacing.values()) {
                this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, enumfacing, 0L));
            }

            this.renderQuads(bufferbuilder, bakedModel.getQuads(this.blockState, null, 0L));
            tessellator.draw();
        } GlStateManager.popMatrix();
        return new AxisAlignedBB(x, y, z, x + 1f, y + 1f, z + 1f);
    }

    @Override
    public String getTooltip() {
        return blockState.toString();
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