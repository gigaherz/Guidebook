package gigaherz.guidebook.common;

import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;

public interface IModProxy
{
    default void displayBook(String book)
    {

    }

    default String getBookName(String book)
    {
        return String.format("Guidebook - %s unknown", book);
    }

    // The book registry is only available in the client.
    default Collection<ResourceLocation> getBooksList()
    {
        return Collections.emptyList();
    }

    // The book registry is only available in the client.
    default void registerBook(ResourceLocation bookLocation)
    {
    }
}
