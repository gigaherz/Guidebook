package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.elements.Image;
import gigaherz.guidebook.guidebook.elements.Stack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

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
            CraftingRecipeProvider crafting = new CraftingRecipeProvider();
            event.getRegistry().register(crafting.new ShapedRecipeProvider());
            event.getRegistry().register(crafting.new ShapelessRecipeProvider());
            event.getRegistry().register(new FurnaceRecipeProvider());
        }
    }

    public abstract boolean hasRecipe(@Nonnull ItemStack targetOutput);

    @Nullable
    public abstract ProvidedComponents provideRecipeComponents(@Nonnull ItemStack targetOutput, int recipeIndex);

    public void reloadCache() {

    }

    protected static List<ItemStack> copyAndExpand(@Nonnull ItemStack stack) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        ItemStack base = stack.copy();
        stacks.add(base);

        if(base.isItemStackDamageable()) base.setItemDamage(0);
        if(base.getMetadata() == OreDictionary.WILDCARD_VALUE && !base.getHasSubtypes()) base.setItemDamage(0);
        else if(base.getMetadata() == OreDictionary.WILDCARD_VALUE && base.getHasSubtypes()){
            Item item = base.getItem();
            int stackSize = base.getCount();
            NBTTagCompound tag = base.hasTagCompound() ? base.getTagCompound().copy() : null;
            NonNullList<ItemStack> processed_items = NonNullList.create();
            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subItems);

            for (ItemStack subitem : subItems) {
                subitem = subitem.copy();
                subitem.setCount(stackSize);
                subitem.setTagCompound(tag);
                processed_items.add(subitem);
            }
            stacks = subItems;
        }
        return stacks;
    }

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
