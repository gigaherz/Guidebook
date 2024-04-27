package dev.gigaherz.guidebook.client;

import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.client.GuidebookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ClientAPI
{
    public static void displayBook(ResourceLocation book)
    {
        BookDocument br = BookRegistry.get(book);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getInstance().setScreen(new GuidebookScreen(book));
    }

    public static String getBookName(ResourceLocation book)
    {
        BookDocument bookDocument = BookRegistry.get(book);
        if (bookDocument != null)
        {
            String name = bookDocument.getName();
            if (name != null)
                return name;
        }
        return "Missing Book";
    }
}
