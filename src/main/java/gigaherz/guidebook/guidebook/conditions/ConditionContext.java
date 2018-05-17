package gigaherz.guidebook.guidebook.conditions;

import net.minecraft.entity.player.EntityPlayer;

public class ConditionContext
{
    private EntityPlayer player;

    public EntityPlayer getPlayer()
    {
        return player;
    }

    public void setPlayer(EntityPlayer player)
    {
        this.player = player;
    }
}
