package gigaherz.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        GuidebookMod.proxy.registerBook(bookLocation);
    }
}
