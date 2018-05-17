package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import gigaherz.guidebook.guidebook.BookParsingException;
import net.darkhax.gamestages.capabilities.PlayerDataHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.w3c.dom.Node;

public abstract class GameStageCondition implements IDisplayCondition
{
    public final String stageName;

    protected GameStageCondition(String stageName)
    {
        this.stageName = stageName;
    }

    @CapabilityInject(PlayerDataHandler.IStageData.class)
    public static void register(Capability<PlayerDataHandler.IStageData> cap)
    {
        ConditionManager.register("stage-locked", (doc, node) -> new Locked(parseStageName(node)));
        ConditionManager.register("stage-unlocked", (doc, node) -> new Unlocked(parseStageName(node)));
    }

    public static class Locked extends GameStageCondition
    {

        public Locked(String stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return !PlayerDataHandler.getStageData(conditionContext.getPlayer()).hasUnlockedStage(stageName);
        }
    }

    public static class Unlocked extends GameStageCondition
    {

        public Unlocked(String stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return PlayerDataHandler.getStageData(conditionContext.getPlayer()).hasUnlockedStage(stageName);
        }
    }

    private static String parseStageName(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("stage");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'stage'.");

        String stageName = attr.getTextContent();
        if (Strings.isNullOrEmpty(stageName))
            throw new BookParsingException("Missing required XML attribute 'stage'.");
        return stageName;
    }
}
