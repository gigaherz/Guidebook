package dev.gigaherz.guidebook.client;

import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

/**
 * @deprecated This hasn't been working for a while and will be removed soon.
 */
@Deprecated(forRemoval = true)
public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }
}
