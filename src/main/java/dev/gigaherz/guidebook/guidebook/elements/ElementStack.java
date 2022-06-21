package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.book.IParseable;
import dev.gigaherz.guidebook.guidebook.book.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualStack;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;

public class ElementStack extends ElementInline
{
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
            return bounds.position.y();
        return bounds.position.y() + element.size.height();
    }

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        int stackSize = 1;
        CompoundTag nbt = new CompoundTag();

        super.parse(context, attributes);

        scale = IParseable.getAttribute(attributes, "scale", scale);

        Node attr;
        attr = attributes.getNamedItem("count");
        if (attr != null)
        {
            stackSize = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("nbt");
        if (attr != null)
        {
            try
            {
                nbt = TagParser.parseTag(attr.getTextContent());
            }
            catch (CommandSyntaxException e)
            {
                GuidebookMod.logger.warn("Invalid nbt format: " + e.getMessage());
            }
        }

        attr = attributes.getNamedItem("name");
        if (attr != null)
        {
            try
            {
                nbt = TagParser.parseTag(attr.getTextContent());// TODO this does not make sense
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
                stack.setTag(nbt);
                stacks.add(stack);
            }
        }

        attr = attributes.getNamedItem("tag");
        if (attr != null)
        {
            TagKey<Item> tagKey = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(attr.getTextContent()));
            ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);

            if (!tag.isEmpty())
            {
                for (Item item : tag)
                {
                    ItemStack stack = new ItemStack(item, stackSize);
                    stacks.add(stack);
                }
            }
        }
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
        for (ItemStack stack : stacks)
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
