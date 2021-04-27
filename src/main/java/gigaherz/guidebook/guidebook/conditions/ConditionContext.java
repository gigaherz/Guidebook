package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;
import net.minecraft.client.entity.player.ClientPlayerEntity;

import java.util.function.Predicate;

public class ConditionContext
{
    private ClientPlayerEntity player;
    private BookDocument book;

    public ClientPlayerEntity getPlayer()
    {
        return player;
    }

    public void setPlayer(ClientPlayerEntity player)
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
