package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import com.sun.javafx.geom.Vec3f;
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
    private PoleMode poles = PoleMode.OFF;
    private FloorMode floor = FloorMode.GRID;
    private Vec3f offset = new Vec3f(0f, 0f, 0f); // A block-space offset for the structure to become the new render origin
    private MultiblockStructure[] structures;
    private boolean expanded = false;
    private boolean modeButtonEnabled = true;
    private boolean layerSelectEnabled = true;
    private float expandScale = 0.65f;
    private float expandLevelGap = 1.2f;

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

    private static int CYCLE_TIME = 2000;
    private static final ResourceLocation BOOK_GUI_TEXTURES = GuidebookMod.location("gui/book");
    private static int MODE_SWITCH_MAX = 10;
    private static int BUTTON_PANEL_RIGHT_OFFSET = 16;
    private static int BUTTON_PANEL_WIDTH = 23;

    @Override
    public int apply(IBookGraphics nav, int left, int top) {
        // Update bounds
        bounds = new Rectangle(left, top, BookRendering.DEFAULT_BOOK_WIDTH, height);
        if(getCurrentStructure() != null) {
            updateLayerSelectLogic();
            renderStructure(getCurrentStructure(), nav,left + (nav.getPageWidth() / 2), top + (height / 2));
            if(layerSelectEnabled) {
                drawUpButton(nav, left, top);
                drawDownButton(nav, left, top);
                drawLevelText(nav, left, top);
            }
            if(modeButtonEnabled) {
                drawModeButton(nav, left, top);
            }
        }
        return height;
    }

    private void updateLayerSelectLogic() {
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
            }
        } else if(collapsing) {
            expanding = false;
            --modeSwitchTicks;
            if(modeSwitchTicks <= 0) {
                // If done with collapsing, stop and ensure ticks is clamped
                modeSwitchTicks = 0;
                collapsing = false;
            }
        }
    }

    private void drawUpButton(IBookGraphics nav, int left, int top) {
        // TODO fix maths
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        int y = top + (height / 2) - 50;
        upBounds = new Rectangle(x, y, 20, 18);
        int u = 4;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), upBounds)) u += 25;
        if(!upEnabled) u = 54;
        nav.drawImage(BOOK_GUI_TEXTURES, x, y, u, 106, 10, 9, 256, 256, 2f);
    }

    private void drawDownButton(IBookGraphics nav, int left, int top) {
        // TODO fix maths
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        int y = top + (height / 2);
        downBounds = new Rectangle(x, y, 20, 18);
        int u = 4;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), downBounds)) u += 25;
        if(!downEnabled) u = 54;
        nav.drawImage(BOOK_GUI_TEXTURES, x, y, u, 117, 10, 9, 256, 256, 2f);
    }

    private void drawModeButton(IBookGraphics nav, int left, int top) {
        // TODO fix maths
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET - 1;
        int y = top + (height / 2) + 24;
        modeBounds = new Rectangle(x, y, 23, 26);
        int u = 54;
        int v = 2;
        if(inBounds(nav.getMouseX(), nav.getMouseY(), modeBounds)) u += 27;
        if(!expanded) v += 28;
        nav.drawImage(BOOK_GUI_TEXTURES, x, y, u, v, 23, 26, 256, 256, 1f);
    }

    private void drawLevelText(IBookGraphics nav, int left, int top) {
        // TODO add in center line
        int x = left + nav.getPageWidth() - BUTTON_PANEL_RIGHT_OFFSET;
        int y = top + (height / 2) - 24;
        y += drawCenteredText(nav, x, y, BUTTON_PANEL_WIDTH, Integer.toString(maxDisplayLayer), Color.darkGray);
        y += 4;
        drawCenteredText(nav, x, y, BUTTON_PANEL_WIDTH, Integer.toString(getCurrentStructureHeight()), Color.darkGray);
    }

    private int drawCenteredText(IBookGraphics nav, int x, int y, int panelWidth, String text, Color color) {
        x += panelWidth / 2;
        x -= Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2;
        return nav.addStringWrapping(x, y, text, color.getRGB(), 0);
    }

    private void renderStructure(MultiblockStructure structure, IBookGraphics nav, int left, int top) {
        final float expandMu = MathHelper.clamp((modeSwitchTicks + nav.getPartialTicks()) / MODE_SWITCH_MAX, 0f, 1f);
        final float expandSinAlpha = getSinInterp(expandMu, 0f, 1f);
        final float expandBlockScale = 1f - (expandSinAlpha * (1f - expandScale));
        final float expandLevelAmount = expandSinAlpha * expandLevelGap;
        // TODO render floor & poles
        structure.render(left, top, expandBlockScale, expandLevelAmount, maxDisplayLayer);
    }

    @Override
    public void parse(NamedNodeMap attributes) {
        Node attr = attributes.getNamedItem("height");
        if (attr != null) {
            Integer parsedHeight = Ints.tryParse(attr.getTextContent());
            if(parsedHeight != null) height = parsedHeight;
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
                expanded = false;
            } else {
                expanding = true;
                expanded = true;
            }
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

    /**
     * Util method that returns a number between y1 and y2 according to an interpolation based off of the transformed sine value at mu
     * @param mu Between [0.0f, 1.0f]
     * @param y1 The y value at mu=0f
     * @param y2 The y value at mu=1f
     * @return Between [y1, y2]
     */
    private float getSinInterp(float mu, float y1, float y2) {
        mu = net.minecraft.util.math.MathHelper.clamp(mu, 0.0f, 1.0f);
        // Calculate the alpha value at mu
        float alpha = 0.5f * (float)Math.sin(((3.142f * mu) - 1.571f)) + 0.5f;
        return (y2 - y1) * alpha + y1;
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

    private enum PoleMode {
        OFF, ON, BELOW_ITEMS;
        private static PoleMode parse(String string) {
            if(string.equalsIgnoreCase("corners")) return ON;
            else if(string.equalsIgnoreCase("below_items")) return BELOW_ITEMS;
            else return OFF; // default
        }
    }

    private enum FloorMode {
        OFF, UNDER, ADJACENT, GRID;
        private static FloorMode parse(String string) {
            if(string.equalsIgnoreCase("grid")) return GRID;
            else if(string.equalsIgnoreCase("under")) return UNDER;
            else if(string.equalsIgnoreCase("adjacent")) return ADJACENT;
            else return OFF; // default
        }
    }
}
