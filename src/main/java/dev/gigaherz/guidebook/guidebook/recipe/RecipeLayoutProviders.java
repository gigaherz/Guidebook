package dev.gigaherz.guidebook.guidebook.recipe;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.Map;

public class RecipeLayoutProviders
{
    public static final VanillaRecipeLayoutProvider VANILLA = new VanillaRecipeLayoutProvider();

    private static final Map<ResourceLocation, IRecipeLayoutProvider> registry = Maps.newHashMap();

    public static void register(RecipeType<?> type, IRecipeLayoutProvider provider)
    {
        ResourceLocation id = ForgeRegistries.RECIPE_TYPES.getKey(type);
        registerAlias(id, provider);
    }

    public static void registerAlias(ResourceLocation id, IRecipeLayoutProvider provider)
    {
        if (registry.containsKey(id))
            throw new IllegalArgumentException("Duplicate recipe layout provider for " + id);
        registry.put(id, provider);
    }

    static
    {
        register(RecipeType.CRAFTING, VANILLA);
        register(RecipeType.SMELTING, VANILLA);
        register(RecipeType.CAMPFIRE_COOKING, VANILLA);
        register(RecipeType.BLASTING, VANILLA);
        register(RecipeType.SMOKING, VANILLA);
        register(RecipeType.STONECUTTING, VANILLA);
        // backward compatibility
        registerAlias(new ResourceLocation("minecraft:shaped"), VANILLA);
        registerAlias(new ResourceLocation("minecraft:shapeless"), VANILLA);
    }

    @Nonnull
    public static IRecipeLayoutProvider getProvider(ResourceLocation id)
    {
        IRecipeLayoutProvider value = registry.get(id);
        if (value == null)
            throw new IllegalArgumentException(String.format("There is no recipe layout provider named: '%s'", id));
        return value;
    }
}
