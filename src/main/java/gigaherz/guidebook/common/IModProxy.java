package gigaherz.guidebook.common;

import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;

public interface IModProxy
{
    default void init()
    {

    }

    default void displayBook(String book)
    {

    }

    default void preInit()
    {

    }

    default String getBookName(String book)
    {
        return String.format("Guidebook - %s unknown", book);
    }

    default Collection<ResourceLocation> getBooksList()
    {
        return Collections.emptyList();
    }
}
