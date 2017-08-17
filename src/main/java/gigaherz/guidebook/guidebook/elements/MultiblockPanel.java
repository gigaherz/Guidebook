package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.sun.javafx.geom.Vec3f;
import com.sun.javafx.geom.Vec4f;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.ParseUtils;
import gigaherz.guidebook.guidebook.client.BookRendering;
import gigaherz.guidebook.guidebook.multiblock.MultiblockComponent;
import gigaherz.guidebook.guidebook.multiblock.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.Color;
import java.util.ArrayList;

/**
 * @author joazlazer
 *
 * A page element that supports the display of complex multiblock structures within guidebooks
 * (See in-game manual)
 */
public class MultiblockPanel implements IHoverPageElement, IClickablePageElement, ITickable {
    private int height = 150; // Default height
    private PoleMode poles = PoleMode.ON;
    private FloorMode floor = FloorMode.ADJACENT;
    private Vec3f offset = new Vec3f(0f, 0f, 0f); // A block-space offset for the structure to become the new render origin
    private Vec4f initialRot = new Vec4f(0f, 0f, 0f, 0f); // A block-space rotation for the structure to be transformed before it is spun / orbited
    private MultiblockStructure[] structures;
    private boolean expanded = false;
    private boolean modeButtonEnabled = true;
    private boolean layerSelectEnabled = true;
    private float scale = 1f;
    private float collapsedScale = 1.2f;
    private float expandScale = 0.65f;
    private float expandLevelGap = 1f;
    private float spinSpeed = 0f;

    public MultiblockPanel() {
        upBounds = new Rectangle();
        downBounds = new Rectangle();
        modeBounds = new Rectangle();
    }

    private Rectangle bounds;
    private Rectangle upBounds;
    private Rectangle downBounds;
    private Rectangle modeBounds;
    private boolean upEnabled = false;
    private boolean downEnabled = true;
    private boolean expanding = false;
    private boolean collapsing = false;
    private int modeSwitchTicks = 0;
    private int maxDisplayLayer = 0;
    private int spinAngle = 0;

    private static final int CYCLE_TIME = 2000;
    private static final ResourceLocation BOOK_GUI_TEXTURES = GuidebookMod.location("gui/book");
    private static final int MODE_SWITCH_MAX = 7;
    private static final int BUTTON_PANEL_RIGHT_OFFSET = 16;
    private static final int BUTTON_PANEL_WIDTH = 23;

    private float getCollapsedScale() {
        return MathHelper.clamp(collapsedScale - 1f, 0f, 8f);
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top) {
        // Update bounds
        bounds = new Rectangle(left, top, BookRendering.DEFAULT_BOOK_WIDTH, height);
        if(getCurrentStructure() != null) {
            updateLayerSelectLogic();
            renderStructure(getCurrentStructure(), nav,left + (nav.getPageWidth() / 2), top + (height / 2));
            int buttonY = top + (height / 2) - (getButtonPanelHeight() / 2) - 4;
            if(layerSelectEnabled) {
                buttonY = drawUpButton(nav, left, buttonY);
                buttonY = drawLevelText(nav, left, buttonY);
                buttonY = drawDownButton(nav, left, buttonY);
            }
            if(modeButtonEnabled) {
                drawModeButton(nav, left, buttonY);
            }
        }
        return height;
    }

    private int getButtonPanelHeight() {
        int end = 0;
        if(layerSelectEnabled) end += 74;
        if(modeButtonEnabled) end += 36;
        return end;
    }

    private void updateLayerSelectLogic() {
        assert getCurrentStructure() != null; // Sanity check
        // If out of bounds or at bound, clamp position and disable movement button towards that direction
        // Else, re-enable that movement button
        if(maxDisplayLayer >= getCurrentStructure().getBounds().getY()) {
            maxDisplayLayer = getCurrentStructureHeight();
            upEnabled = false;
        } else upEnabled = true;
        if(maxDisplayLayer <= 1) {
            maxDisplayLayer = 1;
            downEnabled = false;
        } else downEnabled = true;
    }

    @Override
    public void update() {
        // Add ticks towards expanding or collapsing
        if(expanding) {
            collapsing = false;
            ++modeSwitchTicks;
            if(modeSwitchTicks >= MODE_SWITCH_MAX) {
                // If done with expanding, stop and ensure ticks is clamped
                modeSwitchTicks = MODE_SWITCH_MAX;
                expanding = false;
                expanded = true;
            }
        } else if(collapsing) {
            expanding = false;
            --modeSwitchTicks;
            if(modeSwitchTicks <= 0) {
                // If done with collapsing, stop and ensure ticks is clamped
                modeSwitchTicks = 0;
                collapsing = false;
                expanded = false;
            }
        }

        spinAngle += spinSpeed;
    }

