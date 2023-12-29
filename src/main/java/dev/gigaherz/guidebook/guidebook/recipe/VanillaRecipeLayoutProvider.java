package dev.gigaherz.guidebook.guidebook.recipe;

import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.elements.ElementImage;
import dev.gigaherz.guidebook.guidebook.elements.ElementStack;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.IShapedRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * @author joazlazer
 * A class designed to provide both shaped and shapeless crafting recipes for display in Guidebooks
 */
public class VanillaRecipeLayoutProvider implements IRecipeLayoutProvider
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

    @Nonnull
    @Override
    public RecipeLayout getRecipeLayout(@Nonnull Level world, @Nonnull ItemStack targetOutput, int recipeIndex)
    {
        RecipeHolder<?> recipe = world.getRecipeManager().getRecipes().stream()
                .filter(r -> !r.value().isSpecial() && ItemStack.isSameItem(targetOutput, r.value().getResultItem(world.registryAccess())))
                .skip(recipeIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Recipe not found for provided output item: %s", targetOutput)));
        return getRecipeLayout(recipe);
    }

    @Nonnull
    @Override
    public RecipeLayout getRecipeLayout(@Nonnull Level world, @Nonnull ResourceLocation recipeKey)
    {
        RecipeHolder<?> recipe = world.getRecipeManager().byKey(recipeKey)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Recipe not found for registry name: %s", recipeKey)));
        return getRecipeLayout(recipe);
    }

    private RecipeLayout getRecipeLayout(@Nonnull RecipeHolder<?> recipeHolder)
    {
        var recipe = recipeHolder.value();
        int gridWidth;
        int recipeGraphic;
        if (recipe instanceof AbstractCookingRecipe)
        {
            gridWidth = 1;
            recipeGraphic = 2;
        }
        else
        {
            int gridHeight;
            if (recipe instanceof IShapedRecipe)
            {
                IShapedRecipe<?> shapedRecipe = (IShapedRecipe<?>) recipe;
                gridWidth = shapedRecipe.getRecipeWidth();
                gridHeight = shapedRecipe.getRecipeHeight();
            }
            else
            {
                int ingredients = recipe.getIngredients().size();
                gridWidth = Mth.ceil(Math.sqrt(ingredients));
                gridHeight = Mth.ceil(ingredients / (double) gridWidth);
            }

            switch (Math.max(gridWidth, gridHeight))
            {
                case 1:
                    recipeGraphic = 3;
                    break;
                case 2:
                    recipeGraphic = 1;
                    break;
                default:
                    recipeGraphic = 0;
                    break;
            }
        }

        ArrayList<ElementStack> stackComponents = new ArrayList<>();
        @Nullable
        VisualElement additionalRenderer = null;

        // Set up input slots
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); ++i)
        {
            ElementStack inputSlot = new ElementStack(false, false, TextStyle.DEFAULT);
            ItemStack[] matching = ingredients.get(i).getItems();
            if (matching.length == 0) continue; // If the recipe area is blank, continue and ignore

            // Copy each stack
            for (int j = 0; j < matching.length; ++j)
            {
                inputSlot.stacks.add(matching[j].copy());
            }

            int posX = i % gridWidth;
            int posY = i / gridWidth;
            inputSlot.x = INPUT_SLOT_BASE_X[recipeGraphic] + (posX * INPUT_SLOT_OFFSET) + LEFT_OFFSET;
            inputSlot.y = INPUT_SLOT_BASE_Y[recipeGraphic] + (posY * INPUT_SLOT_OFFSET);
            stackComponents.add(inputSlot); // Only add the element if there is an item in the slot
        }

        // Set up output slot element
        ElementStack outputSlot = new ElementStack(false, false, TextStyle.DEFAULT);
        stackComponents.add(outputSlot);
        ItemStack undamaged = unDamage(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
        outputSlot.stacks.add(undamaged);
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

        return new RecipeLayout(height, components, background, additionalRenderer);
    }

    public static ItemStack unDamage(@Nonnull ItemStack stack)
    {
        ItemStack base = stack.copy();
        if (base.isDamageableItem())
            base.setDamageValue(0);
        return base;
    }
}
