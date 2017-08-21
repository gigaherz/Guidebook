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

/**
 * @author joazlazer
 * A class designed to provide both shaped and shapeless crafting recipes for display in Guidebooks
 */
class CraftingRecipeProvider
{
    public class ShapedRecipeProvider extends RecipeProvider
    {
        ShapedRecipeProvider()
        {
            this.setRegistryName(GuidebookMod.MODID, "shaped");
        }

        @Override
        public boolean hasRecipe(@Nonnull ItemStack targetOutput)
        {
            return hasCraftingRecipe(targetOutput, Type.SHAPED);
        }

        @Override
        public boolean hasRecipe(@Nonnull ResourceLocation recipeKey)
        {
            return hasCraftingRecipe(recipeKey, Type.SHAPED);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex)
        {
            return provideCraftingRecipeComponents(findRecipe(targetOutput, recipeIndex, Type.SHAPED), Type.SHAPED);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey)
        {
            return provideCraftingRecipeComponents(findRecipe(recipeKey), Type.SHAPED);
        }
    }

    public class ShapelessRecipeProvider extends RecipeProvider
    {
        ShapelessRecipeProvider()
        {
            this.setRegistryName(GuidebookMod.MODID, "shapeless");
        }

        @Override
        public void reloadCache()
        {
            // Only reload cache once
            reloadCaches();
        }

        @Override
        public boolean hasRecipe(@Nonnull ItemStack targetOutput)
        {
            return hasCraftingRecipe(targetOutput, Type.SHAPELESS);
        }

        @Override
        public boolean hasRecipe(@Nonnull ResourceLocation recipeKey)
        {
            return hasCraftingRecipe(recipeKey, Type.SHAPELESS);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex)
        {
            return provideCraftingRecipeComponents(findRecipe(targetOutput, recipeIndex, Type.SHAPELESS), Type.SHAPELESS);
        }

        @Nullable
        @Override
        public ProvidedComponents provideRecipeComponents(@Nonnull ResourceLocation recipeKey)
        {
            return provideCraftingRecipeComponents(findRecipe(recipeKey), Type.SHAPELESS);
        }
    }

    public enum Type
    {
        SHAPED,
        SHAPELESS;

        @Override
        public String toString()
        {
            if(this == SHAPED) return "shaped";
            else return "shapeless";
        }
    }

    private ArrayList<IRecipe> shapedRecipes;
    private ArrayList<IRecipe> shapelessRecipes;

