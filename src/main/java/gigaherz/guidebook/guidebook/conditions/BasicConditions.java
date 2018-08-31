package gigaherz.guidebook.guidebook.conditions;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

import gigaherz.guidebook.guidebook.BookParsingException;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.Predicate;

public abstract class BasicConditions implements Predicate<ConditionContext>
{
    public static void register()
    {
        ConditionManager.register("true", (doc, node) -> new True());
        ConditionManager.register("false", (doc, node) -> new False());
        ConditionManager.register("mod-loaded", (doc, node) -> new ModLoaded(parseModId(node)));
        ConditionManager.register("item-exists", (doc, node) -> new ItemExists(parseItemName(node)));
        ConditionManager.register("key-set", (doc, node) -> new KeySet(node));
        ConditionManager.register("has-item", (doc, node) -> new HasItem(parseItemName(node),node));
        ConditionManager.register("creative", (doc, node) -> new IsCreative());
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

    /***
     * Returns true if key for given keybind isn't "NONE". Requires 'key' to be specified as well.
     * 
     * @author Filmos
     */
    public static class KeySet extends BasicConditions
    {
        private final String key;

        public KeySet(Node node)
        {
        	Node att2 = node.getAttributes().getNamedItem("key");
            if (att2 == null || Strings.isNullOrEmpty(att2.getTextContent()))
                throw new BookParsingException("Missing required XML attribute 'key'.");
            this.key = att2.getTextContent();
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            try {
				java.lang.reflect.Field KEYBIND_ARRAY = KeyBinding.class.getDeclaredField("KEYBIND_ARRAY");
				KEYBIND_ARRAY.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<String, KeyBinding> binds = (Map<String, KeyBinding>) KEYBIND_ARRAY.get(null);
				if(binds.get(key).getKeyCode() != 0) return true;
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return false;
        }
    }
    
    /***
     * Returns true if player has given item in its inventory. Requires 'registry-name' to be specified as well.
     * 'Meta' attribute can also be used to specify meta of the item, if unspecified or set to "*" it will match any meta. 
     * 
     * @author Filmos
     */
    public static class HasItem extends BasicConditions
    {
        private final Item item;
        private int meta = -1;

        public HasItem(ResourceLocation item, Node attributes)
        {
        	//this.itemStack = new ItemStack(Item.REGISTRY.getObject(item),1,-1);
        	this.item = Item.REGISTRY.getObject(item);
        	Node attr = attributes.getAttributes().getNamedItem("meta");
            if (attr != null)
            {
                if (attr.getTextContent().equals("*"))
                    meta = -1;
                else
                    meta = Ints.tryParse(attr.getTextContent());
            }
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
        	//conditionContext.getPlayer().dimension
        	//conditionContext.getPlayer().isPotionActive(potionIn)
        	if(meta == -1) {
        		NonNullList<ItemStack> stacks = NonNullList.create();
				item.getSubItems(CreativeTabs.SEARCH, stacks);

                for (int i = 0; i < stacks.size(); i++)
                {
                    ItemStack subitem = stacks.get(i);

                    subitem = subitem.copy();
                    subitem.setCount(1);
                    if(conditionContext.getPlayer().inventory.hasItemStack(subitem))
                    	return true;
                }
        		return false;
        	}
        	else
        	return (conditionContext.getPlayer().inventory.hasItemStack(new ItemStack(item,1,meta)));
        }
    }

    /***
     * Returns true if player is in creative mode.
     * 
     * @author Filmos
     */
    public static class IsCreative extends BasicConditions
    {
        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return conditionContext.getPlayer().isCreative();
        }
    }
}
