package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookParsingException;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import net.minecraft.client.settings.KeyBinding;
import java.util.Map;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.google.common.base.Strings;

/***
 * Dynamic elements are basically text elements, which content depends on user settings.
 * 
 * @author Filmos
 */
public class ElementDynamic extends ElementSpan
{
	NamedNodeMap attr;

    public ElementDynamic(String text)
    {
        super("", true, true);
        //color = 0xFF8288FF;
    }
    public ElementDynamic()
    {
        super("", true, true);
        //color = 0xFF8288FF;
    }

	@SuppressWarnings("unchecked")
	@Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        String oldValue = text;
        text = "";
        
        switch(attr.getNamedItem("type").getTextContent()) {
        /***
         * This type displays keybind for given action. It requires "key" to be declared as well.
         * @author Filmos
         */
        case "key":
			try {
				java.lang.reflect.Field KEYBIND_ARRAY = KeyBinding.class.getDeclaredField("KEYBIND_ARRAY");
				KEYBIND_ARRAY.setAccessible(true);
				Map<String, KeyBinding> binds = (Map<String, KeyBinding>) KEYBIND_ARRAY.get(null);
				text = binds.get(attr.getNamedItem("key").getTextContent()).getDisplayName();
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
			break;
		/***
		 * This type displays name of the user reading this book.
		 * @author Filmos
		 */
        case "username":
        	text = ctx.getPlayer().getDisplayNameString();
        	break;
        case "config path":
        	text = GuidebookMod.configPath;
        	break;
        }
        return oldValue != text;
    }
    
    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        super.parse(book, attributes);
        attr = attributes;
        
        Node att = attributes.getNamedItem("type");
        if (att == null || Strings.isNullOrEmpty(att.getTextContent()))
            throw new BookParsingException("Missing required XML attribute 'type'.");
        
        switch(att.getTextContent()) { 
        	case "key":
        		Node att2 = attributes.getNamedItem("key");
                if (att2 == null || Strings.isNullOrEmpty(att2.getTextContent()))
                    throw new BookParsingException("Missing required XML attribute 'key'.");
        	break;
        }
    }
    
    @Override
    public Element copy()
    {
        ElementDynamic dyn = super.copy(new ElementDynamic());
        dyn.attr = attr;
        return dyn;
    }
}
