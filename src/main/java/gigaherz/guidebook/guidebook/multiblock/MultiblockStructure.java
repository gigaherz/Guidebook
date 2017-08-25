package gigaherz.guidebook.guidebook.multiblock;

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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Quaternion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point2d;
import javax.vecmath.Point2i;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author joazlazer
 * <p>
 * Utility structure that aggregates related methods towards MultiblockStructure parsing and rendering
 */
public class MultiblockStructure
{
    // Structure variables
    private MultiblockComponent[][][] structureMatrix; // 3-d array containing a matrix of MultiblockComponents by position, filled with null by default
    private MultiblockComponent[][][] translucentStructureMatrix; // 3-d array containing a 3-d matrix of translucent blocks to be rendered second
    private StructureBlockAccess structureBlockAccess;
    private BlockPos bounds;

    // Hover variables
    private List<Pair<AxisAlignedBB, Vec3i>> tooltipBBs; // Each bounding box for mouse collision as well as the block-space position of the component
    private Vec3i hoveredPos = new Vec3i(-1, -1, -1);
    private boolean doRenderHighlight = false;
    private List<String> tooltipToRender = null;

    // Floor and Poles
    private MultiblockPanel.FloorMode floorMode;
    private MultiblockPanel.PoleMode poleMode;
    private Point2i[] floorLocations;
    private Vec3i[] poleLocations;

    // Initial render transforms
    private TRSRTransformation transformation;

