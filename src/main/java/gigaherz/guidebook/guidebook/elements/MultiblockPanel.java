package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.sun.javafx.geom.Vec3f;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.ParseUtils;
import gigaherz.guidebook.guidebook.multiblock.MultiblockComponent;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class MultiblockPanel implements IPageElement {
    private int height = 256; // Default height
    private int indent = 0;
    private PoleMode poles = PoleMode.OFF;
    private FloorMode floor = FloorMode.GRID;
    private float spin = 0f;
    private OffsetOrigin offsetOrigin = OffsetOrigin.CENTER;
    private Vec3f offset = new Vec3f(0f, 0f, 0f);
    private int layerGap = 0;
    private MultiblockComponent[] components;

    @Override
    public int apply(IBookGraphics nav, int left, int top) {
        return 0;
    }

    @Override
    public void parse(NamedNodeMap attributes) {
        Node attr = attributes.getNamedItem("height");
        if (attr != null) {
            height = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("indent");
        if (attr != null) {
            indent = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("layerGap");
        if (attr != null) {
            layerGap = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("poles");
        if (attr != null) {
            poles = PoleMode.parse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("floor");
        if (attr != null) {
            floor = FloorMode.parse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("spin");
        if (attr != null) {
            spin = Floats.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("offset");
        if (attr != null) {
            offset = ParseUtils.parseVec3f(attr.getTextContent());
        }

        attr = attributes.getNamedItem("offsetOrigin");
        if (attr != null) {
            offsetOrigin = OffsetOrigin.parse(attr.getTextContent());
        }
    }

    public void parseChildren(NodeList childNodes) {
        ArrayList<MultiblockComponent> componentList = new ArrayList<>();
        for(int i = 0; i < childNodes.getLength(); ++i) {
            Node componentNode = childNodes.item(i);
            MultiblockComponent.MultiblockComponentFactory componentFactory = MultiblockComponent.getFactory(componentNode.getNodeName());
            if(componentFactory != null) {
                componentList.add(componentFactory.parse(componentNode));
            }
        }
        components = componentList.toArray(new MultiblockComponent[componentList.size()]);
    }

    @Override
    public IPageElement copy() {
        return null;
    }

    private enum PoleMode {
        OFF, ON, BELOW_ITEMS;
        private static PoleMode parse(String string) {
            if(string.equalsIgnoreCase("grid")) return ON;
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

    private enum OffsetOrigin {
        CENTER, LEFT, RIGHT, FRONT, BACK;
        private static OffsetOrigin parse(String string) {
            if(string.equalsIgnoreCase("left")) return LEFT;
            else if(string.equalsIgnoreCase("right")) return RIGHT;
            else if(string.equalsIgnoreCase("front")) return FRONT;
            else if(string.equalsIgnoreCase("back")) return BACK;
            else return CENTER; // default
        }
    }
}