    private int drawUpButton(IBookGraphics nav, int left, int top) {
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        upBounds = new Rectangle(x, top, 20, 18);
        int u = 4;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), upBounds)) u += 25;
        if(!upEnabled) u = 54;
        nav.drawImage(BOOK_GUI_TEXTURES, x, top, u, 106, 10, 9, 256, 256, 2f);
        return top + upBounds.getHeight() + 6;
    }

    private int drawDownButton(IBookGraphics nav, int left, int top) {
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        downBounds = new Rectangle(x, top, 20, 18);
        int u = 4;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), downBounds)) u += 25;
        if(!downEnabled) u = 54;
        nav.drawImage(BOOK_GUI_TEXTURES, x, top, u, 117, 10, 9, 256, 256, 2f);
        return top + downBounds.getHeight() + 10;
    }

    private void drawModeButton(IBookGraphics nav, int left, int top) {
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET - 1;
        modeBounds = new Rectangle(x, top, 23, 26);
        int u = 54;
        int v = 2;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), modeBounds)) u += 27;
        if(!expanded) v += 28;
        nav.drawImage(BOOK_GUI_TEXTURES, x, top, u, v, 23, 26, 256, 256, 1f);
    }

    private int drawLevelText(IBookGraphics nav, int left, int top) {
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        Color numberColor = new Color(15, 15, 15, 255);
        top += drawCenteredText(nav, x, top, BUTTON_PANEL_WIDTH, Integer.toString(maxDisplayLayer), numberColor);
        top += 2;
        top += drawCenterBar(nav, x, top, BUTTON_PANEL_WIDTH);
        top += 5;
        top += drawCenteredText(nav, x, top, BUTTON_PANEL_WIDTH, Integer.toString(getCurrentStructureHeight()), numberColor);
        return top + 3;
    }

    private int drawCenterBar(IBookGraphics nav, int x, int y, int panelWidth) {
        x += panelWidth / 2;
        x -= 12;
        nav.drawImage(BOOK_GUI_TEXTURES, x, y, 104, 0, 12, 1, 256, 256, 2f);
        return 2;
    }

    private int drawCenteredText(IBookGraphics nav, int x, int y, int panelWidth, String text, Color color) {
        x += panelWidth / 2;
        x -= Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2;
        return nav.addStringWrapping(x, y, text, color.getRGB(), 0, 1f );
    }

    private void renderStructure(MultiblockStructure structure, IBookGraphics nav, int left, int top) {
        float expandBlockScale, expandLevelAmount, globalScale;
        if(expanding || collapsing) {
            final float expandMu = MathHelper.clamp((modeSwitchTicks + nav.getPartialTicks()) / MODE_SWITCH_MAX, 0f, 1f);
            final float expandSinAlpha = getSinInterp(expandMu, 0f, 1f);
            expandBlockScale = 1f - (expandSinAlpha * (1f - expandScale));
            expandLevelAmount = expandSinAlpha * expandLevelGap;
            globalScale = 1f + getCollapsedScale() * (1f - expandSinAlpha);
        } else {
            if(expanded) {
                expandBlockScale = expandScale;
                expandLevelAmount = expandLevelGap;
                globalScale = 1f;
            } else {
                expandBlockScale = 1f;
                expandLevelAmount = 0f;
                globalScale = 1f + getCollapsedScale();
            }
        }
        structure.render(left, top, expandBlockScale, expandLevelAmount, maxDisplayLayer, globalScale, spinAngle + (nav.getPartialTicks() * spinSpeed));
    }

    @Override
    public void parse(NamedNodeMap attributes) {
        Node attr = attributes.getNamedItem("height");
        if (attr != null) {
            Integer parsedHeight = Ints.tryParse(attr.getTextContent());
            if(parsedHeight != null) height = parsedHeight;
        }

        attr = attributes.getNamedItem("scale");
        if (attr != null) {
            Float parsedScale = Floats.tryParse(attr.getTextContent());
            if(parsedScale != null) scale = parsedScale;
        }

        attr = attributes.getNamedItem("poles");
        if (attr != null) {
            poles = PoleMode.parse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("floor");
        if (attr != null) {
            floor = FloorMode.parse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("offset");
        if (attr != null) {
            Vec3f parsedOffset = ParseUtils.parseVec3f(attr.getTextContent());
            if(parsedOffset != null) this.offset = parsedOffset;
        }

        attr = attributes.getNamedItem("initialRot");
        if (attr != null) {
            Vec4f parsedRot = ParseUtils.parseVec4f(attr.getTextContent());
            if(parsedRot != null) this.initialRot = parsedRot;
        }

        // TODO additional attributes

        attr = attributes.getNamedItem("structures");
        if (attr != null) {
            String[] structures = ParseUtils.parseArray(attr.getTextContent());
            this.parseStructures(structures);
        }
    }

    public void parseChildren(NodeList childNodes) {
        ArrayList<MultiblockComponent> componentList = new ArrayList<>();
        for(int i = 0; i < childNodes.getLength(); ++i) {
            Node componentNode = childNodes.item(i);
            // TODO parse items
        }
    }

    private void parseStructures(String[] structureArray) {
        ArrayList<MultiblockStructure> loadedStructures = new ArrayList<>();
        for (String aStructureArray : structureArray) {
            ResourceLocation structureRL = new ResourceLocation(aStructureArray.trim());
            MultiblockStructure newStructure = MultiblockStructure.tryParse(structureRL);
            if (newStructure != null) {
                loadedStructures.add(newStructure);
                newStructure.setOffset(this.offset);
                newStructure.setInitialRot(this.initialRot);
                newStructure.setScale(this.scale);
                newStructure.setFloorMode(this.floor);
                newStructure.setPoleMode(this.poles);
                this.maxDisplayLayer = newStructure.getBounds().getY(); // Initialize the maxDisplayLayer to the max y of the last structure loaded
            }
        }
        this.structures = loadedStructures.toArray(new MultiblockStructure[loadedStructures.size()]);
    }

    @Override
    public IPageElement copy() {
        MultiblockPanel newMP = new MultiblockPanel();
        // TODO complete copy()
        return newMP;
    }

    @Override
    public void click(IBookGraphics nav) {
        if(inBounds(nav.getMouseX(), nav.getMouseY(), upBounds)) {
            // Update displayed layer by moving it up
            if(maxDisplayLayer < getCurrentStructureHeight()) ++maxDisplayLayer;
        } else if(inBounds(nav.getMouseX(), nav.getMouseY(), downBounds)) {
            // Update displayed layer by moving it down
            if(maxDisplayLayer > 1) --maxDisplayLayer;
        } else if(inBounds(nav.getMouseX(), nav.getMouseY(), modeBounds) && modeButtonEnabled) {
            // Switch modes and begin the collapsing/expanding animation
            if(expanded) {
                collapsing = true;
            } else {
                expanding = true;
            }
        } else {
            // TODO drag orbit
        }
    }

    @Override
    public void mouseOver(IBookGraphics info, int x, int y) {
        // TODO hover tooltips
    }

    @Override
    public Rectangle getBounds() {
        if(bounds == null) {
            return new Rectangle();
        } else return bounds;
    }

    private boolean inBounds(int mouseX, int mouseY, Rectangle bounds) {
        return mouseX >= bounds.getX() && mouseY >= bounds.getY() && mouseX < bounds.getX() + bounds.getWidth() && mouseY < bounds.getY() + bounds.getHeight();
    }

    private MultiblockStructure getCurrentStructure() {
        if (structures == null || structures.length == 0)
            return null;
        long time = System.currentTimeMillis();
        return structures[(int) ((time / CYCLE_TIME) % structures.length)];
    }

    private int getCurrentStructureHeight() {
        return (getCurrentStructure() == null) ? 1 : getCurrentStructure().getBounds().getY();
    }

    /**
     * Util method that returns a number between y1 and y2 according to an interpolation based off of the transformed sine value at mu
     * @param mu Between [0.0f, 1.0f]
     * @param y1 The y value at mu=0f
     * @param y2 The y value at mu=1f
     * @return Between [y1, y2]
     */
    @SuppressWarnings("SameParameterValue")
    private float getSinInterp(float mu, float y1, float y2) {
        mu = net.minecraft.util.math.MathHelper.clamp(mu, 0.0f, 1.0f);
        // Calculate the alpha value at mu
        float alpha = 0.5f * (float)Math.sin(((3.142f * mu) - 1.571f)) + 0.5f;
        return (y2 - y1) * alpha + y1;
    }

    public enum PoleMode {
        OFF, ON, BELOW_ITEMS;
        private static PoleMode parse(String string) {
            if(string.equalsIgnoreCase("corners")) return ON;
            else if(string.equalsIgnoreCase("below_items")) return BELOW_ITEMS;
            else return OFF; // default
        }
    }

    public enum FloorMode {
        OFF, UNDER, ADJACENT, GRID, AROUND;
        private static FloorMode parse(String string) {
            if(string.equalsIgnoreCase("grid")) return GRID;
            else if(string.equalsIgnoreCase("under")) return UNDER;
            else if(string.equalsIgnoreCase("adjacent")) return ADJACENT;
            else if(string.equalsIgnoreCase("around")) return AROUND;
            else return OFF; // default
        }
    }
}
