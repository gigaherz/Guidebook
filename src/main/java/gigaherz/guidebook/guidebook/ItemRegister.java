package gigaherz.guidebook.guidebook;

import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;

public final class ItemRegister
{
    public static Item guidebook;
    
    /**
     * Private constructor to hide the implicit public one.
     */
    private ItemRegister()
    {
        /*
         * Intentionally left empty.
         */
    }
    
    public static void init(final IForgeRegistry<Item> registry)
    {
        guidebook = new ItemGuidebook("guidebook");

        registry.register(guidebook);
    }
}