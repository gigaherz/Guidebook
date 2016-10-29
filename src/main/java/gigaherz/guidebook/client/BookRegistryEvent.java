package gigaherz.guidebook.client;

import gigaherz.guidebook.guidebook.client.BookDocument;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        BookDocument.registerBook(bookLocation);
    }
}
