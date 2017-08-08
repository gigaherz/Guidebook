package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class CraftingRecipeProvider {
    public class ShapedRecipeProvider extends RecipeProvider {
        ShapedRecipeProvider() {
            this.setRegistryName(GuidebookMod.MODID, "shaped");
        }

        @Override
        public boolean hasRecipe(@Nonnull ItemStack targetOutput) {
            return hasCraftingRecipe(targetOutput, Type.SHAPED);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex) {
            return provideCraftingRecipeComponents(targetOutput, recipeIndex, Type.SHAPED);
        }
    }

    public class ShapelessRecipeProvider extends RecipeProvider {
        ShapelessRecipeProvider() {
            this.setRegistryName(GuidebookMod.MODID, "shapeless");
        }

        @Override
        public void reloadCache() {
            reloadCaches();
        }

        @Override
        public boolean hasRecipe(@Nonnull ItemStack targetOutput) {
            return hasCraftingRecipe(targetOutput, Type.SHAPELESS);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex) {
            return provideCraftingRecipeComponents(targetOutput, recipeIndex, Type.SHAPELESS);
        }
    }

    public enum Type {
        SHAPED,
        SHAPELESS;

        @Override
        public String toString() {
            if(this == SHAPED) return "shaped";
            else return "shapeless";
        }
    }

    private ArrayList<IRecipe> shapedRecipes;
    private ArrayList<IRecipe> shapelessRecipes;

    private void reloadCaches() {
        shapelessRecipes = new ArrayList<>();
        shapedRecipes = new ArrayList<>();
        for(IRecipe recipe : ForgeRegistries.RECIPES.getValues()) {
            if(recipe instanceof ShapelessOreRecipe) shapelessRecipes.add(recipe);
            if(recipe instanceof ShapelessRecipes) shapelessRecipes.add(recipe);
            if(recipe instanceof ShapedRecipes) shapedRecipes.add(recipe);
            if(recipe instanceof ShapedOreRecipe) shapedRecipes.add(recipe);
        }
    }

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

    private ArrayList<IRecipe> getCacheForType(Type type) {
        return type == Type.SHAPELESS ? shapelessRecipes : shapedRecipes;
    }

    private boolean hasCraftingRecipe(ItemStack targetOutput, Type type) {
        // Query the shaped and shaped ore recipe caches
        return queryRecipeCaches(targetOutput, 0, getCacheForType(type)) != null;
    }

    @Nullable
    private IRecipe queryRecipeCaches(@Nonnull ItemStack targetOutput, int recipeIndex, ArrayList<IRecipe> cache) {
        // Query either the shaped or shapeless recipe cache, but return the (recipeIndex + 1)th occurance
        for(IRecipe recipe : cache) {
            if(recipe.getRecipeOutput().isItemEqual(targetOutput)) {
                if(recipeIndex > 0) {
                    --recipeIndex;
                } else {
                    return recipe;
                }
            }
        }
        return null;
    }

    @Nullable
    private RecipeProvider.ProvidedComponents provideCraftingRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex, Type type) {
        IRecipe foundRecipe;

        foundRecipe = queryRecipeCaches(targetOutput, recipeIndex, getCacheForType(type));
        if(foundRecipe == null) {
            foundRecipe = queryRecipeCaches(targetOutput, 0, getCacheForType(type));
            GuidebookMod.logger.warn(String.format("<recipe> index '%d' was not found in the list of cached %s recipes for '%s'. Falling back to the first occurrence.", recipeIndex, type.toString(), targetOutput.toString()));
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
            List<ItemStack> stackList = RecipeProvider.copyAndExpand(foundRecipe.getRecipeOutput());
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
            int height = HEIGHT[constantIndex];

            Stack[] components = new Stack[stackComponents.size()];
            stackComponents.toArray(components);
            RecipeProvider.ProvidedComponents result = new RecipeProvider.ProvidedComponents(height, components, background, ird);
            return result;
        } else GuidebookMod.logger.error(String.format("[CraftingRecipeProvider] Recipe not found for '%s' although hasRecipe(...) returned true. Something is wrong!", targetOutput.toString()));
        return null;
    }

}
