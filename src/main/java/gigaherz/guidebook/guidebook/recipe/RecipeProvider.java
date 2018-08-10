package gigaherz.guidebook.guidebook.recipe;

import com.google.common.collect.Maps;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.ElementImage;
import gigaherz.guidebook.guidebook.elements.ElementStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author joazlazer
 * A class designed to be registered and implemented by any recipe implementations that will be queried for display in a Guidebook. Create and
 * register one for each different recipe system/machine. The default ones that exist are:
 * - FurnaceRecipeProvider
 * - CraftingRecipeProvider.ShapedRecipeProvider
 * - CraftingRecipeProvider.ShapelessRecipeProvider
 */
@SuppressWarnings("WeakerAccess")
public abstract class RecipeProvider
{
    public static final Map<ResourceLocation, RecipeProvider> registry = Maps.newHashMap();

    static
    {
        CraftingRecipeProvider crafting = new CraftingRecipeProvider();
        registry.put(GuidebookMod.location("crafting"), crafting);
        registry.put(GuidebookMod.location("shaped"), crafting); // legacy
        registry.put(GuidebookMod.location("shapeless"), crafting); // legacy
        registry.put(GuidebookMod.location("smelting"), new FurnaceRecipeProvider());
    }

    /**
     * Prepares display of the recipe for the target item (if multiple, the (recipeIndex + 1)th occurrence) by creating a ProvidedComponents construct that contains:
     * - An array of Stack objects to represent ItemStacks to render
     * - An Image object to represent the background image
     * - A height int that represents how much space this element should take up on the section
     * - An IRenderDelegate instance designed to be used via a lambda that allows a RecipeProvider implementation to draw additional items (i.e. Thaumcraft infusion essentia)
     *
     * @param targetOutput A target ItemStack that was specified via XML
     * @param recipeIndex  The offset to use when searching for recipes
     * @return A valid ProvidedComponents object containing the above, and null if the recipe was not found
     */
    @Nullable
    public abstract ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex);

    /**
     * Prepares display of the recipe that matches the specified key by creating a ProvidedComponents construct that contains:
     * - An array of Stack objects to represent ItemStacks to render
     * - An Image object to represent the background image
     * - A height int that represents how much space this element should take up on the section
     * - An IRenderDelegate instance designed to be used via a lambda that allows a RecipeProvider implementation to draw additional items (i.e. Thaumcraft infusion essentia)
     *
     * @param recipeKey The registry name for the recipe to be displayed that was specified via XML
     * @return A valid ProvidedComponents object containing the above, and null if the recipe was not found
     */
    @Nullable
    public abstract ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey);

    /**
     * A helper method designed to validate ItemStacks from recipes with metadata that is OreDictionary.WILDCARD_VALUE
     * If the input is a tool/armor, sets it to full durability
     * If the input is a wildcard but doesn't have subtypes, set it to its only metadata
     * If the input is a wildcard but does have subtypes, expand it by retrieving all valid subtypes
     *
     * @param stack The ItemStack to inspect, which gets copied from the start and is unaffected
     * @return Each ItemStack created as a result of copying and expanding the input stack (for cases that lack expansion, the size will be 1)
     */
    protected static List<ItemStack> copyAndExpand(@Nonnull ItemStack stack)
    {
        NonNullList<ItemStack> stacks = NonNullList.create();
        ItemStack base = stack.copy();
        stacks.add(base);

        if (base.isItemStackDamageable()) base.setItemDamage(0);
        if (base.getMetadata() == OreDictionary.WILDCARD_VALUE && !base.getHasSubtypes()) base.setItemDamage(0);
        else if (base.getMetadata() == OreDictionary.WILDCARD_VALUE && base.getHasSubtypes())
        {
            Item item = base.getItem();
            int stackSize = base.getCount();
            NBTTagCompound tag = (base.hasTagCompound() && base.getTagCompound() != null) ? base.getTagCompound().copy() : null;
            NonNullList<ItemStack> processed_items = NonNullList.create();
            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);

            for (ItemStack subItem : subItems)
            {
                subItem = subItem.copy();
                subItem.setCount(stackSize);
                subItem.setTagCompound(tag);
                processed_items.add(subItem);
            }
            stacks = subItems;
        }
        return stacks;
    }

    /**
     * A helper packaging class that allows RecipeProvider.provideRecipeComponents(...) to return multiple GUI components and values
     */
    public static class ProvidedComponents
    {
        public int height = 0;
        public ElementStack[] recipeComponents;
        public ElementImage background;
        public VisualElement delegate;

        public ProvidedComponents(int h, ElementStack[] rc, ElementImage background, VisualElement ird)
        {
            this.height = h;
            this.recipeComponents = rc;
            this.background = background;
            this.delegate = ird;
        }
    }
}
