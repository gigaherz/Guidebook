package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Stack implements IHoverPageElement, IClickablePageElement
{
    //The time each stack displayed lasts in ms.
    public static final int CYCLE_TIME=1000;//=1s

    public ItemStack[] stacks;
    public int x = 0;
    public int y = 0;
    public int z = 0;
    public float scale = 1.0f;

    public Rectangle bounds;

    public Stack()
    {
    }

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        left += x;
        top += y;
        int width = (int) (16 * scale);
        int height = (int) (16 * scale);
        bounds = new Rectangle(left, top, width, height);

        ItemStack stack=getCurrentStack();

        if (stack != null)
        {
            nav.drawItemStack(left, top, z, stack, 0xFFFFFFFF, scale);
        }

        return 0;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        int meta = 0;
        int stackSize = 1;
        NBTTagCompound tag = new NBTTagCompound();

        Node attr = attributes.getNamedItem("meta");
        if (attr != null)
        {
            // meta="*" -> wildcard (for both blocks and items)
            if(attr.getTextContent().equals("*"))
                meta=-1;
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
                //if wildcard
                if( (item instanceof ItemBlock && meta==OreDictionary.WILDCARD_VALUE) || meta==-1 ){
                    //init empty list to fill with resolved items
                    NonNullList<ItemStack> processed_items=NonNullList.create();
                    //init empty subitems list
                    NonNullList<ItemStack> subitems=NonNullList.create();
                    //fill list
                    item.getSubItems(item,null,subitems);
                    //iterate over the list
                    for (ItemStack subitem:subitems) {
                        //just in case the ItemStack instance is not just a copy or a new instance
                        subitem=subitem.copy();

                        //set count and tag
                        subitem.setCount(stackSize);
                        subitem.setTagCompound(tag);

                        //add to processed list
                        processed_items.add(subitem);
                    }
                    //save processed list into the array
                    stacks = subitems.toArray(new ItemStack[subitems.size()]);
                }else{
                    stacks = new ItemStack[]{new ItemStack(item, stackSize, meta)};
                    stacks[0].setTagCompound(tag);
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

            if (items.size()!=0)
            {
                //init empty list to fill with resolved items
                NonNullList<ItemStack> items_processed=NonNullList.create();

                //foreach item: try to resolve wildcard meta data
                for (ItemStack item:items) {
                    //make sure not to mess up ore dictionary item stacks
                    item=item.copy();

                    if(item.getItem() instanceof ItemBlock && item.getItemDamage()==OreDictionary.WILDCARD_VALUE){
                        //replace wildcard metas with subitems
                        NonNullList<ItemStack> subitems=NonNullList.create();
                        item.getItem().getSubItems(item.getItem(),null,subitems);
                        for (ItemStack subitem:subitems) {
                            //just in case the ItemStack instance is not just a copy or a new instance
                            subitem=subitem.copy();

                            subitem.setCount(stackSize);
                            subitem.setTagCompound(tag);
                            items_processed.add(subitem);
                        }
                    }else{
                        item.setCount(stackSize);
                        items_processed.add(item);
                    }
                }

                //
                stacks = items_processed.toArray(new ItemStack[items_processed.size()]);
            }
        }

        attr = attributes.getNamedItem("x");
        if (attr != null)
        {
            x = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("y");
        if (attr != null)
        {
            y = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("z");
        if (attr != null)
        {
            z = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("scale");
        if (attr != null)
        {
            Float f = Floats.tryParse(attr.getTextContent());
            scale = f != null ? f : scale;
        }
    }

    @Override
    public IPageElement copy()
    {
        Stack stack = new Stack();
        if(this.stacks!=null){
            stack.stacks=new ItemStack[this.stacks.length];
            for(int i=0;i<this.stacks.length;i++){
                stack.stacks[i] = this.stacks[i]!=null ? this.stacks[i].copy() : null;
            }
        }else{
            stack.stacks=null;
        }
        stack.x = x;
        stack.y = y;
        return stack;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        ItemStack stack=getCurrentStack();
        if (stack != null)
        {
            nav.drawTooltip(stack, x, y);
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        PageRef ref=nav.getBook().getStackRef(getCurrentStack());
        if(ref!=null)
            nav.navigateTo(ref);
    }

    public ItemStack getCurrentStack(){
        if(stacks==null||stacks.length==0)
            return null;
        long time=System.currentTimeMillis();
        return stacks[(int)((time/1000)%stacks.length)];
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }
}
