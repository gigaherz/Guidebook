package gigaherz.guidebook.guidebook.recipe;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Map;
import java.util.Optional;

public class RecipeProviders
{
    private static final Map<IRecipeType<?>, IRecipeProvider> registry = Maps.newHashMap();

    static
    {
        CraftingRecipeProvider crafting = new CraftingRecipeProvider();
        registry.put(IRecipeType.CRAFTING, crafting);
        registry.put(IRecipeType.SMELTING, crafting);
        registry.put(IRecipeType.CAMPFIRE_COOKING, crafting);
        registry.put(IRecipeType.BLASTING, crafting);
        registry.put(IRecipeType.SMOKING, crafting);
        registry.put(IRecipeType.STONECUTTING, crafting);
    }

    public static Either<IRecipeProvider, String> getProvider(ResourceLocation id)
    {
        return Registry.RECIPE_TYPE.getValue(id).map(iRecipeType -> Optional.of(registry.get(iRecipeType))
                .map(Either::<IRecipeProvider, String>left)
                .orElseGet(() -> Either.right("There is no recipe provider for recipe type '" + id + "'"))
        ).orElseGet(() -> Either.right("There is no recipe type with id '" + id + "'"));
    }
}
