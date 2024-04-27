package dev.gigaherz.guidebook.guidebook.elements;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualStack;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import joptsimple.internal.Strings;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Locale;

public class ElementStack extends ElementInline
{
    public final NonNullList<ItemStack> stacks = NonNullList.create();

    public float scale = 1.0f;

    public LabelPosition labelPosition = LabelPosition.NONE;
    public int labelColor;

    public ElementStack(boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        super(isFirstElement, isLastElement);
        // default size
        w = 16;
        h = 16;
        labelColor = style.color;
    }

    private VisualStack getVisual(IBookGraphics nav)
    {
        int width = (int) (w * scale);
        int height = (int) (h * scale);
        var iconSize = new Size(width, height);

        var size = iconSize;
        if (labelPosition != LabelPosition.NONE)
        {
            var labelText = stacks.get(0).getHoverName();
            var labelSize = nav.measure(labelText);

            switch (labelPosition)
            {
                case LEFT, RIGHT -> size = new Size(iconSize.width() + labelSize.width(), Math.max(iconSize.height(), labelSize.height()));
                case ABOVE, BELOW -> size = new Size(Math.max(iconSize.width(), labelSize.width()), iconSize.height() + labelSize.height());
            }
        }

        return new VisualStack(stacks, size, position, baseline, verticalAlignment, scale, z, iconSize, labelPosition, labelColor);
    }

    @Override
    public List<VisualElement> measure(IBookGraphics nav, int width, int firstLineWidth)
    {
        return List.of(getVisual(nav));
    }

    @Override
    public int reflow(List<VisualElement> paragraph, IBookGraphics nav, Rect bounds, Rect page)
    {
        var element = getVisual(nav);
        element.position = applyPosition(bounds.position, bounds.position);
        paragraph.add(element);
        if (position != POS_RELATIVE)
            return bounds.position.y();
        return bounds.position.y() + element.size.height();
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        int stackSize = 1;
        CompoundTag tag = new CompoundTag();

        super.parse(context, attributes);

        scale = attributes.getAttribute("scale", scale);

        String attr;
        //String attr = attributes.getAttribute("tag");
        //if (attr != null)
        //{
        //    try
        //    {
        //        tag = TagParser.parseTag(attr);
        //    }
        //    catch (CommandSyntaxException e)
        //    {
        //        GuidebookMod.logger.warn("Invalid tag format: " + e.getMessage());
        //    }
        //}

        attr = attributes.getAttribute("count");
        if (attr != null)
        {
            stackSize = Integer.parseInt(attr);
        }

        String name = null;

        attr = attributes.getAttribute("name");
        if (attr != null)
        {
            name = attr;
        }

        attr = attributes.getAttribute("item");
        if (attr != null)
        {
            String itemName = attr;

            if (itemName.startsWith("#"))
            {
                var count = stackSize;
                var hoverName = name;
                //var nbt = tag;
                var mcTag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, new ResourceLocation(itemName.substring(1))));
                mcTag.ifPresent(tag1 -> tag1.stream().forEachOrdered(item -> {
                    ItemStack stack = new ItemStack(item, count);
                    //stack.setTag(nbt);
                    stacks.add(stack);
                    if (!Strings.isNullOrEmpty(hoverName))
                    {
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(hoverName));
                    }
                }));
            }
            else
            {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemName));

                if (item != null)
                {
                    ItemStack stack = new ItemStack(item, stackSize);
                    //stack.setTag(tag);
                    stacks.add(stack);
                    if (!Strings.isNullOrEmpty(name))
                    {
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                    }
                }
            }
        }

        attr = attributes.getAttribute("labelPosition");
        if (attr != null)
        {
            if (stacks.size() != 1)
                throw new RuntimeException("stack labels cannot be used with multi-stack elements!");

            labelPosition = Enum.valueOf(LabelPosition.class, attr.toUpperCase(Locale.ROOT));
        }

        // TODO: Tags
        /*
        //get stacks from ore dictionary
        attr = attributes.getNamedItem("ore");
        if (attr != null)
        {
            String oreName = attr;
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
        ElementStack newStack = super.copy(new ElementStack(isFirstElement, isLastElement, TextStyle.DEFAULT));
        newStack.scale = scale;
        newStack.labelPosition = labelPosition;
        newStack.labelColor = labelColor;
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
