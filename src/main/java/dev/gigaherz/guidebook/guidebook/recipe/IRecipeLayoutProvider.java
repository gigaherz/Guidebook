package dev.gigaherz.guidebook.guidebook.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * @author joazlazer
 * A class designed to be registered and implemented by any recipe implementations that will be queried for display in a Guidebook. Create and
 * register one for each different recipe system/machine. The default ones that exist are:
 * - FurnaceRecipeProvider
 * - CraftingRecipeProvider.ShapedRecipeProvider
 * - CraftingRecipeProvider.ShapelessRecipeProvider
 */
public interface IRecipeLayoutProvider
{
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
    @Nonnull
    RecipeLayout getRecipeLayout(@Nonnull Level world, @Nonnull ItemStack targetOutput, int recipeIndex);

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
    @Nonnull
    RecipeLayout getRecipeLayout(@Nonnull Level world, @Nonnull ResourceLocation recipeKey);
}
