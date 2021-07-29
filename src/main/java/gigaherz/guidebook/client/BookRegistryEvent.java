package gigaherz.guidebook.client;

import gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }
}
