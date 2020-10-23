package gigaherz.guidebook.guidebook.recipe;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RecipeLayoutProviders
{
    public static final VanillaRecipeLayoutProvider VANILLA = new VanillaRecipeLayoutProvider();

    private static final Map<ResourceLocation, IRecipeLayoutProvider> registry = Maps.newHashMap();

    public static void register(IRecipeType<?> type, VanillaRecipeLayoutProvider provider)
    {
        ResourceLocation id = Registry.RECIPE_TYPE.getKey(type);
        registry.put(id, provider);
    }

    public static void registerAlias(ResourceLocation id, VanillaRecipeLayoutProvider provider)
    {
        registry.put(id, provider);
    }

    static
    {
        register(IRecipeType.CRAFTING, VANILLA);
        register(IRecipeType.SMELTING, VANILLA);
        register(IRecipeType.CAMPFIRE_COOKING, VANILLA);
        register(IRecipeType.BLASTING, VANILLA);
        register(IRecipeType.SMOKING, VANILLA);
        register(IRecipeType.STONECUTTING, VANILLA);
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
