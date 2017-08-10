package gigaherz.guidebook.guidebook.multiblock;

import org.w3c.dom.Node;

public class ComponentBlock extends MultiblockComponent {
    public static class Factory extends MultiblockComponent.MultiblockComponentFactory {
        public Factory() {
            this.setRegistryName("block");
        }

        @Override
        public MultiblockComponent parse(Node thisNode) {
            return new ComponentBlock();
        }
    }
}
