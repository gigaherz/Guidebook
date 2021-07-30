package dev.gigaherz.guidebook.guidebook.conditions;

import dev.gigaherz.guidebook.guidebook.BookDocument;
import net.minecraft.client.player.LocalPlayer;

public class ConditionContext
{
    private LocalPlayer player;
    private BookDocument book;

    public LocalPlayer getPlayer()
    {
        return player;
    }

    public void setPlayer(LocalPlayer player)
    {
        this.player = player;
    }

    public BookDocument getBook()
    {
        return book;
    }

    public void setBook(BookDocument book)
    {
        this.book = book;
    }
}
