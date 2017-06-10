package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.List;

public class Stack implements IHoverPageElement, IClickablePageElement
{
    public static final int CYCLE_TIME = 1000;//=1s
    public static final String WILDCARD = "*";

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

        ItemStack stack = getCurrentStack();

        if (stack != null && stack.stackSize > 0)
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
                    List<ItemStack> processedItems = Lists.newArrayList();
                    List<ItemStack> subitems = Lists.newArrayList();

                    item.getSubItems(item, null, subitems);

                    for (ItemStack subitem : subitems)
                    {
                        if (subitem != null)
                        {
                            subitem = subitem.copy();

                            subitem.stackSize = stackSize;
                            subitem.setTagCompound(tag);

                            processedItems.add(subitem);
                        }
                    }

                    stacks = processedItems.toArray(new ItemStack[processedItems.size()]);
                }
                else
                {
                    ItemStack stack = new ItemStack(item, stackSize, meta);
                    stack.setTagCompound(tag);
                    stacks = new ItemStack[]{stack};
                }
            }
        }

        //get stacks from ore dictionary
        attr = attributes.getNamedItem("ore");
        if (attr != null)
        {
            String oreName = attr.getTextContent();
            //list of matching item stack; may contain wildcard meta data
            List<ItemStack> items = OreDictionary.getOres(oreName);

            if (items.size() != 0)
            {
                //init empty list to fill with resolved items
                List<ItemStack> items_processed = Lists.newArrayList();

                //foreach item: try to resolve wildcard meta data
                for (ItemStack item : items)
                {
                    //make sure not to mess up ore dictionary item stacks
                    item = item.copy();

                    if (meta == OreDictionary.WILDCARD_VALUE && item.getHasSubtypes())
                    {
                        //replace wildcard metas with subitems
                        List<ItemStack> subitems = Lists.newArrayList();
                        item.getItem().getSubItems(item.getItem(), null, subitems);
                        for (ItemStack subitem : subitems)
                        {
                            //just in case the ItemStack instance is not just a copy or a new instance
                            subitem = subitem.copy();

                            subitem.stackSize = stackSize;
                            subitem.setTagCompound(tag);
                            items_processed.add(subitem);
                        }
                    }
                    else
                    {
                        item.stackSize = stackSize;
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
        if (this.stacks != null)
        {
            stack.stacks = new ItemStack[this.stacks.length];
            for (int i = 0; i < this.stacks.length; i++)
            {
                stack.stacks[i] = this.stacks[i].copy();
            }
        }
        else
        {
            stack.stacks = null;
        }
        stack.x = x;
        stack.y = y;
        return stack;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        ItemStack stack = getCurrentStack();
        if (stack != null && stack.stackSize > 0)
        {
            nav.drawTooltip(stack, x, y);
        }
    }

    @Override
    public void click(IBookGraphics nav)
    {
        ItemStack stack = getCurrentStack();
        if (stack == null)
            return;

        PageRef ref = nav.getBook().getStackLink(stack);
        if (ref != null)
            nav.navigateTo(ref);
    }

    @Nullable
    public ItemStack getCurrentStack()
    {
        if (stacks == null || stacks.length == 0)
            return null;
        long time = System.currentTimeMillis();
        return stacks[(int) ((time / CYCLE_TIME) % stacks.length)];
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }
}
