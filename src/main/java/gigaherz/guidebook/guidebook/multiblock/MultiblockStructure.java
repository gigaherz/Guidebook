package gigaherz.guidebook.guidebook.multiblock;

import com.sun.javafx.geom.Vec3f;
import com.sun.javafx.geom.Vec4f;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.MultiblockPanel;
import javafx.util.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author joazlazer
 *
 * Utility structure that aggregates related methods towards MultiblockStructure parsing and rendering
 */
public class MultiblockStructure {
    private MultiblockComponent[][][] structureMatrix; // 3-d array containing a matrix of MultiblockComponents by position, filled with null by default
    private MultiblockComponent[][][] translucentStructureMatrix; // 3-d array containing a 3-d matrix of translucent blocks to be rendered second
    private List<Pair<AxisAlignedBB, String>> tooltipBBs;
    private Point2i[] floorLocations;
    private Vec3i[] poleLocations;
    private Vec3i hoveredPos = new Vec3i(1, 1, 1); // TODO update
    private BlockPos bounds;
    private float scale;
    private Vec3f offset;
    private Vec4f initialRot;
    private MultiblockPanel.FloorMode floorMode;
    private MultiblockPanel.PoleMode poleMode;
    private FloatBuffer modelViewCache;
    private FloatBuffer projectionCache;
    private IntBuffer viewportCache;

    private static final ResourceLocation FLOOR_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_floor.png");
    private static final ResourceLocation POLE_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_beacon.png");

    private MultiblockStructure(BlockPos size) {
        this.bounds = size;
        tooltipBBs = new ArrayList<>();
    }

    public BlockPos getBounds() {
        return bounds;
    }

    public void setOffset(Vec3f offset) {
        this.offset = offset;
    }

