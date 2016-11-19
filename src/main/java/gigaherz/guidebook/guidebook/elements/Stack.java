package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Stack implements IHoverPageElement
{
    public ItemStack stack;
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

        nav.drawItemStack(left, top, z, stack, 0xFFFFFFFF, scale);

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
                stack = new ItemStack(item, stackSize, meta);
                stack.setTagCompound(tag);
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
        stack.stack = this.stack.copy();
        stack.x = x;
        stack.y = y;
        return stack;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        nav.drawTooltip(stack, x, y);
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }
}
