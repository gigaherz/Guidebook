package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.drawing.Rect;
import gigaherz.guidebook.guidebook.drawing.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.List;

public class ElementStack extends Element
{
    public static final String WILDCARD = "*";
    public int CYCLE_TIME = 1000;

    public final NonNullList<ItemStack> stacks = NonNullList.create();

    public ElementStack()
    {
        // default size
        w = 16;
        h = 16;
    }

    private Size getVisualSize()
    {
        int width = (int) (w * scale);
        int height = (int) (h * scale);
        return new Size(width, height);
    }

    private VisualStack getVisual()
    {
    	VisualStack vis = new VisualStack(stacks, getVisualSize(), position, baseline, verticalAlignment, scale, z, CYCLE_TIME);
    	vis.clickData = clickData;
        return vis;
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return Collections.singletonList(getVisual());
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        VisualStack element = getVisual();
        element.position = applyPosition(bounds.position, bounds.position);
        paragraph.add(element);
        if (position != POS_RELATIVE)
            return bounds.position.y;
        return bounds.position.y + element.size.height;
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes) 
    {
        super.parse(book, attributes);

        Node attr = attributes.getNamedItem("speed");
        if(attr != null) {
        	CYCLE_TIME = Integer.parseInt(attr.getTextContent());
        }
        subParse(book, attributes);
    }
    @Override
    public void parseChildNodes(IConditionSource book, Node element)
    {
    	NodeList childList = element.getChildNodes();
    	int l = childList.getLength();
        for (int q = 0; q < l; q++)
        {
            Node childNode = childList.item(q);
            if(childNode.getNodeName()=="item")
            	subParse(book, childNode.getAttributes());
        }
    }
    
    /***
     * Adds items to the stack based on attributes.
     * Used multiple item if stack is constructed with <item> elements
     * @param book Condition source
     * @param attributes Attributes
     */
    void subParse(IConditionSource book, NamedNodeMap attributes)
    {
        int meta = 0;
        int stackSize = 1;
        int startI = stacks.size();
        NBTTagCompound tag = new NBTTagCompound();

        Node attr = attributes.getNamedItem("meta");
        if (attr != null)
        {
            if (attr.getTextContent().equals(WILDCARD))
                meta = -1;
            else
                meta = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("count");
        if (attr != null)
        {
            stackSize = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("tag");
        if (attr != null)
        {
            try
            {
                tag = JsonToNBT.getTagFromJson(attr.getTextContent());
            }
            catch (NBTException e)
            {
                GuidebookMod.logger.warn("Invalid tag format: " + e.getMessage());
            }
        }

        attr = attributes.getNamedItem("item");
        if (attr != null)
        {
            String itemName = attr.getTextContent();

            Item item = Item.REGISTRY.getObject(new ResourceLocation(itemName));

            if (item != null)
            {
                if (((meta == OreDictionary.WILDCARD_VALUE) || meta == -1) && item.getHasSubtypes())
                {
                    item.getSubItems(CreativeTabs.SEARCH, stacks);

                    for (int i = startI; i < stacks.size(); i++)
                    {
                        ItemStack subitem = stacks.get(i);

                        subitem = subitem.copy();

                        subitem.setCount(stackSize);
                        subitem.setTagCompound(tag);

                        stacks.set(i, subitem);
                    }
                }
                else
                {
                    ItemStack stack = new ItemStack(item, stackSize, meta);
                    stack.setTagCompound(tag);
                    stacks.add(stack);
                }
            }
        }

        //get stacks from ore dictionary
        attr = attributes.getNamedItem("ore");
        if (attr != null)
        {
            String oreName = attr.getTextContent();
            //list of matching item stack; may contain wildcard meta data
            NonNullList<ItemStack> items = OreDictionary.getOres(oreName);

            if (items.size() != 0)
            {
                //foreach item: try to resolve wildcard meta data
                for (ItemStack item : items)
                {
                    //make sure not to mess up ore dictionary item stacks
                    item = item.copy();
                    meta = item.getMetadata();

                    if (meta == OreDictionary.WILDCARD_VALUE && item.getHasSubtypes())
                    {
                        //replace wildcard metas with subitems
                        NonNullList<ItemStack> subitems = NonNullList.create();
                        item.getItem().getSubItems(CreativeTabs.SEARCH, subitems);
                        for (ItemStack subitem : subitems)
                        {
                            //just in case the ItemStack instance is not just a copy or a new instance
                            subitem = subitem.copy();

                            subitem.setCount(stackSize);
                            subitem.setTagCompound(tag);
                            stacks.add(subitem);
                        }
                    }
                    else
                    {
                        item.setCount(stackSize);
                        stacks.add(item);
                    }
                }
            }
        }
    }

    @Override
    public Element copy()
    {
        ElementStack newStack = super.copy(new ElementStack());
        for(ItemStack stack : stacks)
        {
            newStack.stacks.add(stack.copy());
        }
        return newStack;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
