package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookParsingException;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.config.Configuration;

import java.io.File;
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
	public static Configuration config;
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
    	/***
    	 * This type displays full path to the config folder.
    	 * @author Filmos
    	 */
        case "config path":
        	text = GuidebookMod.configPath;
        	break;
    	/***
    	 * This type displays given field from config file. Requires following attributes:
    	 * 'file' - name of the file inside the config folder, can also be a sub-path
    	 * 'category' - name of category in which field is located (this is that text right outside { inside config file)
    	 * 'field' - name of field to check
    	 * 'type' - type of that field, possible values are "int", "double", "string" and "bool"
    	 * @author Filmos
    	 */
        case "config":
        	if(config != null) {
        		config.load();
        		String category = attr.getNamedItem("cat").getTextContent();
        		String field = attr.getNamedItem("field").getTextContent();
            	switch(attr.getNamedItem("field-type").getTextContent()) {
            	case "bool":
            		text = config.get(category, field, false).getBoolean()?"true":"false";
            		break;
            	case "int":
            		text = String.valueOf(config.get(category, field, 0).getInt());
            		break;
            	case "double":
            		text = String.valueOf(config.get(category, field, 0).getDouble());
            		break;
            	case "string":
            		text = config.get(category, field, "").getString();
            		break;
            	}
        	}
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
        	case "config":
            	Node attr = attributes.getNamedItem("file");
                if (attr == null || Strings.isNullOrEmpty(attr.getTextContent()))
                    throw new BookParsingException("Missing required XML attribute 'key'.");
        		File f = new File(GuidebookMod.configPath+"\\"+attr.getTextContent());
    			if(f.exists() && !f.isDirectory())
    				config = new Configuration(f);
    			
    			attr = attributes.getNamedItem("cat");
                if (attr == null || Strings.isNullOrEmpty(attr.getTextContent()))
                    throw new BookParsingException("Missing required XML attribute 'cat'.");
    			attr = attributes.getNamedItem("field");
                if (attr == null || Strings.isNullOrEmpty(attr.getTextContent()))
                    throw new BookParsingException("Missing required XML attribute 'field'.");
    			attr = attributes.getNamedItem("field-type");
                if (attr == null || Strings.isNullOrEmpty(attr.getTextContent()))
                    throw new BookParsingException("Missing required XML attribute 'field-type'.");
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
