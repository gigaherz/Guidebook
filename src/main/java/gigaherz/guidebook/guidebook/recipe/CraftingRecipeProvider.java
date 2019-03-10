package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.util.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.ElementImage;
import gigaherz.guidebook.guidebook.elements.ElementStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author joazlazer
 * A class designed to provide both shaped and shapeless crafting recipes for display in Guidebooks
 */
class CraftingRecipeProvider extends RecipeProvider
{
    private static final int[] INPUT_SLOT_BASE_X = {4, 13};
    private static final int[] INPUT_SLOT_BASE_Y = {3, 3};
    private static final int INPUT_SLOT_OFFSET = 19;
    private static final int[] OUTPUT_SLOT_X = {80, 70};
    private static final int[] OUTPUT_SLOT_Y = {22, 13};

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "gui/recipe_backgrounds");
    private static final int[] BACKGROUND_U = {0, 0};
    private static final int[] BACKGROUND_V = {0, 60};
    private static final int[] BACKGROUND_W = {100, 100};
    private static final int[] BACKGROUND_H = {60, 41};

    private static final int[] HEIGHT = BACKGROUND_H;
    private static final int LEFT_OFFSET = 38;

    @Nullable
    @Override
    public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex)
    {
        return provideCraftingRecipeComponents(getRecipesByOutput(targetOutput, recipeIndex));
    }

    @Nullable
    @Override
    public ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey)
    {
        return provideCraftingRecipeComponents(getRecipeByName(recipeKey));
    }

    @Nullable
    private IRecipe getRecipeByName(ResourceLocation name)
    {
        return ForgeRegistries.RECIPES.getValue(name);
    }

    @Nullable
    private IRecipe getRecipesByOutput(@Nonnull ItemStack targetOutput, int recipeIndex)
    {
        return ForgeRegistries.RECIPES.getValuesCollection().stream()
                .filter(r -> !r.isDynamic()
                                && ItemStack.areItemsEqualIgnoreDurability(targetOutput, r.getRecipeOutput())
                        /*&& ItemStack.areItemStackTagsEqual(targetOutput, r.getRecipeOutput())*/
                )
                .skip(recipeIndex).findFirst().orElse(null);
    }

    @Nullable
    private RecipeProvider.ProvidedComponents provideCraftingRecipeComponents(@Nullable IRecipe recipe)
    {
        if (recipe == null)
            return null;

        int constantIndex = recipe.getIngredients().size() <= 4 ? 1 : 0; // Whether to use the 3x3 (0) or 2x2 (1) grid
        ArrayList<ElementStack> stackComponents = new ArrayList<>();
        VisualElement additionalRenderer = new VisualElement(new Size(), 0, 0, 0)
        {
        };
        int gridWidth = constantIndex == 0 ? 3 : 2;

        // Set up input slots
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); ++i)
        {
            ElementStack inputSlot = new ElementStack(false, false);
            ItemStack[] matching = ingredients.get(i).getMatchingStacks();
            if (matching.length == 0) continue; // If the recipe area is blank, continue and ignore

            // Copy each stack
            for (int j = 0; j < matching.length; ++j)
            {
                inputSlot.stacks.add(matching[j].copy());
            }

            int posX = i % gridWidth;
            int posY = i / gridWidth;
            inputSlot.x = INPUT_SLOT_BASE_X[constantIndex] + (posX * INPUT_SLOT_OFFSET) + LEFT_OFFSET;
            inputSlot.y = INPUT_SLOT_BASE_Y[constantIndex] + (posY * INPUT_SLOT_OFFSET);
            stackComponents.add(inputSlot); // Only add the element if there is an item in the slot
        }

        // Set up output slot element
        ElementStack outputSlot = new ElementStack(false, false);
        stackComponents.add(outputSlot);
        List<ItemStack> stackList = RecipeProvider.copyAndExpand(recipe.getRecipeOutput());
        outputSlot.stacks.addAll(stackList);
        outputSlot.x = OUTPUT_SLOT_X[constantIndex] + LEFT_OFFSET;
        outputSlot.y = OUTPUT_SLOT_Y[constantIndex];

        // Set up background image
        ElementImage background = new ElementImage(false, false);
        background.textureLocation = BACKGROUND_TEXTURE;
        background.x = LEFT_OFFSET;
        background.y = 0;
        background.tx = BACKGROUND_U[constantIndex];
        background.ty = BACKGROUND_V[constantIndex];
        background.w = BACKGROUND_W[constantIndex];
        background.h = BACKGROUND_H[constantIndex];

        // Set up overall height
        int height = HEIGHT[constantIndex];

        ElementStack[] components = new ElementStack[stackComponents.size()];
        stackComponents.toArray(components);

        return new ProvidedComponents(height, components, background, additionalRenderer);
    }
}
