package gigaherz.guidebook.guidebook.conditions;

import net.minecraft.client.entity.player.ClientPlayerEntity;

public class ConditionContext
{
    private ClientPlayerEntity player;

    public ClientPlayerEntity getPlayer()
    {
        return player;
    }

    public void setPlayer(ClientPlayerEntity player)
    {
        this.player = player;
    }
}
