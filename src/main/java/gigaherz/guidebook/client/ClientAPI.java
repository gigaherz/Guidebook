package gigaherz.guidebook.client;

import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.client.GuiGuidebook;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ClientAPI
{
    public static void displayBook(String book)
    {
        ResourceLocation loc = new ResourceLocation(book);
        BookDocument br = BookRegistry.get(loc);
        if (br != null && br.chapterCount() > 0)
            Minecraft.getInstance().setScreen(new GuiGuidebook(loc));
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
