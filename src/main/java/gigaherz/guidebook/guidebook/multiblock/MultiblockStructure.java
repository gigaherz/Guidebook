package gigaherz.guidebook.guidebook.multiblock;

import com.sun.javafx.geom.Vec3f;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.io.InputStream;
import java.util.List;

/**
 * @author joazlazer
 *
 * Utility structure that aggregates related methods towards MultiblockStructure parsing and rendering
 */
public class MultiblockStructure {
    private MultiblockComponent[][][] structureMatrix; // 3-d array containing a matrix of MultiblockComponents by position, filled with null by default
    private MultiblockComponent[][][] translucentStructureMatrix; // 3-d array containing a 3-d matrix of translucent blocks to be rendered second
    private BlockPos bounds;
    private Vec3f offset;

    private MultiblockStructure(BlockPos size) {
        this.bounds = size;
    }

    public BlockPos getBounds() {
        return bounds;
    }

    public void setOffset(Vec3f offset) {
        this.offset = offset;
    }

    private void setStructureMatrix(MultiblockComponent[][][] structureMatrix) {
        this.structureMatrix = structureMatrix;
    }

    private void setTranslucentStructureMatrix(MultiblockComponent[][][] translucentStructureMatrix) {
        this.translucentStructureMatrix = translucentStructureMatrix;
    }

