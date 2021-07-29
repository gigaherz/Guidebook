package gigaherz.guidebook.common;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;

@Deprecated
public interface IModProxy
{
    @Deprecated
    default void displayBook(String book)
    {

    }

    @Deprecated
    default String getBookName(String book)
    {
        return String.format("Placeholder(%s)", book);
    }

    // The book registry is only available in the client.
    @Deprecated
    default Collection<ResourceLocation> getBooksList()
    {
        return Collections.emptyList();
    }

    // The book registry is only available in the client.
    @Deprecated
    default void registerBook(ResourceLocation bookLocation)
    {
    }
}
