package gigaherz.guidebook.guidebook.multiblock;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import org.w3c.dom.Node;

import javax.annotation.Nullable;

/**
 * @author joazlazer
 *
 * A class designed to be implemented by any custom usages of multiblock display componenets
 * The default implementations currently are:
 *  - ComponentItem, which handles display of ItemStacks via Stacks
 *  - ComponentBlock, which handles display of Blocks
 */
public abstract class MultiblockComponent {
    public static IForgeRegistry<MultiblockComponentFactory> factoryRegistry;

    /**
     * Handles registry of the MultiblockComponentFactory registry to the meta-registry and register the default MultiblockComponentFactory's
     */
    @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerRegistries(RegistryEvent.NewRegistry event) {
            RegistryBuilder rb = new RegistryBuilder<MultiblockComponentFactory>();
            rb.setType(MultiblockComponentFactory.class);
            rb.setName(new ResourceLocation(GuidebookMod.MODID, "multiblock_component_factory"));
            factoryRegistry = rb.create();
        }

        @SubscribeEvent
        public static void registerDefaults(RegistryEvent.Register<MultiblockComponentFactory> event) {
            event.getRegistry().register(new ComponentBlock.Factory());
            event.getRegistry().register(new ComponentItem.Factory());
        }
    }

    /**
     * Gets the multiblock component factory with the specified factory name
     * NOTE: Ignores registry name domains
     * @param name
     * @return Null if not found; otherwise, the factory
     */
    @Nullable
    public static MultiblockComponentFactory getFactory(String name) {
        for(MultiblockComponentFactory factory : factoryRegistry.getValues()) {
            if(factory.getRegistryName().getResourcePath().equals(name)) return factory;
        }
        return null; // If not found, return null
    }

    /**
     * A class that can be registered to the factory registry and is responsible for creating new MultiblockComponents
     * WARNING: The registry name's domain is ignored when determining the correct factory to use when parsing from XML
     */
    public static abstract class MultiblockComponentFactory extends IForgeRegistryEntry.Impl<MultiblockComponentFactory> {
        /**
         * Parses the current node and is responsible for creating a new instance of the MultiblockComponent with its new parsed values
         * @param thisNode The current inner node of the MultiblockPanel
         * @return A new instance of the MultiblockComponent implementation with its values set
         */
        public abstract MultiblockComponent parse(Node thisNode);
    }
}

