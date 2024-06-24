package dev.gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import dev.gigaherz.guidebook.guidebook.BookParsingException;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public abstract class AdvancementCondition implements Predicate<ConditionContext>
{
    public final ResourceLocation advancement;

    @Nullable
    static AdvancementProgress getAdvancementProgress(ResourceLocation advancement)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        ClientAdvancements mgr = player.connection.getAdvancements();
        AdvancementHolder holder = mgr.get(advancement);
        return holder != null ? mgr.progress.get(holder) : null;
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
        return ResourceLocation.parse(stageName);
    }
}
