package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class RecipeProvider extends IForgeRegistryEntry.Impl<RecipeProvider> {
    public static IForgeRegistry<RecipeProvider> registry;

    public abstract boolean hasRecipe(ItemStack targetOutput);
    public abstract int provideRecipeComponents(ItemStack targetOutput, int recipeIndex, Stack[] recipeComponents, Image background, IRenderDelegate additionalRenderer);
}
