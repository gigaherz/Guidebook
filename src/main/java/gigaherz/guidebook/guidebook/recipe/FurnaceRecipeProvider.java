package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.util.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.ElementImage;
import gigaherz.guidebook.guidebook.elements.ElementStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * @author joazlazer
 * A class designed to provide furnace recipes for display in Guidebooks
 */
public class FurnaceRecipeProvider extends RecipeProvider
{
    private static final int INPUT_SLOT_X = 19;
    private static final int INPUT_SLOT_Y = 3;
    private static final int OUTPUT_SLOT_X = 64;
    private static final int OUTPUT_SLOT_Y = 14;

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(GuidebookMod.MODID, "gui/recipe_backgrounds");
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 101;
    private static final int BACKGROUND_W = 100;
    private static final int BACKGROUND_H = 39;

    private static final int HEIGHT = BACKGROUND_H;
    private static final int LEFT_OFFSET = 38;

    @Override
    @Nullable
    public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex)
    {
        // Ignore recipeIndex because a furnace recipe can show each recipe by alternating the slots

        ArrayList<ItemStack> inputStacks = new ArrayList<>();
        for (ItemStack key : FurnaceRecipes.instance().getSmeltingList().keySet())
        {
            if (FurnaceRecipes.instance().getSmeltingList().get(key).isItemEqual(targetOutput))
            {
                inputStacks.addAll(copyAndExpand(key));
            }
        }

        if (inputStacks.size() > 0) // Should always be true
        {
            VisualElement additionalRenderer = new VisualElement(new Size(), 0, 0, 0)
            {
            };
            ElementStack[] recipeComponents = new ElementStack[2];

            // Set up input slot element
            ElementStack inputSlot = new ElementStack(false, false);
            recipeComponents[0] = inputSlot;
            inputSlot.stacks.addAll(inputStacks);
            inputSlot.x = INPUT_SLOT_X + LEFT_OFFSET;
            inputSlot.y = INPUT_SLOT_Y;

            // Set up output slot element
            ElementStack outputSlot = new ElementStack(false, false);
            recipeComponents[1] = outputSlot;
            // Add output stacks for each recipe in the same order as the input ones (in case the item quantities vary)
            for (ItemStack inputStack : inputStacks)
            {
                // Use copyAndExpand utility method to fix WILDCARD_VALUE stack meta's and low-durability tools
                outputSlot.stacks.addAll(copyAndExpand(FurnaceRecipes.instance().getSmeltingResult(inputStack)));
            }
            outputSlot.x = OUTPUT_SLOT_X + LEFT_OFFSET;
            outputSlot.y = OUTPUT_SLOT_Y;

            // Set up background image
            ElementImage background = new ElementImage(false, false);
            background.textureLocation = BACKGROUND_TEXTURE;
            background.x = LEFT_OFFSET;
            background.y = 0;
            background.tx = BACKGROUND_U;
            background.ty = BACKGROUND_V;
            background.w = BACKGROUND_W;
            background.h = BACKGROUND_H;

            return new ProvidedComponents(HEIGHT, recipeComponents, background, additionalRenderer);
        }
        else
            GuidebookMod.logger.error(String.format("[FurnaceRecipeProvider] Recipe not found for '%s' although hasRecipe(...) returned true. Something is wrong!", targetOutput));
        return null;
    }

    @Nullable
    @Override
    public ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey)
    {
        GuidebookMod.logger.warn(String.format("[FurnaceRecipeProvider] Furnace recipe specified via recipeKey '%s', however furnace recipes are not registered using a ResourceLocation. Ignoring.", recipeKey));
        return null;
    }
}
