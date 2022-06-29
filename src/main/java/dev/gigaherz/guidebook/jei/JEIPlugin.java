package dev.gigaherz.guidebook.jei;

import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.GuidebookItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JEIPlugin implements IModPlugin
{
    private static final ResourceLocation UID = new ResourceLocation(GuidebookMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid()
    {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry)
    {
        subtypeRegistry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, GuidebookMod.guidebook, (ingredient, context) -> {

            if (ingredient.getItem() instanceof GuidebookItem item)
            {
                var key = item.getBookLocation(ingredient);
                return key == null ? "" : "book_" + key;
            }

            return "";
        });
    }
}
