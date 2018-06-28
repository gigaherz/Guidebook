package gigaherz.guidebook.guidebook.conditions;

import net.minecraft.client.entity.EntityPlayerSP;

public class ConditionContext
{
    private EntityPlayerSP player;

    public EntityPlayerSP getPlayer()
    {
        return player;
    }

    public void setPlayer(EntityPlayerSP player)
    {
        this.player = player;
    }
}
