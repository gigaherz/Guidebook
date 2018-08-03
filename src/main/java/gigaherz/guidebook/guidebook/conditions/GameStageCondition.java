package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import gigaherz.guidebook.guidebook.BookParsingException;
import net.darkhax.gamestages.GameStageHelper;
import org.w3c.dom.Node;

import java.util.function.Predicate;

public abstract class GameStageCondition implements Predicate<ConditionContext>
{
    public final String stageName;

    protected GameStageCondition(String stageName)
    {
        this.stageName = stageName;
    }

    public static void register()
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
            return !GameStageHelper.clientHasStage(conditionContext.getPlayer(), stageName);
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
            return GameStageHelper.clientHasStage(conditionContext.getPlayer(), stageName);
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
