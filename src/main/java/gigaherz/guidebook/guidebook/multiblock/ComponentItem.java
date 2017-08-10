package gigaherz.guidebook.guidebook.multiblock;

import org.w3c.dom.Node;

public class ComponentItem extends MultiblockComponent {
    public static class Factory extends MultiblockComponent.MultiblockComponentFactory {
        public Factory() {
            this.setRegistryName("stack");
        }

        @Override
        public MultiblockComponent parse(Node thisNode) {
            return new ComponentItem();
        }
    }
}
