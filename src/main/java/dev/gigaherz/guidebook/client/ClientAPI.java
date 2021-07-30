package dev.gigaherz.guidebook.client;

import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.client.GuidebookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ClientAPI
{
    public static void displayBook(String book)
    {
        ResourceLocation loc = new ResourceLocation(book);
        BookDocument br = BookRegistry.get(loc);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getInstance().setScreen(new GuidebookScreen(loc));
    }

    public static String getBookName(String book)
    {
        BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
        if (bookDocument != null)
        {
            String name = bookDocument.getName();
            if (name != null)
                return name;
        }
        return "Missing Book";
    }
}
