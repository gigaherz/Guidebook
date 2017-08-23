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

/**
 * @author joazlazer
 * <p>
 * An extention of MultiblockComponent that supports parsing from nodes within the parent <multiblock> node by registry of a Parser implementation
 */
@SuppressWarnings("WeakerAccess")
public abstract class ParsableMultiblockComponent extends MultiblockComponent
{
    /**
     * A registrable factory-type class that parses specific implementations of ParsableMultiblockComponent according to their tag names
     * Default implementations:
     * - ItemComponent which is mapped to 'stack' and handles display of items in multiblocks
     */
    @SuppressWarnings("unused")
    public static abstract class Parser extends IForgeRegistryEntry.Impl<ParsableMultiblockComponent.Parser>
    {
        public static IForgeRegistry<ParsableMultiblockComponent.Parser> registry;

        /**
         * Handles registry of the Parser registry to the meta-registry and register the default vanilla Parser's
         */
        @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
        public static class RegistrationHandler
        {
            @SuppressWarnings("unchecked")
            @SubscribeEvent
            public static void registerRegistries(RegistryEvent.NewRegistry event)
            {
                RegistryBuilder rb = new RegistryBuilder<ParsableMultiblockComponent.Parser>();
                rb.setType(ParsableMultiblockComponent.Parser.class);
                rb.setName(new ResourceLocation(GuidebookMod.MODID, "multiblock_component_parser"));
                registry = rb.create();
            }

            @SubscribeEvent
            public static void registerDefaults(RegistryEvent.Register<ParsableMultiblockComponent.Parser> event)
            {
                event.getRegistry().registerAll(new ItemComponent.Parser());
            }
        }

        /**
         * Gets the tag mapping for the current parsable multiblock component
         *
         * @return A string to compare to the multiblock's sub-nodes
         */
        public abstract String getBaseNodeName();

        /**
         * Parses the node and creates a new multiblock component from the specified XML parameters
         *
         * @param baseNode The XML node at the base of this component
         * @return A new instance of a ParsableMultiblockComponent
         */
        public abstract ParsableMultiblockComponent parse(Node baseNode);
    }
}
