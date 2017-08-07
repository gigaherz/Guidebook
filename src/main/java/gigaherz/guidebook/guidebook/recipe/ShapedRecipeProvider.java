package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ShapedRecipeProvider extends RecipeProvider {
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

    private ArrayList<ShapedRecipes> shapedRecipes;
    private ArrayList<ShapedOreRecipe> shapedOreRecipes;

    public ShapedRecipeProvider() {
        this.setRegistryName(GuidebookMod.MODID, "shaped");
    }

    @Override
    public void reloadCache() {
        shapedRecipes = new ArrayList<>();
        shapedOreRecipes = new ArrayList<>();
        for(IRecipe recipe : ForgeRegistries.RECIPES.getValues()) {
            if(recipe instanceof ShapedRecipes) shapedRecipes.add((ShapedRecipes)recipe);
            if(recipe instanceof ShapedOreRecipe) shapedOreRecipes.add((ShapedOreRecipe)recipe);
        }
    }

    @Override
    public boolean hasRecipe(ItemStack targetOutput) {
        // Query the shaped and shaped ore recipe caches
        return queryRecipeCaches(targetOutput, 0) != null;
    }

    @Nullable
    public IRecipe queryRecipeCaches(@Nonnull ItemStack targetOutput, int recipeIndex) {
        // Query the shaped and shaped ore recipe caches, but return the (recipeIndex + 1)th occurance
        for(ShapedOreRecipe shapedOreRecipe : shapedOreRecipes) {
            if(shapedOreRecipe.getRecipeOutput().isItemEqual(targetOutput)) {
                if(recipeIndex > 0) {
                    --recipeIndex;
                } else {
                    return shapedOreRecipe;
                }
            }
        }
        for(ShapedRecipes shapedRecipe : shapedRecipes) {
            if(shapedRecipe.getRecipeOutput().isItemEqual(targetOutput)) {
                if(recipeIndex > 0) {
                    --recipeIndex;
                } else {
                    return shapedRecipe;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex) {
        IRecipe foundRecipe;

        foundRecipe = queryRecipeCaches(targetOutput, recipeIndex);
        if(foundRecipe == null) {
            foundRecipe = queryRecipeCaches(targetOutput, 0);
            GuidebookMod.logger.warn(String.format("<recipe> index '%d' was not found in the list of cached shaped recipes for '%s'. Falling back to the first occurrence.", recipeIndex, targetOutput.toString()));
        }

        if(foundRecipe != null) {
            int constantIndex = foundRecipe.getIngredients().size() == 4 ? 1 : 0; // Whether to use the 3x3 (0) or 2x2 (1) grid
            ArrayList<Stack> stackComponents = new ArrayList<>();
            IRenderDelegate ird = (nav, top, left) -> { };
            int gridWidth = constantIndex == 0 ? 3 : 2;

            // Set up input slots
            for(int i = 0; i < foundRecipe.getIngredients().size(); ++i) {
                Stack inputSlot = new Stack();
                ItemStack[] matching = foundRecipe.getIngredients().get(i).getMatchingStacks();
                if(matching.length == 0) continue; // If the recipe area is blank, continue and ignore
                ArrayList<ItemStack> copyStacks = new ArrayList<>();
                for (int j = 0; j < matching.length; ++j) {
                    copyStacks.add(matching[j].copy());
                }
                inputSlot.stacks = copyStacks.toArray(new ItemStack[copyStacks.size()]);
                int posX = i % gridWidth;
                int posY = i / gridWidth;
                inputSlot.x = INPUT_SLOT_BASE_X[constantIndex] + (posX * INPUT_SLOT_OFFSET) + LEFT_OFFSET;
                inputSlot.y = INPUT_SLOT_BASE_Y[constantIndex] + (posY * INPUT_SLOT_OFFSET);
                stackComponents.add(inputSlot); // Only add the element if there is an item in the slot
            }

            // Set up output slot element
            Stack outputSlot = new Stack();
            stackComponents.add(outputSlot);
            List<ItemStack> stackList = copyAndExpand(foundRecipe.getRecipeOutput());
            outputSlot.stacks = stackList.toArray(new ItemStack[stackList.size()]);
            outputSlot.x = OUTPUT_SLOT_X[constantIndex] + LEFT_OFFSET;
            outputSlot.y = OUTPUT_SLOT_Y[constantIndex];

            // Set up background image
            Image background = new Image();
            background.textureLocation = BACKGROUND_TEXTURE;
            background.x = 0 + LEFT_OFFSET;
            background.y = 0;
            background.tx = BACKGROUND_U[constantIndex];
            background.ty = BACKGROUND_V[constantIndex];
            background.w = BACKGROUND_W[constantIndex];
            background.h = BACKGROUND_H[constantIndex];

            // Set up overall height
            int     height = HEIGHT[constantIndex];

            Stack[] components = new Stack[stackComponents.size()];
            stackComponents.toArray(components);
            ProvidedComponents result = new ProvidedComponents(height, components, background, ird);
            return result;
        } else GuidebookMod.logger.error(String.format("[ShapedRecipeProvider] Recipe not found for '%s' although hasRecipe(...) returned true. Something is wrong!", targetOutput.toString()));
        return null;
    }
}
