package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

public abstract class RecipeProvider extends IForgeRegistryEntry.Impl<RecipeProvider> {
    public static IForgeRegistry<RecipeProvider> registry;

    @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerRegistries(RegistryEvent.NewRegistry event) {
            RegistryBuilder rb = new RegistryBuilder<RecipeProvider>();
            rb.setType(RecipeProvider.class);
            rb.setName(new ResourceLocation(GuidebookMod.MODID, "recipe_provider"));
            registry = rb.create();
        }

        @SubscribeEvent
        public static void registerDefaults(RegistryEvent.Register<RecipeProvider> event) {
            event.getRegistry().register(new FurnaceRecipeProvider());
        }
    }

    public abstract boolean hasRecipe(ItemStack targetOutput);
    public abstract ProvidedComponents provideRecipeComponents(ItemStack targetOutput, int recipeIndex);

    public static class ProvidedComponents {
        public int height = 0;
        public Stack[] recipeComponents;
        public Image background;
        public IRenderDelegate delegate;
        public ProvidedComponents(int h, Stack[] rc, Image bkgrd, IRenderDelegate ird) {
            height = h;
            recipeComponents = rc;
            background = bkgrd;
            delegate = ird;
        }
    }
}
