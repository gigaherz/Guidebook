package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import gigaherz.guidebook.guidebook.BookParsingException;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.w3c.dom.Node;

import java.util.function.Predicate;

public abstract class BasicConditions implements Predicate<ConditionContext>
{
    public static void register()
    {
        ConditionManager.register("true", (doc, node) -> new True());
        ConditionManager.register("false", (doc, node) -> new False());
        ConditionManager.register("mod-loaded", (doc, node) -> new ModLoaded(parseModId(node)));
        ConditionManager.register("item-exists", (doc, node) -> new ItemExists(parseItemName(node)));
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
            return Loader.isModLoaded(modId);
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
