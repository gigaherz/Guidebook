package dev.gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import dev.gigaherz.guidebook.guidebook.BookParsingException;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.w3c.dom.Node;

import java.util.function.Predicate;

public abstract class BasicConditions implements Predicate<ConditionContext>
{
    public static void register()
    {
        ConditionManager.register("true", (node) -> new True());
        ConditionManager.register("false", (node) -> new False());
        ConditionManager.register("mod-loaded", (node) -> new ModLoaded(parseModId(node)));
        ConditionManager.register("item-exists", (node) -> new ItemExists(parseItemName(node)));
        ConditionManager.register("condition", (node) -> new Ref(parseConditionId(node)));
    }

    public static class True extends BasicConditions
    {
        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return true;
        }
    }

    public static class False extends BasicConditions
    {
        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return false;
        }
    }

    public static class Ref extends BasicConditions
    {
        private final String ref;
        private Predicate<ConditionContext> condition;

        public Ref(String ref)
        {
            this.ref = ref;
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            if (condition == null)
                condition = conditionContext.getBook().getCondition(ref);
            return condition.test(conditionContext);
        }
    }

    public static class ModLoaded extends BasicConditions
    {
        private final String modId;

        public ModLoaded(String modId)
        {
            this.modId = modId;
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return ModList.get().isLoaded(modId);
        }
    }

    public static class ItemExists extends BasicConditions
    {
        private final ResourceLocation item;

        public ItemExists(ResourceLocation item)
        {
            this.item = item;
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return ForgeRegistries.ITEMS.containsKey(item);
        }
    }

    private static String parseConditionId(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("condition");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'condition'.");

        String cond = attr.getTextContent();
        if (Strings.isNullOrEmpty(cond))
            throw new BookParsingException("Missing required XML attribute 'condition'.");
        return cond;
    }

    private static String parseModId(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("modid");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'modid'.");

        String modId = attr.getTextContent();
        if (Strings.isNullOrEmpty(modId))
            throw new BookParsingException("Missing required XML attribute 'modid'.");
        return modId;
    }

    private static ResourceLocation parseItemName(Node xmlNode)
    {
        Node attr = xmlNode.getAttributes().getNamedItem("registry-name");
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute 'registry-name'.");

        String name = attr.getTextContent();
        if (Strings.isNullOrEmpty(name))
            throw new BookParsingException("Missing required XML attribute 'registry-name'.");
        return new ResourceLocation(name);
    }
}
