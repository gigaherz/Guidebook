package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author joazlazer
 * A class designed to be registered and implemented by any recipe implementations that will be queried for display in a Guidebook. Create and
 * register one for each different recipe system/machine. The default ones that exist are:
 *  - FurnaceRecipeProvider
 *  - CraftingRecipeProvider.ShapedRecipeProvider
 *  - CraftingRecipeProvider.ShapelessRecipeProvider
 */
public abstract class RecipeProvider extends IForgeRegistryEntry.Impl<RecipeProvider> {
    public static IForgeRegistry<RecipeProvider> registry;

    /**
     * Handles registry of the RecipeProvider registry to the meta-registry and register the default vanilla RecipeProvider's
     */
    @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public static void registerRegistries(RegistryEvent.NewRegistry event) {
            RegistryBuilder rb = new RegistryBuilder<RecipeProvider>();
            rb.setType(RecipeProvider.class);
            rb.setName(new ResourceLocation(GuidebookMod.MODID, "recipe_provider"));
            registry = rb.create();
        }

        @SubscribeEvent
        public static void registerDefaults(RegistryEvent.Register<RecipeProvider> event) {
            CraftingRecipeProvider crafting = new CraftingRecipeProvider();
            event.getRegistry().register(crafting.new ShapedRecipeProvider());
            event.getRegistry().register(crafting.new ShapelessRecipeProvider());
            event.getRegistry().register(new FurnaceRecipeProvider());
        }
    }

    /**
     * Whether the RecipeProvider implementation can provide a recipe that outputs the target item
     * @param targetOutput A target ItemStack that was specified via XML
     * @return True if the RecipeProvider can provide a recipe & its display components, and false if not
     */
    public abstract boolean hasRecipe(@Nonnull ItemStack targetOutput);

    /**
     * Whether the RecipeProvider implementation can provide a recipe that matches the specified key
     * @param recipeKey The registry name for the recipe to be queried that was specified via XML
     * @return True if the RecipeProvider can provide a recipe & its display components, and false if not
     */
    public abstract boolean hasRecipe(@Nonnull ResourceLocation recipeKey);

    /**
     * Prepares display of the recipe for the target item (if multiple, the (recipeIndex + 1)th occurrence) by creating a ProvidedComponents construct that contains:
     *  - An array of Stack objects to represent ItemStacks to render
     *  - An Image object to represent the background image
     *  - A height int that represents how much space this element should take up on the page
     *  - An IRenderDelegate instance designed to be used via a lambda that allows a RecipeProvider implementation to draw additional items (i.e. Thaumcraft infusion essentia)
     * @param targetOutput A target ItemStack that was specified via XML
     * @param recipeIndex The offset to use when searching for recipes
     * @return A valid ProvidedComponents object containing the above, and null if the recipe was not found
     */
    @Nullable
    public abstract ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex);

    /**
     * Prepares display of the recipe that matches the specified key by creating a ProvidedComponents construct that contains:
     *  - An array of Stack objects to represent ItemStacks to render
     *  - An Image object to represent the background image
     *  - A height int that represents how much space this element should take up on the page
     *  - An IRenderDelegate instance designed to be used via a lambda that allows a RecipeProvider implementation to draw additional items (i.e. Thaumcraft infusion essentia)
     * @param recipeKey The registry name for the recipe to be displayed that was specified via XML
     * @return A valid ProvidedComponents object containing the above, and null if the recipe was not found
     */
    @Nullable
    public abstract ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey);

    /**
     * An optionally overridable method that gets called before book parsing which allows RecipeProviders to cache
     * recipes retrieved from global registries (for efficiency).
     */
    public void reloadCache() {

    }

    /**
     * A helper method designed to validate ItemStacks from recipes with metadata that is OreDictionary.WILDCARD_VALUE
     * If the input is a tool/armor, sets it to full durability
     * If the input is a wildcard but doesn't have subtypes, set it to its only metadata
     * If the input is a wildcard but does have subtypes, expand it by retrieving all valid subtypes
     * @param stack The ItemStack to inspect, which gets copied from the start and is unaffected
     * @return Each ItemStack created as a result of copying and expanding the input stack (for cases that lack expansion, the size will be 1)
     */
    protected static List<ItemStack> copyAndExpand(@Nonnull ItemStack stack) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        ItemStack base = stack.copy();
        stacks.add(base);

        if(base.isItemStackDamageable()) base.setItemDamage(0);
        if(base.getMetadata() == OreDictionary.WILDCARD_VALUE && !base.getHasSubtypes()) base.setItemDamage(0);
        else if(base.getMetadata() == OreDictionary.WILDCARD_VALUE && base.getHasSubtypes()){
            Item item = base.getItem();
            int stackSize = base.getCount();
            NBTTagCompound tag = (base.hasTagCompound() && base.getTagCompound() != null) ? base.getTagCompound().copy() : null;
            NonNullList<ItemStack> processed_items = NonNullList.create();
            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);

            for (ItemStack subitem : subItems) {
                subitem = subitem.copy();
                subitem.setCount(stackSize);
                subitem.setTagCompound(tag);
                processed_items.add(subitem);
            }
            stacks = subItems;
        }
        return stacks;
    }

    /**
     * A helper packaging class that allows RecipeProvider.provideRecipeComponents(...) to return multiple GUI components and values
     */
    public static class ProvidedComponents {
        public int height = 0;
        public Stack[] recipeComponents;
        public Image background;
        public IRenderDelegate delegate;
        public ProvidedComponents(int h, Stack[] rc, Image bkgrd, IRenderDelegate ird) {
            height = h;
            recipeComponents = rc;
            background = bkgrd;
            delegate = ird;
        }
    }
}
