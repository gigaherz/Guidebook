package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.util.Rect;
import gigaherz.guidebook.guidebook.util.Size;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.drawing.VisualStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;

public class ElementStack extends ElementInline
{
    public static final String WILDCARD = "*";

    public final NonNullList<ItemStack> stacks = NonNullList.create();

    public float scale = 1.0f;

    public ElementStack(boolean isFirstElement, boolean isLastElement)
    {
        super(isFirstElement, isLastElement);
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
        return new VisualStack(stacks, getVisualSize(), position, baseline, verticalAlignment, scale, z);
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
        int stackSize = 1;
        CompoundNBT tag = new CompoundNBT();

        super.parse(book, attributes);

        scale = getAttribute(attributes, "scale", scale);

        Node attr = attributes.getNamedItem("count");
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
            catch (CommandSyntaxException e)
            {
                GuidebookMod.logger.warn("Invalid tag format: " + e.getMessage());
            }
        }

        attr = attributes.getNamedItem("item");
        if (attr != null)
        {
            String itemName = attr.getTextContent();

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));

            if (item != null)
            {
                ItemStack stack = new ItemStack(item, stackSize);
                stack.setTag(tag);
                stacks.add(stack);
            }
        }

        // TODO: Tags
        /*
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
        */
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO
        return "<stack .../>";
    }

    @Override
    public ElementInline copy()
    {
        ElementStack newStack = super.copy(new ElementStack(isFirstElement, isLastElement));
        newStack.scale = scale;
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
