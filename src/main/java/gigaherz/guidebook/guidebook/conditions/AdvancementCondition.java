package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import gigaherz.guidebook.guidebook.BookParsingException;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AdvancementCondition implements Predicate<ConditionContext>
{
    public final ResourceLocation advancement;

    @SuppressWarnings("unchecked")
    @Nullable
    static AdvancementProgress getAdvancementProgress(ResourceLocation advancement)
    {
        ClientAdvancementManager mgr = Minecraft.getInstance().player.connection.getAdvancementManager();
        Advancement adv = mgr.getAdvancementList().getAdvancement(advancement);
        Map<Advancement, AdvancementProgress> advancementToProgress =
                ObfuscationReflectionHelper.getPrivateValue(ClientAdvancementManager.class, mgr, "field_192803_d");
        return advancementToProgress.get(adv);
    }

    protected AdvancementCondition(ResourceLocation advancement)
    {
        this.advancement = advancement;
    }

    public static void register()
    {
        ConditionManager.register("advancement-locked", (doc, node) -> new Locked(parseAdvancementLocation(node)));
        ConditionManager.register("advancement-unlocked", (doc, node) -> new Unlocked(parseAdvancementLocation(node)));
    }

    public static class Locked extends AdvancementCondition
    {
        public Locked(ResourceLocation stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            AdvancementProgress p = getAdvancementProgress(advancement);
            return p == null || !p.isDone();
        }
    }

    public static class Unlocked extends AdvancementCondition
    {
        public Unlocked(ResourceLocation stageName)
        {
            super(stageName);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            AdvancementProgress p = getAdvancementProgress(advancement);
            return p != null && p.isDone();
        }
    }

    private static ResourceLocation parseAdvancementLocation(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("advancement");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'advancement'.");

        String stageName = attr.getTextContent();
        if (Strings.isNullOrEmpty(stageName))
            throw new BookParsingException("Missing required XML attribute 'advancement'.");
        return new ResourceLocation(stageName);
    }
}
