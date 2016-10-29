package gigaherz.guidebook.client;

import gigaherz.guidebook.guidebook.client.BookRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }
}
