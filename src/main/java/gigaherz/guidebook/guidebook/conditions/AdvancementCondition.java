package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import gigaherz.guidebook.guidebook.BookParsingException;
import org.w3c.dom.Node;

import java.util.function.Predicate;

public abstract class AdvancementCondition implements Predicate<ConditionContext>
{
    public final String stageName;

    protected AdvancementCondition(String stageName)
    {
        this.stageName = stageName;
    }

    public static void register()
    {
        ConditionManager.register("advancement-locked", (doc, node) -> new Locked(parseStageName(node)));
        ConditionManager.register("advancement-unlocked", (doc, node) -> new Unlocked(parseStageName(node)));
    }

    public static class Locked extends AdvancementCondition
    {

        public Locked(String stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return false; // !Advancement.getStageData(conditionContext.getPlayer()).hasUnlockedStage(stageName);
        }
    }

    public static class Unlocked extends AdvancementCondition
    {

        public Unlocked(String stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return false; // Advancement.getStageData(conditionContext.getPlayer()).hasUnlockedStage(stageName);
        }
    }

    private static String parseStageName(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("advancement");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'advancement'.");

        String stageName = attr.getTextContent();
        if (Strings.isNullOrEmpty(stageName))
            throw new BookParsingException("Missing required XML attribute 'advancement'.");
        return stageName;
    }
}