    // Static constants
    private static final ResourceLocation FLOOR_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_floor.png");
    private static final ResourceLocation POLE_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "textures/multiblock_beacon.png");
    private static final float POLE_SCALE = 0.65f;
    private static final BlockPos DEFAULT_HOVER_POSITION = new BlockPos(-1, -1, -1);

    private MultiblockStructure(BlockPos size)
    {
        this.bounds = size;
        tooltipBBs = new ArrayList<>();
        structureBlockAccess = new StructureBlockAccess(size);
    }

    public BlockPos getBounds()
    {
        return bounds;
    }

    private IBlockAccess getBlockAccess()
    {
        return structureBlockAccess;
    }

    public void setTransformation(TRSRTransformation transformation)
    {
        this.transformation = transformation;
    }

    public void setPoleMode(MultiblockPanel.PoleMode poleMode)
    {
        this.poleMode = poleMode;
    }

    public void setFloorMode(MultiblockPanel.FloorMode floorMode)
    {
        this.floorMode = floorMode;
    }

    private void setStructureMatrix(MultiblockComponent[][][] structureMatrix)
    {
        this.structureMatrix = structureMatrix;
    }

    private void setTranslucentStructureMatrix(MultiblockComponent[][][] translucentStructureMatrix)
    {
        this.translucentStructureMatrix = translucentStructureMatrix;
    }

    /**
     * Draws the structure at the given position
     *
     * @param left            X coordinate to begin at
     * @param top             Y coordinate to begin at
     * @param blockScale      Interpolated and animated scale for each individual block
     * @param layerGap        Y-layer gap
     * @param maxDisplayLayer The maximum layer to display, hides others
     * @param globalScale     Interpolated and animated scale for the multiblock as a whole
     * @param spinAngle       Interpolated and animated angle for spinning the multiblock
     */
    public void render(int left, int top, float blockScale, float layerGap, int maxDisplayLayer, float globalScale, float spinAngle)
    {
        this.tooltipToRender = null;
        GlStateManager.pushMatrix();
        {
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

            // Set up OpenGL settings
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            GlStateManager.translate(left, top, 0f); // Screen-space transform to the specified x,y
            GlStateManager.translate(-18f, 0f, 200.0F); // Screen-space transform to the left 18 pixels (necessary arbitrary offset) and forward on the z-axis by 200 units

            GlStateManager.pushMatrix();
            {
                // Apply and scale out lighting
                GlStateManager.scale(24, 24, 24);
                RenderHelper.enableGUIStandardItemLighting();
            }
            GlStateManager.popMatrix();

            GlStateManager.scale(1.0F, -1.0F, 1.0F); // Screen-space flip on y-axis
            GlStateManager.scale(16.0F, 16.0F, 16.0F); // Screen-space scale by 16 units in each direction to make the multiblock appear larger
            applyGUITransformationMatrix(); // Screens-space -> Block-space

            // Block space transformations:
            GlStateManager.scale(2.0F, 2.0F, 2.0F); // Scale each block up by a hard 2x in each direction
            GlStateManager.scale(transformation.getScale().x, transformation.getScale().y, transformation.getScale().z); // Then, scale by the specified parsed scale in each direction
            GlStateManager.scale(globalScale, globalScale, globalScale); // Finally, scale by the interpolated value used for scaling down during expansion
            GlStateManager.rotate(spinAngle, 0f, 1f, 0f); // Rotate the structure by the interpolated spin angle
            //noinspection SuspiciousNameCombination
            GlStateManager.rotate(new Quaternion(transformation.getLeftRot().x, transformation.getLeftRot().y, transformation.getLeftRot().z, transformation.getLeftRot().w)); // Rotate by the specified parsed scale in each direction
            GlStateManager.translate(transformation.getTranslation().x, transformation.getTranslation().y, transformation.getTranslation().z); // Transform by the specified parsed offset

            renderPoles(layerGap, blockScale, maxDisplayLayer); // Draw the poles and lazily initialize if null
            renderFloor(layerGap); // Draw the floor and lazily initialize if null

            // Set texture manager settings
            textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);

            tooltipBBs.clear();
            renderComponents(structureMatrix, maxDisplayLayer, layerGap, blockScale); // Draw opaque blocks
            renderComponents(translucentStructureMatrix, maxDisplayLayer, layerGap, blockScale); // Draw translucent blocks

            FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
            FloatBuffer projection = BufferUtils.createFloatBuffer(16);
            IntBuffer viewport = BufferUtils.createIntBuffer(16);

            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

            Pair<Vector3f, Vector3f> mouseRay = getMouseRay(Mouse.getX(), Mouse.getY(), modelView, projection, viewport);
            if (mouseRay != null)
            {
                // Invert y-values for the mouse ray
                // TODO Mouse ray still not correct
                mouseRay.getKey().y = -mouseRay.getKey().y + 4.5f;
                mouseRay.getValue().y = -mouseRay.getValue().y + 4.5f;

                this.hoveredPos = getHoveredPosition(mouseRay, tooltipBBs);
                if (hoveredPos != null)
                {
                    doRenderHighlight = true;
                    MultiblockComponent hoveredComponent = this.getComponentAt(hoveredPos.getX(), hoveredPos.getY(), hoveredPos.getZ());
                    if (hoveredComponent != null) tooltipToRender = hoveredComponent.getTooltip();
                }
                else
                {
                    resetHover();
                }
            }

            if (doRenderHighlight)
                renderHighlight(blockScale, layerGap, maxDisplayLayer); // Draw the block highlight
            else resetHover();

            // Reset texture manager settings
            textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableAlpha();
            GlStateManager.disableRescaleNormal();

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();

            GlStateManager.popMatrix();
        }
    }

    /**
     * Draws the highlight for the specified component at the hovered position, and resets the highlight render state to be updated next frame if need be
     *
     * @param blockScale      Interpolated and animated scale for each individual block
     * @param layerGap        Y-layer gap
     * @param maxDisplayLayer The maximum layer to display, hides others
     */
    private void renderHighlight(float blockScale, float layerGap, int maxDisplayLayer)
    {
        MultiblockComponent hoveredComponent = getComponentAt(hoveredPos.getX(), hoveredPos.getY(), hoveredPos.getZ()); // If applicable, draw the hover highlight for the highlighted component
        if (hoveredComponent != null && hoveredPos.getY() + 1 <= maxDisplayLayer)
        {
            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap
            hoveredComponent.renderHighlight(hoveredPos.getX(), hoveredPos.getY() + offsetY + (layerGap * hoveredPos.getY()), hoveredPos.getZ(), blockScale);
        }
        doRenderHighlight = false;
    }

    /**
     * Renders tooltips and other mouse interactions and is called each frame
     *
     * @param info   A reference to a book graphics renderer
     * @param mouseX Mouse's screen-space x position
     * @param mouseY Mouse's screen-space y position
     */
    public void mouseOver(IBookGraphics info, int mouseX, int mouseY)
    {
        if (tooltipToRender != null)
        {
            info.drawHoverText(mouseX, mouseY, tooltipToRender);
        }
    }

    /**
     * Resets the position of the hovered component to (-1,-1,-1) in order for the highlight to not be rendered
     */
    private void resetHover()
    {
        this.hoveredPos = DEFAULT_HOVER_POSITION;
        this.doRenderHighlight = false;
    }

    /**
     * Utility method to convert between java vec-math and minecraft vec-math
     *
     * @param vecIn The java vec-math Vector
     * @return The minecraft vec-math Vector
     */
    private static Vec3d toVec3d(Vector3f vecIn)
    {
        return new Vec3d(vecIn.x, vecIn.y, vecIn.z);
    }

    /**
     * Gets a ray extending from the mouse position
     *
     * @param mouseX     Mouse's screen-space x position
     * @param mouseY     Mouse's screen-space y position
     * @param modelView  The model and view transformation matrices (combined to one)
     * @param projection The projection transformation matrix
     * @param viewport   The current viewport
     * @return A pair of 3-d vectors representing the Start & End coordinates of the ray
     */
    @Nullable
    private static Pair<Vector3f, Vector3f> getMouseRay(int mouseX, int mouseY, FloatBuffer modelView, FloatBuffer projection, IntBuffer viewport)
    {
        // TODO Mouse ray still not correct
        if (modelView == null || projection == null || viewport == null) return null;
        float winX, winY;
        FloatBuffer startPos = BufferUtils.createFloatBuffer(3);
        FloatBuffer endPos = BufferUtils.createFloatBuffer(3);
        winX = (float) mouseX;
        winY = (float) viewport.get(3) - (float) mouseY;
        GLU.gluUnProject(winX, winY, 0f, modelView, projection, viewport, startPos); // Gets point on near clipping plane
        GLU.gluUnProject(winX, winY, 1f, modelView, projection, viewport, endPos); // Gets point on far clipping plane
        return new Pair<>(new Vector3f(startPos.get(0), startPos.get(1), startPos.get(2)), new Vector3f(endPos.get(0), endPos.get(1), endPos.get(2)));
    }

    /**
     * Gets the structure coordinates of the hovered MultiblockComponent
     *
     * @param mouseRay           A pair of points representing (start, end) for the test ray
     * @param hoverBoundingBoxes A list of pairs of bounding boxes and the structure coordinates they came from
     * @return The structure coordinates of the currently hovered component, or null if nothing is being hovered
     */
    @Nullable
    private static Vec3i getHoveredPosition(@Nonnull Pair<Vector3f, Vector3f> mouseRay, List<Pair<AxisAlignedBB, Vec3i>> hoverBoundingBoxes)
    {
        List<Pair<RayTraceResult, Vec3i>> results = new ArrayList<>();
        for (Pair<AxisAlignedBB, Vec3i> hoverBoundingBox : hoverBoundingBoxes)
        {
            RayTraceResult result = hoverBoundingBox.getKey().calculateIntercept(toVec3d(mouseRay.getKey()), toVec3d(mouseRay.getValue()));
            if (result != null && result.typeOfHit != RayTraceResult.Type.MISS)
            {
                results.add(new Pair<>(result, hoverBoundingBox.getValue()));
            }
        }

        if (results.size() != 0)
        {
            Pair<RayTraceResult, Vec3i> minDistance = results.get(0);
            for (Pair<RayTraceResult, Vec3i> result : results)
            {
                if (result == minDistance) continue;
                if (minDistance.getKey().hitVec.squareDistanceTo(toVec3d(mouseRay.getKey())) > result.getKey().hitVec.squareDistanceTo(toVec3d(mouseRay.getKey())))
                    minDistance = result;
            }
            return minDistance.getValue();
        }
        return null;
    }

    /**
     * Draws thin opaque beacon-like poles according to the PoleMode
     *
     * @param layerGap        Y-layer gap
     * @param blockScale      Interpolated and animated scale for each individual block
     * @param maxDisplayLayer The maximum layer to display, hides others
     */
    private void renderPoles(float layerGap, float blockScale, int maxDisplayLayer)
    {
        GlStateManager.pushMatrix();
        {
            GlStateManager.disableLighting(); // Disable lighting for pole rendering

            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap
            Vector3f tileSize = new Vector3f(0f, 1f + layerGap, 1f); // Create a plane the size of one block plus the layer gap
            Vector3f tileLoc = new Vector3f(0f, 0f, 0f);
            BufferBuilder renderer = Tessellator.getInstance().getBuffer();
            Point2d uv = new Point2d(0d, 0d);
            Point2d wh = new Point2d(1d, 1d);
            Color poleColor = Color.white;

            GlStateManager.translate(0f, offsetY, 0f);
            for (Vec3i polePoint : getPoleLocations())
            {
                if (polePoint.getY() + 2 <= maxDisplayLayer)
                { // If the current layer should be rendered according to the display layer slider
                    GlStateManager.pushMatrix();
                    {
                        GlStateManager.translate(polePoint.getX(), polePoint.getY() + (polePoint.getY() * layerGap), polePoint.getZ()); // Offset by the pole's location and then by the layer gap offset at that point
                        GlStateManager.scale(blockScale, blockScale, blockScale); // Scale in each direction by the current block scale
                        GlStateManager.scale(POLE_SCALE, 1f, POLE_SCALE); // Scale in x-z by a constant scale
                        GlStateManager.translate(0f, 0f, -0.5F); // Translate by negative half a block in the z to prepare for rendering the CrossModel
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.EAST, poleColor); // Draw forward-facing plane
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.WEST, poleColor); // Draw backward-facing plane
                        GlStateManager.translate(0f, 0f, +0.5F); // Reset that translation
                        GlStateManager.translate(-0.5F, 0, 0F); // Move the plane into a new position for the other cross plane
                        GlStateManager.rotate(90f, 0f, 1f, 0f); // Rotate by 90 degrees into the new position
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.EAST, poleColor); // Draw forward-facing plane
                        drawTexturedQuad(tileLoc, tileSize, renderer, POLE_TEXTURE, uv, wh, EnumFacing.WEST, poleColor); // Draw backward-facing plane
                    }
                    GlStateManager.popMatrix();
                }
            }
            GlStateManager.enableLighting(); // Re-enable lighting
        }
        GlStateManager.popMatrix();
    }

    /**
     * Draws a thin grid-like flooring according to the FloorMode
     *
     * @param layerGap Y-layer gap
     */
    private void renderFloor(float layerGap)
    {
        GlStateManager.pushMatrix();
        {
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
            for (Point2i tilePoint : getFloorLocations())
            {
                GlStateManager.pushMatrix();
                {
                    GlStateManager.translate(tilePoint.x, offsetY, tilePoint.y); // Offset to the tile block position and offset by the calculated y offset
                    GlStateManager.translate(-0.5F, -0.5F, -0.5F); // Offset to the back corner of the block position
                    drawTexturedQuad(tileLoc, tileSize, renderer, FLOOR_TEXTURE, uv, wh, EnumFacing.UP, tileColor); // Draw up-facing plane
                    drawTexturedQuad(tileLoc, tileSize, renderer, FLOOR_TEXTURE, uv, wh, EnumFacing.DOWN, tileColor); // Draw down-facing plane
                }
                GlStateManager.popMatrix();
            }
            GlStateManager.enableLighting();
        }
        GlStateManager.popMatrix();
    }

    /**
     * Draws each MultiblockComponent in the given componentMatrix
     *
     * @param componentMatrix A 3-dimensional jagged array representing a matrix of MultiblockComponents
     * @param maxDisplayLayer The maximum layer to display, hides others
     * @param layerGap        Y-layer gap
     * @param blockScale      Interpolated and animated scale for each individual block
     */
    private void renderComponents(MultiblockComponent[][][] componentMatrix, int maxDisplayLayer, float layerGap, float blockScale)
    {
        GlStateManager.pushMatrix();
        {
            final float offsetY = -layerGap * (bounds.getY() - 1) / 2f; // Calculate the y offset to account for expansion and move back down by half of the amount moved up by the layer gap

            // Render each component
            for (int x = 0; x < componentMatrix.length; ++x)
            { // Loop through each x and y array
                for (int y = 0; y < componentMatrix[x].length; ++y)
                {
                    if (y + 1 <= maxDisplayLayer)
                    { // If the current layer should be rendered according to the display layer slider
                        for (int z = 0; z < componentMatrix[x][y].length; ++z)
                        {
                            if (componentMatrix[x][y][z] != null)
                            { // Ensure there isn't air at the position
                                tooltipBBs.add(new Pair<>(componentMatrix[x][y][z].render(x, y + offsetY + (layerGap * y), z, blockScale), new Vec3i(x, y, z))); // Only add tooltip bounds to visible elements
                            }
                        }
                    }
                }
            }
        }
        GlStateManager.popMatrix();
    }

    /**
     * Apply a transformation matrix that converts screens-space to block/world-space
     * Transforms it into an orthographic isometric style with each X,Y,Z face showing
     * Mimics the transform of ItemStacks rendered in GUIContainers
     */
    private void applyGUITransformationMatrix()
    {
        TRSRTransformation transformation = TRSRTransformation.blockCenterToCorner(new TRSRTransformation(new Vector3f(0, 0, 0), TRSRTransformation.quatFromXYZDegrees(new Vector3f(30, 225, 0)), new Vector3f(0.625f, 0.625f, 0.625f), null)); // Hard-coded copy of the standard GUI matrix
        Matrix4f mat = null;
        if (!transformation.equals(TRSRTransformation.identity())) mat = transformation.getMatrix();
        ForgeHooksClient.multiplyCurrentGlMatrix(mat);
    }

    /**
     * Lazily initializes floor locations according to the parsed FloorMode
     *
     * @return An array of locations to render a floor tile at
     */
    private Point2i[] getFloorLocations()
    {
        if (floorLocations != null) return floorLocations;
        List<Point2i> floorLocationList = new ArrayList<>();
        switch (floorMode)
        {
            case GRID:
            { // Adds one tile under each x,z coordinate, filled with blocks above or not
                for (int x = 0; x < getBounds().getX(); ++x)
                {
                    for (int z = 0; z < getBounds().getZ(); ++z)
                    {
                        floorLocationList.add(new Point2i(x, z));
                    }
                }
                break;
            }
            case UNDER:
            { // Adds one tile under each x,z coordinate that has at least one non-air block in the column
                for (int x = 0; x < getBounds().getX(); ++x)
                {
                    for (int z = 0; z < getBounds().getZ(); ++z)
                    {
                        for (int y = 0; y < getBounds().getY(); ++y)
                        {
                            if (hasBlockAt(x, y, z))
                            {
                                floorLocationList.add(new Point2i(x, y));
                                break;
                            }
                        }
                    }
                }
                break;
            }
            case ADJACENT:
            { // Adds one tile under each x,z coordinate that has at least one non-air block in the column, and then 4 additional tiles to each side of that tile
                boolean[][] floorInPos = new boolean[getBounds().getX() + 2][getBounds().getZ() + 2];
                for (int x = 0; x < getBounds().getX(); ++x)
                {
                    for (int z = 0; z < getBounds().getZ(); ++z)
                    {
                        for (int y = 0; y < getBounds().getY(); ++y)
                        {
                            if (hasBlockAt(x, y, z))
                            {
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
                for (int x = 0; x < floorInPos.length; ++x)
                {
                    for (int z = 0; z < floorInPos[x].length; ++z)
                    {
                        if (floorInPos[x][z]) floorLocationList.add(new Point2i(x - 1, z - 1));
                    }
                }
                break;
            }
            case AROUND:
            { // Adds one tile under each x,z coordinate that has at least one non-air block in the column, and then 8 additional tiles to each side and corner of that tile
                boolean[][] floorInPos = new boolean[getBounds().getX() + 2][getBounds().getZ() + 2];
                for (int x = 0; x < getBounds().getX(); ++x)
                {
                    for (int z = 0; z < getBounds().getZ(); ++z)
                    {
                        for (int y = 0; y < getBounds().getY(); ++y)
                        {
                            if (hasBlockAt(x, y, z))
                            {
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
                for (int x = 0; x < floorInPos.length; ++x)
                {
                    for (int z = 0; z < floorInPos[x].length; ++z)
                    {
                        if (floorInPos[x][z]) floorLocationList.add(new Point2i(x - 1, z - 1));
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
     *
     * @return An array of locations to render a pole beacon at
     */
    private Vec3i[] getPoleLocations()
    {
        if (poleLocations != null) return poleLocations;
        List<Vec3i> poleLocationList = new ArrayList<>();
        switch (poleMode)
        {
            case ON:
            { // If the multiblock is larger than 3 in both x and z directions, add one pole in each corner starting from the bottom and continuing up as long as a non-air block exists above
                if (getBounds().getY() >= 3 && getBounds().getX() >= 3)
                {
                    poleLocationList.addAll(getPoles(0, 0));
                    poleLocationList.addAll(getPoles(getBounds().getX() - 1, 0));
                    poleLocationList.addAll(getPoles(getBounds().getX() - 1, getBounds().getZ() - 1));
                    poleLocationList.addAll(getPoles(0, getBounds().getZ() - 1));
                }
            }
            case BELOW_ITEMS:
            { // Adds one pole for each y-position in a column that contains an Item by going down and adding poles once an item has been found
                for (int x = 0; x < getBounds().getX(); ++x)
                {
                    for (int z = 0; z < getBounds().getZ(); ++z)
                    {
                        boolean itemAbove = false;
                        for (int y = getBounds().getY() - 1; y >= 0; --y)
                        {
                            if (itemAbove)
                            {
                                poleLocationList.add(new Vec3i(x, y, z));
                            }

                            if (hasComponentAt(x, y, z))
                            {
                                MultiblockComponent component = getComponentAt(x, y, z);
                                if (component instanceof ItemComponent) itemAbove = true;
                            }
                        }
                    }
                }
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
     *
     * @param x multiblock x-position
     * @param z multiblock z-position
     * @return A list of valid x,y,z block positions of poles
     */
    private Collection<? extends Vec3i> getPoles(int x, int z)
    {
        List<Vec3i> poleLocationList = new ArrayList<>();
        for (int y = 0; y < getBounds().getY() - 1; ++y)
        {
            if (getBlockAt(x, y, z) != null && getBlockAt(x, y + 1, z) != null)
            {
                poleLocationList.add(new Vec3i(x, y, z));
            }
            else break;
        }
        return poleLocationList;
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return Whether there is a block at the given x,y,z coordinate in the multiblock structure
     */
    private boolean hasBlockAt(int x, int y, int z)
    {
        // TODO Fix with the translucent block restructure
        return (x < this.bounds.getX() && y < this.bounds.getY() && z < this.getBounds().getZ() &&
                x > -1 && y > -1 && z > -1 &&
                ((structureMatrix[x][y][z] != null && structureMatrix[x][y][z] instanceof BlockComponent) ||
                        (translucentStructureMatrix[x][y][z] != null && translucentStructureMatrix[x][y][z] instanceof BlockComponent)));
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return Whether there is a component at the given x,y,z coordinate in the multiblock structure
     */
    private boolean hasComponentAt(int x, int y, int z)
    {
        return (x < this.bounds.getX() && y < this.bounds.getY() && z < this.getBounds().getZ() && x > -1 && y > -1 && z > -1 && (structureMatrix[x][y][z] != null || translucentStructureMatrix[x][y][z] != null));
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return The BlockComponent at the given x,y,z coordinate, or <code>null</code> if none exist there
     */
    private BlockComponent getBlockAt(int x, int y, int z)
    {
        // TODO Fix with the translucent block restructure
        if (!hasBlockAt(x, y, z)) return null;
        if (structureMatrix[x][y][z] != null && structureMatrix[x][y][z] instanceof BlockComponent)
            return (BlockComponent) structureMatrix[x][y][z];
        if (translucentStructureMatrix[x][y][z] != null && translucentStructureMatrix[x][y][z] instanceof BlockComponent)
            return (BlockComponent) translucentStructureMatrix[x][y][z];
        return null;
    }

    /**
     * @param x relative x
     * @param y relative y
     * @param z relative z
     * @return The MultiblockComponent at the given x,y,z coordinate, or <code>null</code> if none exist there
     */
    private MultiblockComponent getComponentAt(int x, int y, int z)
    {
        if (!hasComponentAt(x, y, z)) return null;
        if (structureMatrix[x][y][z] != null)
            return structureMatrix[x][y][z];
        if (translucentStructureMatrix[x][y][z] != null)
            return translucentStructureMatrix[x][y][z];
        return null;
    }

    /**
     * Attempts to find the structure nbt file at the given resource location and parse it, creating a new MultiblockStructure object
     *
     * @param structureRL The resource location of the file within assets/<domain>/structures/<path>.nbt
     * @return The new Multiblock structure object, or null if parsing failed
     */
    @Nullable
    public static MultiblockStructure tryParse(@Nonnull ResourceLocation structureRL)
    {
        Template blockTemplate;
        // Try and retrieve the template from file
        {
            String domain = structureRL.getResourceDomain();
            String path = structureRL.getResourcePath();
            InputStream inputstream = null;

            try
            {
                inputstream = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(domain, "structures/" + path + ".nbt")).getInputStream();
                NBTTagCompound nbttagcompound = CompressedStreamTools.readCompressed(inputstream);
                blockTemplate = new Template();
                blockTemplate.read(nbttagcompound);
            } catch (Throwable ex)
            {
                blockTemplate = null; // Ensure nothing is loaded
            } finally
            {
                IOUtils.closeQuietly(inputstream);
            }
        }
        if (blockTemplate != null)
        {
            // Retrieve the list of blocks via Reflection
            List<Template.BlockInfo> blocks = ReflectionHelper.getPrivateValue(Template.class, blockTemplate, "blocks");

            // Initialize the multiblock structure and its component matrices
            MultiblockStructure structure = new MultiblockStructure(blockTemplate.getSize());
            MultiblockComponent[][][] structureMatrix = new MultiblockComponent[structure.getBounds().getX()][structure.getBounds().getY()][structure.getBounds().getZ()];
            MultiblockComponent[][][] translucentStructureMatrix = new MultiblockComponent[structure.getBounds().getX()][structure.getBounds().getY()][structure.getBounds().getZ()];

            structure.setStructureMatrix(structureMatrix);
            structure.setTranslucentStructureMatrix(translucentStructureMatrix);

            // Loop through each block in the structure
            for (Template.BlockInfo block : blocks)
            {
                BlockComponent component;
                component = makeBlockComponent(block.blockState, structure.getBlockAccess(), block.pos); // Retrieve the correct block component type instance
                structure.addComponent(component, block.pos.getX(), block.pos.getY(), block.pos.getZ());
            }

            return structure;

        }
        else
        {
            GuidebookMod.logger.warn(String.format("Structure '%s.nbt' not found in assets/%s/structures/! Ignoring!", structureRL.getResourcePath(), structureRL.getResourceDomain()));
            return null;
        }
    }

    public void addComponent(MultiblockComponent component, int x, int y, int z)
    {
        // If the component is a block and is air, set it to null (but still add it)
        if (component instanceof BlockComponent && ((BlockComponent) component).getBlockState().getBlock() == Blocks.AIR)
            component = null;

        if (component != null && component.isTranslucent())
        {
            // TODO Redo with translucency restructure
            translucentStructureMatrix[x][y][z] = component;
            structureMatrix[x][y][z] = null;
        }
        else
        {
            structureMatrix[x][y][z] = component;
            translucentStructureMatrix[x][y][z] = null;
        }

        // Adds the component to the IBlockAccess if it is a block
        if (component instanceof BlockComponent)
        {
            this.structureBlockAccess.getBlockComponents()[x][y][z] = (BlockComponent) component;
        }
    }

    /**
     * Creates a BlockComponent based off of the BlockState's block
     * Uses BlockComponent.Factory.registry to create mapped instances
     *
     * @param blockState The block state that was parsed from the template
     * @return A specialized BlockComponent subclass if a registered Factory has the block mapped, or a BlockComponent if not
     */
    private static BlockComponent makeBlockComponent(IBlockState blockState, IBlockAccess blockAccess, BlockPos position)
    {
        // Loop through each factory
        for (BlockComponent.Factory componentFactory : BlockComponent.Factory.registry.getValues())
        {
            // Loop through each of its class mappings
            for (Class<?> blockClass : componentFactory.getMappings())
            {
                // If the BlockState's block is an instance of one of the class mappings, create a new instance of that type of BlockComponent and return it
                if (blockClass.isInstance(blockState.getBlock()))
                {
                    return componentFactory.create(blockState, blockAccess, position);
                }
            }
        }
        // If no mapping was needed, use the default BlockComponent
        return new BlockComponent(blockState, blockAccess, position);
    }

    /**
     * Utility method that draws a textured face of a cuboid based off of Tinkers' fluid rendering utility method
     *
     * @param location   The relative location (starting x,y,z)
     * @param dimensions The dimensions of the cuboid (ending x,y,z - starting x,y,z)
     * @param renderer   BufferBuilder instance
     * @param texture    texture location
     * @param UV         u,v coordinates on texture
     * @param WH         uWidth, vHeight on texture
     * @param face       The face of the cuboid to render
     * @param color      The glColor color
     */
    static void drawTexturedQuad(Vector3f location, Vector3f dimensions, BufferBuilder renderer, ResourceLocation texture, Point2d UV, Point2d WH, EnumFacing face, Color color)
    {
        if (UV == null || WH == null) return;
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
        switch (face)
        {
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
     *
     * @param location   The relative location (starting x,y,z)
     * @param dimensions The dimensions of the cuboid (ending x,y,z - starting x,y,z)
     * @param renderer   BufferBuilder instance
     * @param sprite     A texture atlas sprite instance
     * @param face       The face of the cuboid to render
     * @param color      The glColor color
     * @param flowing    Whether the texture is a fluid flowing texture or not
     */
    static void drawTexturedQuad(Vector3f location, Vector3f dimensions, BufferBuilder renderer, TextureAtlasSprite sprite, EnumFacing face, Color color, boolean flowing)
    {
        if (sprite == null) return;
        double minU, maxU, minV, maxV;
        double size = 16f;
        if (flowing) size = 8f;
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
        while (xt2 > 1f) xt2 -= 1f;
        double yt1 = y1 % 1d;
        double yt2 = yt1 + 1d;
        while (yt2 > 1f) yt2 -= 1f;
        double zt1 = z1 % 1d;
        double zt2 = zt1 + 1d;
        while (zt2 > 1f) zt2 -= 1f;

        if (flowing)
        {
            double tmp = 1d - yt1;
            yt1 = 1d - yt2;
            yt2 = tmp;
        }

        switch (face)
        {
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
        switch (face)
        {
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
