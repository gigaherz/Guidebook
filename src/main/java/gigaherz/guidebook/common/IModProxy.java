package gigaherz.guidebook.common;

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
}