    public void render(int left, int top, float blockScale, float layerGap, int maxDisplayLayer) {
        float scale = 2f;

        GlStateManager.pushMatrix(); {
            GlStateManager.translate(left, top, 0f);
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            GlStateManager.pushMatrix(); {
                // Apply and scale out lighting
                GlStateManager.scale(24, 24, 24);
                RenderHelper.enableGUIStandardItemLighting();
            } GlStateManager.popMatrix();

            GlStateManager.pushMatrix(); {
                // Set texture manager settings
                textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.enableDepth();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.translate(0, 0, 100.0F);
                GlStateManager.scale(1.0F, -1.0F, 1.0F); // Flip on y-axis
                GlStateManager.scale(16.0F * scale, 16.0F * scale, 16.0F * scale);

                // TODO render floor & poles
                renderComponents(structureMatrix, maxDisplayLayer, layerGap, blockScale);
                renderComponents(translucentStructureMatrix, maxDisplayLayer, layerGap, blockScale);

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableAlpha();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
            } GlStateManager.popMatrix();

            // Reset texture manager settings
            textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        } GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    private void renderComponents(MultiblockComponent[][][] componentMatrix, int maxDisplayLayer, float layerGap, float blockScale) {
        GlStateManager.pushMatrix(); {
            applyGUITransformationMatrix();
            final float offsetX = offset.x;
            final float offsetY = offset.y - layerGap * ((componentMatrix[0].length - 1) / 2f);
            final float offsetZ = offset.z;

            // Render each component
            for(int i = 0; i < componentMatrix.length; ++i) {
                for(int j = 0; j < componentMatrix[i].length; ++j) {
                    if(j + 1 <= maxDisplayLayer) {
                        for (int k = 0; k < componentMatrix[i][j].length; ++k) {
                            if (componentMatrix[i][j][k] != null) {
                                componentMatrix[i][j][k].render(i + offsetX, j + offsetY + (layerGap * j), k + offsetZ, blockScale);
                            }
                        }
                    }
                }
            }
        } GlStateManager.popMatrix();
    }

    private void applyGUITransformationMatrix() {
        // Get the GUI transformation matrix and apply it
        TRSRTransformation transformation = TRSRTransformation.blockCenterToCorner(new TRSRTransformation(new Vector3f(0, 0, 0), TRSRTransformation.quatFromXYZDegrees(new Vector3f(30, 225, 0)), new Vector3f(0.625f, 0.625f, 0.625f), null));
        Matrix4f mat = null;
        if(!transformation.equals(TRSRTransformation.identity())) mat = transformation.getMatrix();
        ForgeHooksClient.multiplyCurrentGlMatrix(mat);
    }

    /**
     * Attempts to find the structure nbt file at the given resource location and parse it, creating a new MultiblockStructure object
     * @param structureRL The resource location of the file within assets/<domain>/structures/<path>.nbt
     * @return The new Multiblock structure object, or null if parsing failed
     */
    @Nullable
    public static MultiblockStructure tryParse(@Nonnull ResourceLocation structureRL) {
        Template blockTemplate;
        // Try and retrieve the template from file
        {
            String domain = structureRL.getResourceDomain();
            String path = structureRL.getResourcePath();
            InputStream inputstream = null;

            try {
                inputstream = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(domain, "structures/" + path + ".nbt")).getInputStream();
                NBTTagCompound nbttagcompound = CompressedStreamTools.readCompressed(inputstream);
                blockTemplate = new Template();
                blockTemplate.read(nbttagcompound);
            } catch (Throwable ex) {
                blockTemplate = null; // Ensure nothing is loaded
            } finally {
                IOUtils.closeQuietly(inputstream);
            }
        }
        if(blockTemplate != null) {
            // Retrieve the list of blocks via Reflection
            List<Template.BlockInfo> blocks = ReflectionHelper.getPrivateValue(Template.class, blockTemplate,"blocks");

            // Initialize the multiblock structure and its component matrices
            MultiblockStructure structure = new MultiblockStructure(blockTemplate.getSize());
            MultiblockComponent[][][] structureMatrix = new MultiblockComponent[structure.getBounds().getX()][][];
            MultiblockComponent[][][] translucentStructureMatrix = new MultiblockComponent[structure.getBounds().getX()][][];

            // Loop through each block in the structure
            for(Template.BlockInfo block : blocks) {
                BlockComponent component;
                if(block.blockState.getBlock() instanceof BlockFluidBase || block.blockState.getBlock() instanceof BlockLiquid) component = new BlockFluidComponent(block.blockState);
                else component = new BlockComponent(block.blockState);
                // If necessary, initialize the y-array at the x-position
                if(structureMatrix[block.pos.getX()] == null) structureMatrix[block.pos.getX()] = new MultiblockComponent[structure.getBounds().getY()][];
                if(translucentStructureMatrix[block.pos.getX()] == null) translucentStructureMatrix[block.pos.getX()] = new MultiblockComponent[structure.getBounds().getY()][];
                // If necessary, initialize the z-array at the x-y-position
                if(structureMatrix[block.pos.getX()][block.pos.getY()] == null) structureMatrix[block.pos.getX()][block.pos.getY()] = new MultiblockComponent[structure.getBounds().getZ()];
                if(translucentStructureMatrix[block.pos.getX()][block.pos.getY()] == null) translucentStructureMatrix[block.pos.getX()][block.pos.getY()] = new MultiblockComponent[structure.getBounds().getZ()];
                // If the block is air, set it to null (but still add it)
                if(block.blockState.getBlock() == Blocks.AIR) component = null;

                if(component != null && component.getBlockState().getBlock().getBlockLayer() == BlockRenderLayer.TRANSLUCENT) {
                    translucentStructureMatrix[block.pos.getX()][block.pos.getY()][block.pos.getZ()] = component;
                    structureMatrix[block.pos.getX()][block.pos.getY()][block.pos.getZ()] = null;
                } else {
                    structureMatrix[block.pos.getX()][block.pos.getY()][block.pos.getZ()] = component;
                    translucentStructureMatrix[block.pos.getX()][block.pos.getY()][block.pos.getZ()] = null;
                }
            }

            structure.setStructureMatrix(structureMatrix);
            structure.setTranslucentStructureMatrix(translucentStructureMatrix);
            return structure;

        } else {
            GuidebookMod.logger.warn(String.format("Structure '%s.nbt' not found in assets/%s/structures/! Ignoring!", structureRL.getResourcePath(), structureRL.getResourceDomain()));
            return null;
        }
    }
}