    private void reloadCaches()
    {
        shapelessRecipes = new ArrayList<>();
        shapedRecipes = new ArrayList<>();
        for(IRecipe recipe : ForgeRegistries.RECIPES.getValues())
        {
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

    private ArrayList<IRecipe> getCacheForType(Type type)
    {
        return type == Type.SHAPELESS ? shapelessRecipes : shapedRecipes;
    }

    private boolean hasCraftingRecipe(ItemStack targetOutput, Type type)
    {
        // Query the shaped and shaped ore recipe caches
        return queryRecipeCaches(targetOutput, 0, getCacheForType(type)) != null;
    }

    private boolean hasCraftingRecipe(ResourceLocation recipeKey, Type type)
    {
        // Query the recipe registry to find the specified registry key
        if(type == Type.SHAPED && ForgeRegistries.RECIPES.containsKey(recipeKey))
        {
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeKey);
            if(recipe instanceof ShapedRecipes || recipe instanceof ShapedOreRecipe) return true;
            else
            {
                GuidebookMod.logger.warn(String.format("[CraftingRecipeProvider] Specified recipe '%s' was registered, but is not in the recipe category '%s'. Ignoring.", recipeKey, type.toString()));
                return false;
            }
        }
        else if (type == Type.SHAPELESS && ForgeRegistries.RECIPES.containsKey(recipeKey))
        {
            IRecipe recipe = ForgeRegistries.RECIPES.getValue(recipeKey);
            if(recipe instanceof ShapelessRecipes || recipe instanceof ShapelessOreRecipe) return true;
            else
            {
                GuidebookMod.logger.warn(String.format("[CraftingRecipeProvider] Specified recipe '%s' was registered, but is not in the recipe category '%s'. Ignoring.", recipeKey, type.toString()));
                return false;
            }
        }
        return false;
    }

    @Nullable
    private IRecipe queryRecipeCaches(@Nonnull ItemStack targetOutput, int recipeIndex, ArrayList<IRecipe> cache)
    {
        // Query either the shaped or shapeless recipe cache, but return the (recipeIndex + 1)th occurance
        for(IRecipe recipe : cache)
        {
            if(recipe.getRecipeOutput().isItemEqual(targetOutput))
            {
                if(recipeIndex > 0)
                {
                    --recipeIndex;
                }
                else
                {
                    return recipe;
                }
            }
        }
        return null;
    }

    @Nullable
    private IRecipe findRecipe(@Nonnull ResourceLocation recipeKey)
    {
        return ForgeRegistries.RECIPES.getValue(recipeKey);
    }

    @Nullable
    private IRecipe findRecipe(@Nonnull ItemStack targetOutput, int recipeIndex, Type type)
    {
        IRecipe foundRecipe = queryRecipeCaches(targetOutput, recipeIndex, getCacheForType(type));
        if(foundRecipe == null)
        {
            foundRecipe = queryRecipeCaches(targetOutput, 0, getCacheForType(type));
            GuidebookMod.logger.warn(String.format("[CraftingRecipeProvider] <recipe> index '%d' was not found in the list of cached %s recipes for '%s'. Falling back to the first occurrence.", recipeIndex, type.toString(), targetOutput.toString()));
        }
        return foundRecipe;
    }

    @Nullable
    private RecipeProvider.ProvidedComponents provideCraftingRecipeComponents(@Nullable IRecipe recipe, Type type)
    {
        if(recipe != null)
        {
            int constantIndex = recipe.getIngredients().size() <= 4 ? 1 : 0; // Whether to use the 3x3 (0) or 2x2 (1) grid
            ArrayList<Stack> stackComponents = new ArrayList<>();
            IRenderDelegate ird = (nav, top, left) -> { };
            int gridWidth = constantIndex == 0 ? 3 : 2;

            // Set up input slots
            for(int i = 0; i < recipe.getIngredients().size(); ++i)
            {
                Stack inputSlot = new Stack();
                ItemStack[] matching = recipe.getIngredients().get(i).getMatchingStacks();
                if(matching.length == 0) continue; // If the recipe area is blank, continue and ignore

                // Copy each stack
                inputSlot.stacks = new ItemStack[matching.length];
                for (int j = 0; j < matching.length; ++j)
                {
                    inputSlot.stacks[j] = matching[j].copy();
                }

                int posX = i % gridWidth;
                int posY = i / gridWidth;
                inputSlot.x = INPUT_SLOT_BASE_X[constantIndex] + (posX * INPUT_SLOT_OFFSET) + LEFT_OFFSET;
                inputSlot.y = INPUT_SLOT_BASE_Y[constantIndex] + (posY * INPUT_SLOT_OFFSET);
                stackComponents.add(inputSlot); // Only add the element if there is an item in the slot
            }

            // Set up output slot element
            Stack outputSlot = new Stack();
            stackComponents.add(outputSlot);
            List<ItemStack> stackList = RecipeProvider.copyAndExpand(recipe.getRecipeOutput());
            outputSlot.stacks = stackList.toArray(new ItemStack[stackList.size()]);
            outputSlot.x = OUTPUT_SLOT_X[constantIndex] + LEFT_OFFSET;
            outputSlot.y = OUTPUT_SLOT_Y[constantIndex];

            // Set up background image
            Image background = new Image();
            background.textureLocation = BACKGROUND_TEXTURE;
            background.x = LEFT_OFFSET;
            background.y = 0;
            background.tx = BACKGROUND_U[constantIndex];
            background.ty = BACKGROUND_V[constantIndex];
            background.w = BACKGROUND_W[constantIndex];
            background.h = BACKGROUND_H[constantIndex];

            // Set up overall height
            int height = HEIGHT[constantIndex];

            Stack[] components = new Stack[stackComponents.size()];
            stackComponents.toArray(components);
            return new RecipeProvider.ProvidedComponents(height, components, background, ird);
        }
        else GuidebookMod.logger.error(String.format("[CraftingRecipeProvider] %s Recipe not found although hasRecipe(...) returned true. Something is wrong!", type.toString()));
        return null;
    }
}
