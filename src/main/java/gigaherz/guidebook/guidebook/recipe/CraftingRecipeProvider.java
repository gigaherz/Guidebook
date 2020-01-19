package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.ElementImage;
import gigaherz.guidebook.guidebook.elements.ElementStack;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.crafting.IShapedRecipe;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author joazlazer
 * A class designed to provide both shaped and shapeless crafting recipes for display in Guidebooks
 */
class CraftingRecipeProvider implements IRecipeProvider
{
    private static final int[] INPUT_SLOT_BASE_X = {4, 13, 19, 19};
    private static final int[] INPUT_SLOT_BASE_Y = {3, 3, 3, 13};
    private static final int INPUT_SLOT_OFFSET = 19;
    private static final int[] OUTPUT_SLOT_X = {80, 70, 64, 64};
    private static final int[] OUTPUT_SLOT_Y = {22, 13, 14, 13};

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "gui/recipe_backgrounds");
    private static final int[] BACKGROUND_U = {0, 0, 0, 0};
    private static final int[] BACKGROUND_V = {0, 60, 101, 142};
    private static final int[] BACKGROUND_W = {100, 100, 100, 100};
    private static final int[] BACKGROUND_H = {60, 41, 39, 39};

    private static final int[] HEIGHT = BACKGROUND_H;
    private static final int LEFT_OFFSET = 38;

    @Override
    public Optional<ProvidedComponents> provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex)
    {
        return getRecipesByOutput(targetOutput, recipeIndex).map(this::provideCraftingRecipeComponents);
    }

    @Override
    public Optional<ProvidedComponents> provideRecipeComponents(@Nonnull ResourceLocation recipeKey)
    {
        return getRecipeByName(recipeKey).map(this::provideCraftingRecipeComponents);
    }

    private Optional<? extends IRecipe> getRecipeByName(ResourceLocation name)
    {
        return Minecraft.getInstance().player.world.getRecipeManager().getRecipe(name);
    }

    private Optional<? extends IRecipe> getRecipesByOutput(@Nonnull ItemStack targetOutput, int recipeIndex)
    {
        return Minecraft.getInstance().player.world.getRecipeManager().getRecipes().stream()
                .filter(r -> !r.isDynamic()
                                && ItemStack.areItemsEqualIgnoreDurability(targetOutput, r.getRecipeOutput())
                        /*&& ItemStack.areItemStackTagsEqual(targetOutput, r.getRecipeOutput())*/
                )
                .skip(recipeIndex).findFirst();
    }

    private ProvidedComponents provideCraftingRecipeComponents(@Nonnull IRecipe<?> recipe)
    {
        int recipeWidth;
        int recipeHeight;
        int recipeGraphic;
        if(recipe instanceof AbstractCookingRecipe)
        {
            recipeWidth = 1;
            recipeHeight = 1;
            recipeGraphic = 2;
        }
        else {
            if (recipe instanceof IShapedRecipe)
            {
                IShapedRecipe<?> shapedRecipe = (IShapedRecipe<?>)recipe;
                recipeWidth = shapedRecipe.getRecipeWidth();
                recipeHeight = shapedRecipe.getRecipeHeight();
            }
            else
            {
                int ingredients = recipe.getIngredients().size();
                recipeWidth = MathHelper.ceil(Math.sqrt(ingredients));
                recipeHeight = MathHelper.ceil(ingredients / (double)recipeWidth);
            }

            switch (Math.max(recipeWidth,recipeHeight))
            {
                case 1: recipeGraphic = 3; break;
                case 2: recipeGraphic = 1; break;
                default: recipeGraphic = 0; break;
            }
        }

        ArrayList<ElementStack> stackComponents = new ArrayList<>();
        VisualElement additionalRenderer = VisualElement.EMPTY;

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

            int posX = i % recipeWidth;
            int posY = i / recipeWidth;
            inputSlot.x = INPUT_SLOT_BASE_X[recipeGraphic] + (posX * INPUT_SLOT_OFFSET) + LEFT_OFFSET;
            inputSlot.y = INPUT_SLOT_BASE_Y[recipeGraphic] + (posY * INPUT_SLOT_OFFSET);
            stackComponents.add(inputSlot); // Only add the element if there is an item in the slot
        }

        // Set up output slot element
        ElementStack outputSlot = new ElementStack(false, false);
        stackComponents.add(outputSlot);
        List<ItemStack> stackList = IRecipeProvider.copyAndExpand(recipe.getRecipeOutput());
        outputSlot.stacks.addAll(stackList);
        outputSlot.x = OUTPUT_SLOT_X[recipeGraphic] + LEFT_OFFSET;
        outputSlot.y = OUTPUT_SLOT_Y[recipeGraphic];

        // Set up background image
        ElementImage background = new ElementImage(false, false);
        background.textureLocation = BACKGROUND_TEXTURE;
        background.x = LEFT_OFFSET;
        background.y = 0;
        background.tx = BACKGROUND_U[recipeGraphic];
        background.ty = BACKGROUND_V[recipeGraphic];
        background.w = BACKGROUND_W[recipeGraphic];
        background.h = BACKGROUND_H[recipeGraphic];

        // Set up overall height
        int height = HEIGHT[recipeGraphic];

        ElementStack[] components = new ElementStack[stackComponents.size()];
        stackComponents.toArray(components);

        return new ProvidedComponents(height, components, background, additionalRenderer);
    }
}
