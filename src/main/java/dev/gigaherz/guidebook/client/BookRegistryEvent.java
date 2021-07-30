package dev.gigaherz.guidebook.client;

import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

public class BookRegistryEvent extends Event
{
    public void register(ResourceLocation bookLocation)
    {
        BookRegistry.registerBook(bookLocation);
    }
}