    public void setInitialRot(Vec4f initialRot) {
        this.initialRot = initialRot;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setPoleMode(MultiblockPanel.PoleMode poleMode) {
        this.poleMode = poleMode;
    }

    public void setFloorMode(MultiblockPanel.FloorMode floorMode) {
        this.floorMode = floorMode;
    }

    private void setStructureMatrix(MultiblockComponent[][][] structureMatrix) {
        this.structureMatrix = structureMatrix;
    }

    private void setTranslucentStructureMatrix(MultiblockComponent[][][] translucentStructureMatrix) {
        this.translucentStructureMatrix = translucentStructureMatrix;
    }

    /**
     * Draws the structure at the given position
     * @param left X coordinate to begin at
     * @param top Y coordinate to begin at
     * @param blockScale Interpolated and animated scale for each individual block
     * @param layerGap Y-layer gap
     * @param maxDisplayLayer The maximum layer to display, hides others
     * @param globalScale Interpolated and animated scale for the multiblock as a whole
     * @param spinAngle Interpolated and animated angle for spinning the multiblock
     */
    public void render(int left, int top, float blockScale, float layerGap, int maxDisplayLayer, float globalScale, float spinAngle) {
        GlStateManager.pushMatrix(); {
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

            GlStateManager.pushMatrix(); {

                // Set up OpenGL settings
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                GlStateManager.enableDepth();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                translate(left, top, 0f); // Screen-space transform to the specified x,y
                translate(-18f, 0f, 100.0F); // Screen-space transform to the left 18 pixels (necessary arbitrary offset) and forward on the z-axis by 100 units

                GlStateManager.pushMatrix(); {
                    // Apply and scale out lighting
                    GlStateManager.scale(24, 24, 24);
                    RenderHelper.enableGUIStandardItemLighting();
                } GlStateManager.popMatrix();

                scale(1.0F, -1.0F, 1.0F); // Screen-space flip on y-axis
                scale(16.0F, 16.0F, 16.0F); // Screen-space scale by 16 units in each direction to make the multiblock appear larger
                applyGUITransformationMatrix(); // Screens-space -> Block-space

                // Block space transformations:
                scale(2.0F, 2.0F, 2.0F); // Scale each block up by a hard 2x in each direction
                scale(scale, scale, scale); // Then, scale by the specified parsed scale in each direction
                scale(globalScale, globalScale, globalScale); // Finally, scale by the interpolated value used for scaling down during expansion
                rotate(spinAngle, 0f, 1f, 0f); // Rotate the structure by the interpolated spin angle
                //noinspection SuspiciousNameCombination
                rotate(initialRot.x, initialRot.y, initialRot.z, initialRot.w); // Rotate by the specified parsed scale in each direction
                translate(offset.x, offset.y, offset.z); // Transform by the specified parsed offset

                storeViewModelPerspective();

                renderPoles(layerGap, blockScale, maxDisplayLayer); // Draw the poles and lazily initialize if null
                renderFloor(layerGap); // Draw the floor and lazily initialize if null

                // Set texture manager settings
                textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);

                tooltipBBs.clear();
                renderComponents(structureMatrix, maxDisplayLayer, layerGap, blockScale); // Draw opaque blocks
                renderComponents(translucentStructureMatrix, maxDisplayLayer, layerGap, blockScale); // Draw translucent blocks

                MultiblockComponent hoveredComponent = getBlockAt(hoveredPos.getX(), hoveredPos.getY(), hoveredPos.getZ()); // If applicable, draw the hover highlight for the highlighted component
                if(hoveredComponent != null) {
                    hoveredComponent.renderHighlight(1, 1 + (-layerGap * (bounds.getY() - 1) / 2f) + (layerGap * 1), 1, blockScale);
                }

                // Reset texture manager settings
                textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableAlpha();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
            } GlStateManager.popMatrix();

        } GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    private void storeViewModelPerspective() {
        modelViewCache = BufferUtils.createFloatBuffer(16);
        projectionCache = BufferUtils.createFloatBuffer(16);
        viewportCache = BufferUtils.createIntBuffer(16);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewCache);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionCache);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportCache);
    }

    private void scale(float x, float y, float z) {
        GlStateManager.scale(x, y, z);
    }

    private void rotate(float angle, float x, float y, float z) {
        GlStateManager.rotate(angle, x, y, z);
    }

    private void translate(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    /**
     *
     * @param info
     * @param mouseX
     * @param mouseY
     */
    public void mouseOver(IBookGraphics info, int mouseX, int mouseY) {
        Pair<Vector3f, Vector3f> mouseRay = getMouseRay(mouseX, mouseY, this.modelViewCache, this.projectionCache, this.viewportCache);

        List<Pair<RayTraceResult, String>> results = new ArrayList<>();
        for(Pair<AxisAlignedBB, String> bbStringPair : tooltipBBs) {
            RayTraceResult result = bbStringPair.getKey().calculateIntercept(toVec3d(mouseRay.getKey()), toVec3d(mouseRay.getValue()));
            if(result != null && result.typeOfHit != RayTraceResult.Type.MISS) {
                results.add(new Pair<>(result, bbStringPair.getValue()));
            }
        }

        if(results.size() != 0) {
            Pair<RayTraceResult, String> minDistance = results.get(0);
            for(Pair<RayTraceResult, String> result : results) {
                if(result == minDistance) continue;
                if(minDistance.getKey().hitVec.squareDistanceTo(toVec3d(mouseRay.getKey())) > result.getKey().hitVec.squareDistanceTo(toVec3d(mouseRay.getKey()))) minDistance = result;
            }
            info.drawHoverText(mouseX, mouseY, 101, minDistance.getValue());
        }
        BlockComponent bc = getBlockAt(hoveredPos.getX(), hoveredPos.getY(), hoveredPos.getZ());
        info.drawHoverText(mouseX, mouseY, 101, bc != null ? bc.getTooltip() : "");
    }

    private Vec3d toVec3d(Vector3f vecIn) {
        return new Vec3d(vecIn.x, vecIn.y, vecIn.z);
    }

    /**
     *
     * @param mouseX
     * @param mouseY
     * @param modelView
     * @param projection
     * @param viewport
     * @return
     */
    public static Pair<Vector3f, Vector3f> getMouseRay(int mouseX, int mouseY, FloatBuffer modelView, FloatBuffer projection, IntBuffer viewport) {
        float winX, winY;
        FloatBuffer startPos = BufferUtils.createFloatBuffer(3);
        FloatBuffer endPos = BufferUtils.createFloatBuffer(3);
        winX = (float) mouseX;
        winY = (float) viewport.get(2) - (float) mouseY;
        GLU.gluUnProject(winX, winY, 0f, modelView, projection, viewport, startPos);
        GLU.gluUnProject(winX, winY, 1f, modelView, projection, viewport, endPos);
        return new Pair<>(new Vector3f(startPos.get(0), startPos.get(1), startPos.get(2)), new Vector3f(endPos.get(0), endPos.get(1), endPos.get(2)));
    }

    /**
     * Draws thin opaque beacon-like poles according to the PoleMode
     * @param layerGap Y-layer gap
     * @param blockScale Interpolated and animated scale for each individual block
     * @param maxDisplayLayer The maximum layer to display, hides others
     */
    private void renderPoles(float layerGap, float blockScale, int maxDisplayLayer) {
        GlStateManager.pushMatrix(); {
            GlStateManager.disableLighting(); // Disable lighting for pole rendering

            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap
            Vector3f tileSize = new Vector3f(0f, 1f + layerGap, 1f); // Create a plane the size of one block plus the layer gap
            Vector3f tileLoc = new Vector3f(0f, 0f, 0f);
            BufferBuilder renderer = Tessellator.getInstance().getBuffer();
            Point2d uv = new Point2d(0d, 0d);
            Point2d wh = new Point2d(1d, 1d);
            Color poleColor = Color.white;

            GlStateManager.translate(0f, offsetY, 0f);
            for(Vec3i polePoint : getPoleLocations()) {
                if(polePoint.getY() + 2 <= maxDisplayLayer) { // If the current layer should be rendered according to the display layer slider
                    GlStateManager.pushMatrix(); {
                        GlStateManager.translate(polePoint.getX(), polePoint.getY() + (polePoint.getY() * layerGap), polePoint.getZ()); // Offset by the pole's location and then by the layer gap offset at that point
                        GlStateManager.scale(blockScale, blockScale, blockScale); // Scale in each direction by the current block scale
                        GlStateManager.translate(0f, 0f, -0.5F); // Translate by negative half a block in the z to prepare for rendering the CrossModel
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.EAST, poleColor); // Draw forward-facing plane
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.WEST, poleColor); // Draw backward-facing plane
                        GlStateManager.translate(0f, 0f, +0.5F); // Reset that translation
                        GlStateManager.translate(-0.5F, 0, 0F); // Move the plane into a new position for the other cross plane
                        GlStateManager.rotate(90f, 0f, 1f, 0f); // Rotate by 90 degrees into the new position
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.EAST, poleColor); // Draw forward-facing plane
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.WEST, poleColor); // Draw backward-facing plane
                    } GlStateManager.popMatrix();
                }
            }
            GlStateManager.enableLighting(); // Re-enable lighting
        } GlStateManager.popMatrix();
    }

    /**
     * Draws a thin grid-like flooring according to the FloorMode
     * @param layerGap Y-layer gap
     */
    private void renderFloor(float layerGap) {
        GlStateManager.pushMatrix(); {
            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap

            Vector3f tileSize = new Vector3f(1f, 0f, 1f); // Floor tile 1x1 and with 0-height
            Vector3f tileLoc = new Vector3f(0f, 0f, 0f);
            BufferBuilder renderer = Tessellator.getInstance().getBuffer();
            Point2d uv = new Point2d(0d, 0d);
            Point2d wh = new Point2d(1d, 1d);
            Color tileColor = new Color(255, 255, 255, 100);

            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();

            // Render each floor tile
            for(Point2i tilePoint : getFloorLocations()) {
                GlStateManager.pushMatrix(); {
                    GlStateManager.translate(tilePoint.x, offsetY, tilePoint.y); // Offset to the tile block position and offset by the calculated y offset
                    GlStateManager.translate(-0.5F, -0.5F, -0.5F); // Offset to the back corner of the block position
                    drawTexturedQuad(tileLoc, tileSize, renderer, FLOOR_TEXTURE, uv, wh, EnumFacing.UP, tileColor); // Draw up-facing plane
                    drawTexturedQuad(tileLoc, tileSize, renderer, FLOOR_TEXTURE, uv, wh, EnumFacing.DOWN, tileColor); // Draw down-facing plane
                } GlStateManager.popMatrix();
            }
            GlStateManager.enableLighting();
        } GlStateManager.popMatrix();
    }

    /**
     * Draws each MultiblockComponent in the given componentMatrix
     * @param componentMatrix A 3-deep jagged array representing a matrix of MultiblockComponents
     * @param maxDisplayLayer The maximum layer to display, hides others
     * @param layerGap Y-layer gap
     * @param blockScale Interpolated and animated scale for each individual block
     */
    private void renderComponents(MultiblockComponent[][][] componentMatrix, int maxDisplayLayer, float layerGap, float blockScale) {
        GlStateManager.pushMatrix(); {
            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap

            // Render each component
            for(int i = 0; i < componentMatrix.length; ++i) { // Loop through each x and y array
                for(int j = 0; j < componentMatrix[i].length; ++j) {
                    if(j + 1 <= maxDisplayLayer) { // If the current layer should be rendered according to the display layer slider
                        for (int k = 0; k < componentMatrix[i][j].length; ++k) {
                            if (componentMatrix[i][j][k] != null) { // Ensure there isn't air at the position
                                tooltipBBs.add(new Pair<>(componentMatrix[i][j][k].render(i, j + offsetY + (layerGap * j), k, blockScale), componentMatrix[i][j][k].getTooltip()));
                            }
                        }
                    }
                }
            }
        } GlStateManager.popMatrix();
    }

    /**
     * Apply a transformation matrix that converts screens-space to block/world-space
     * Transforms it into an orthographic isometric style with each X,Y,Z face showing
     * Mimics the transform of ItemStacks rendered in GUIContainers
     */
    private void applyGUITransformationMatrix() {
        TRSRTransformation transformation = TRSRTransformation.blockCenterToCorner(new TRSRTransformation(new Vector3f(0, 0, 0), TRSRTransformation.quatFromXYZDegrees(new Vector3f(30, 225, 0)), new Vector3f(0.625f, 0.625f, 0.625f), null)); // Hard-coded copy of the standard GUI matrix
        Matrix4f mat = null;
        if(!transformation.equals(TRSRTransformation.identity())) mat = transformation.getMatrix();
        ForgeHooksClient.multiplyCurrentGlMatrix(mat);
    }

    /**
     * Lazily initializes floor locations according to the parsed FloorMode
     * @return An array of locations to render a floor tile at
     */
    private Point2i[] getFloorLocations() {
        if(floorLocations != null) return floorLocations;
        List<Point2i> floorLocationList = new ArrayList<>();
        switch (floorMode) {
            case GRID: { // Adds one tile under each x,z coordinate, filled with blocks above or not
                for(int x = 0; x < getBounds().getX(); ++x) {
                    for(int z = 0; z < getBounds().getZ(); ++z) {
                        floorLocationList.add(new Point2i(x, z));
                    }
                }
                break;
            }
            case UNDER: { // Adds one tile under each x,z coordinate that has at least one non-air block in the column
                for(int x = 0; x < getBounds().getX(); ++x) {
                    for(int z = 0; z < getBounds().getZ(); ++z) {
                        for(int y = 0; y < getBounds().getY(); ++y) {
                            if(hasBlockAt(x, y, z)) {
                                floorLocationList.add(new Point2i(x, y));
                                break;
                            }
                        }
                    }
                }
                break;
            }
            case ADJACENT: { // Adds one tile under each x,z coordinate that has at least one non-air block in the column, and then 4 additional tiles to each side of that tile
                boolean[][] floorInPos = new boolean[getBounds().getX() + 2][];
                for(int i = 0; i < floorInPos.length; ++i) floorInPos[i] = new boolean[getBounds().getZ() + 2];
                for(int x = 0; x < getBounds().getX(); ++x) {
                    for(int z = 0; z < getBounds().getZ(); ++z) {
                        for(int y = 0; y < getBounds().getY(); ++y) {
                            if(hasBlockAt(x, y, z)) {
                                floorInPos[x + 1][z + 1] = true;

                                floorInPos[x][z + 1] = true;
                                floorInPos[x + 2][z + 1] = true;
                                floorInPos[x + 1][z + 2] = true;
                                floorInPos[x + 1][z] = true;
                                break;
                            }
                        }
                    }
                }
                for(int x = 0; x < floorInPos.length; ++x) {
                    for(int z = 0; z < floorInPos[x].length; ++z) {
                        if(floorInPos[x][z]) floorLocationList.add(new Point2i(x - 1, z - 1));
                    }
                }
                break;
            }
            case AROUND: { // Adds one tile under each x,z coordinate that has at least one non-air block in the column, and then 8 additional tiles to each side and corner of that tile
                boolean[][] floorInPos = new boolean[getBounds().getX() + 2][];
                for(int i = 0; i < floorInPos.length; ++i) floorInPos[i] = new boolean[getBounds().getZ() + 2];
                for(int x = 0; x < getBounds().getX(); ++x) {
                    for(int z = 0; z < getBounds().getZ(); ++z) {
                        for(int y = 0; y < getBounds().getY(); ++y) {
                            if(hasBlockAt(x, y, z)) {
                                floorInPos[x + 1][z + 1] = true;

                                floorInPos[x][z] = true;
                                floorInPos[x][z + 1] = true;
                                floorInPos[x][z + 2] = true;

                                floorInPos[x + 2][z] = true;
                                floorInPos[x + 2][z + 1] = true;
                                floorInPos[x + 2][z + 2] = true;

                                floorInPos[x + 1][z + 2] = true;
                                floorInPos[x + 1][z] = true;
                                break;
                            }
                        }
                    }
                }
                for(int x = 0; x < floorInPos.length; ++x) {
                    for(int z = 0; z < floorInPos[x].length; ++z) {
                        if(floorInPos[x][z]) floorLocationList.add(new Point2i(x - 1, z - 1));
                    }
                }
                break;
            }
            case OFF:
            default:
                // Do nothing and leave the list empty
                break;
        }
        floorLocations = new Point2i[floorLocationList.size()];
        return floorLocationList.toArray(floorLocations);
    }

    /**
     * Lazily initializes pole locations according to the parsed PoleMode
     * @return An array of locations to render a pole beacon at
     */
    private Vec3i[] getPoleLocations() {
        if (poleLocations != null) return poleLocations;
        List<Vec3i> poleLocationList = new ArrayList<>();
        switch (poleMode) {
            case ON: { // If the multiblock is larger than 3 in both x and z directions, add one pole in each corner starting from the bottom and continuing up as long as a non-air block exists above
                if(getBounds().getY() >= 3 && getBounds().getX() >= 3) {
                    poleLocationList.addAll(getPoles(0, 0));
                    poleLocationList.addAll(getPoles(getBounds().getX() - 1, 0));
                    poleLocationList.addAll(getPoles(getBounds().getX() - 1, getBounds().getZ() - 1));
                    poleLocationList.addAll(getPoles(0, getBounds().getZ() - 1));
                }
            }
            case BELOW_ITEMS: {
                // TODO Implement after items have been
            }
            case OFF:
            default:
                // Do nothing and leave the list empty
                break;
        }
        poleLocations = new Vec3i[poleLocationList.size()];
        return poleLocationList.toArray(poleLocations);
    }

    /**
     * Returns a list of poles starting from the ground and moving up as long as a non-air block exists above
     * @param x multiblock x-position
     * @param z multiblock z-position
     * @return A list of valid x,y,z block positions of poles
     */
    private Collection<? extends Vec3i> getPoles(int x, int z) {
        List<Vec3i> poleLocationList = new ArrayList<>();
        for(int y = 0; y < getBounds().getY() - 1; ++y) {
            if(getBlockAt(x, y, z) != null && getBlockAt(x, y + 1, z) != null) {
                poleLocationList.add(new Vec3i(x, y, z));
            } else break;
        }
        return poleLocationList;
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return Whether there is a block at the given x,y,z coordinate in the multiblock structure
     */
    private boolean hasBlockAt(int x, int y, int z) {
        // TODO Fix with the translucent block restructure
        return (structureMatrix[x][y][z] != null || translucentStructureMatrix[x][y][z] != null);
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return The BlockComponent at the given x,y,z coordinate, or <code>null</code> if none exist there
     */
    private BlockComponent getBlockAt(int x, int y, int z) {
        // TODO Fix with the translucent block restructure
        if(!hasBlockAt(x, y, z)) return null;
        if(structureMatrix[x][y][z] != null && structureMatrix[x][y][z] instanceof BlockComponent) return (BlockComponent) structureMatrix[x][y][z];
        if(translucentStructureMatrix[x][y][z] != null && translucentStructureMatrix[x][y][z] instanceof BlockComponent) return (BlockComponent) translucentStructureMatrix[x][y][z];
        return null;
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
                component = makeBlockComponent(block.blockState); // Retrieve the correct block component type instance

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

    /**
     * Creates a BlockComponent based off of the BlockState's block
     * Uses BlockComponent.Factory.registry to create mapped instances
     * @param blockState The block state that was parsed from the template
     * @return A specialized BlockComponent subclass if a registered Factory has the block mapped, or a BlockComponent if not
     */
    private static BlockComponent makeBlockComponent(IBlockState blockState) {
        // Loop through each factory
        for(BlockComponent.Factory componentFactory : BlockComponent.Factory.registry.getValues()) {
            // Loop through each of its class mappings
            for(Class<?> blockClass : componentFactory.getMappings()) {
                // If the BlockState's block is an instance of one of the class mappings, create a new instance of that type of BlockComponent and return it
                if(blockClass.isInstance(blockState.getBlock())) {
                    return componentFactory.create(blockState);
                }
            }
        }
        // If no mapping was needed, use the default BlockComponent
        return new BlockComponent(blockState);
    }

    /**
     * Utility method that draws a textured face of a cuboid based off of Tinkers' fluid rendering utility method
     * @param location The relative location (starting x,y,z)
     * @param dimensions The dimensions of the cuboid (ending x,y,z - starting x,y,z)
     * @param renderer BufferBuilder instance
     * @param texture texture location
     * @param UV u,v coordinates on texture
     * @param WH uWidth, vHeight on texture
     * @param face The face of the cuboid to render
     * @param color The glColor color
     */
    static void drawTexturedQuad(Vector3f location, Vector3f dimensions, BufferBuilder renderer, ResourceLocation texture, Point2d UV, Point2d WH, EnumFacing face, Color color) {
        if(UV == null || WH == null) return;
        double minU, maxU, minV, maxV;
        double x1 = location.x;
        double x2 = dimensions.x;
        double y1 = location.y;
        double y2 = dimensions.y;
        double z1 = location.z;
        double z2 = dimensions.z;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        minU = UV.x;
        minV = UV.y;
        maxU = UV.x + WH.x;
        maxV = UV.y + WH.y;

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        switch(face) {
            case DOWN:
                renderer.pos(x1, y1, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                break;
            case UP:
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case NORTH:
                renderer.pos(x1, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
            case SOUTH:
                renderer.pos(x1, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case WEST:
                renderer.pos(x1, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case EAST:
                renderer.pos(x2, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
        }
        Tessellator.getInstance().draw();
    }

    /**
     * Utility method that draws a textured face of a cuboid based off of Tinkers' fluid rendering utility method
     * @param location The relative location (starting x,y,z)
     * @param dimensions The dimensions of the cuboid (ending x,y,z - starting x,y,z)
     * @param renderer BufferBuilder instance
     * @param sprite A texture atlas sprite instance
     * @param face The face of the cuboid to render
     * @param color The glColor color
     * @param flowing Whether the texture is a fluid flowing texture or not
     */
    static void drawTexturedQuad(Vector3f location, Vector3f dimensions, BufferBuilder renderer, TextureAtlasSprite sprite, EnumFacing face, Color color, boolean flowing) {
        if(sprite == null) return;
        double minU, maxU, minV, maxV;
        double size = 16f;
        if(flowing) size = 8f;
        double x1 = location.x;
        double x2 = dimensions.x;
        double y1 = location.y;
        double y2 = dimensions.y;
        double z1 = location.z;
        double z2 = dimensions.z;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        double xt1 = x1 % 1d;
        double xt2 = xt1 + 1d;
        while(xt2 > 1f) xt2 -= 1f;
        double yt1 = y1 % 1d;
        double yt2 = yt1 + 1d;
        while(yt2 > 1f) yt2 -= 1f;
        double zt1 = z1 % 1d;
        double zt2 = zt1 + 1d;
        while(zt2 > 1f) zt2 -= 1f;

        if(flowing) {
            double tmp = 1d - yt1;
            yt1 = 1d - yt2;
            yt2 = tmp;
        }

        switch(face) {
            case DOWN:
            case UP:
                minU = sprite.getInterpolatedU(xt1 * size);
                maxU = sprite.getInterpolatedU(xt2 * size);
                minV = sprite.getInterpolatedV(zt1 * size);
                maxV = sprite.getInterpolatedV(zt2 * size);
                break;
            case NORTH:
            case SOUTH:
                minU = sprite.getInterpolatedU(xt1 * size);
                maxU = sprite.getInterpolatedU(xt2 * size);
                minV = sprite.getInterpolatedV(yt1 * size);
                maxV = sprite.getInterpolatedV(yt2 * size);
                break;
            case WEST:
            case EAST:
                minU = sprite.getInterpolatedU(zt1 * size);
                maxU = sprite.getInterpolatedU(zt2 * size);
                minV = sprite.getInterpolatedV(yt1 * size);
                maxV = sprite.getInterpolatedV(yt2 * size);
                break;
            default:
                minU = sprite.getMinU();
                maxU = sprite.getMaxU();
                minV = sprite.getMinV();
                maxV = sprite.getMaxV();
        }

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        switch(face) {
            case DOWN:
                renderer.pos(x1, y1, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                break;
            case UP:
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case NORTH:
                renderer.pos(x1, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
            case SOUTH:
                renderer.pos(x1, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case WEST:
                renderer.pos(x1, y1, z1).tex(maxU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y1, z2).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z2).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x1, y2, z1).tex(maxU, minV).color(r, g, b, a).endVertex();
                break;
            case EAST:
                renderer.pos(x2, y1, z1).tex(minU, maxV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z1).tex(minU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y2, z2).tex(maxU, minV).color(r, g, b, a).endVertex();
                renderer.pos(x2, y1, z2).tex(maxU, maxV).color(r, g, b, a).endVertex();
                break;
        }
        Tessellator.getInstance().draw();
    }
}