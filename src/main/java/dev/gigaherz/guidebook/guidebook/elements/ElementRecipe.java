package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPanel;
import dev.gigaherz.guidebook.guidebook.recipe.IRecipeLayoutProvider;
import dev.gigaherz.guidebook.guidebook.recipe.RecipeLayout;
import dev.gigaherz.guidebook.guidebook.recipe.RecipeLayoutProviders;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.AttributeGetter;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author joazlazer
 * A section element that will display a recipe provided by the specified recipe type's RecipeProvider and will render hoverable stacks,
 * a background image, and additional components to display said recipe
 */
public class ElementRecipe extends Element
{
    private ResourceLocation recipeProviderKey = ResourceLocation.parse("crafting");
    private ResourceLocation recipeKey;
    private Element recipeOutput;
    private int recipeIndex = 0; // An index to use to specify a certain recipe when multiple ones exist for the target output item
    private int indent = 0;

    @Nonnull
    private RecipeLayout getRecipeLayout(@Nonnull Level world, IRecipeLayoutProvider recipeProvider, ElementStack output)
    {
        if (output == null || output.stacks == null || output.stacks.size() == 0)
            throw new IllegalArgumentException("Provided output stack is null or empty.");

        ItemStack targetOutput = output.stacks.get(0);

        return recipeProvider.getRecipeLayout(world, targetOutput, recipeIndex);
    }

    @Nonnull
    private RecipeLayout getRecipeLayout(@Nonnull Level world, IRecipeLayoutProvider recipeProvider, ResourceLocation recipeKey)
    {
        return recipeProvider.getRecipeLayout(world, recipeKey);
    }

    @Override
    public int reflow(List<VisualElement> list0, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        try
        {
            RecipeLayout recipeLayout;

            IRecipeLayoutProvider recipeProvider = RecipeLayoutProviders.getProvider(recipeProviderKey);

            if (recipeKey != null)
            {
                recipeLayout = getRecipeLayout(nav.getWorld(), recipeProvider, recipeKey);
            }
            else if (recipeOutput instanceof ElementStack)
            {
                recipeLayout = getRecipeLayout(nav.getWorld(), recipeProvider, (ElementStack) recipeOutput);
            }
            else
            {
                if (recipeOutput != null)
                    throw new IllegalArgumentException("Recipe output is not a stack element.");
                else
                    throw new IllegalArgumentException("Recipe name or output not provided, could not identify recipe.");
            }

            ElementImage background = recipeLayout.background;
            @org.jetbrains.annotations.Nullable
            VisualElement additionalRenderer = recipeLayout.delegate;
            ElementStack[] ingredients = recipeLayout.recipeComponents;
            int height = h != 0 ? h : recipeLayout.height;

            Point adjustedPosition = applyPosition(bounds.position, bounds.position);
            Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

            var list1 = new ArrayList<VisualElement>();

            background.reflow(list1, nav, adjustedBounds, pageBounds);

            for (ElementStack ingredient : ingredients)
            {
                ingredient.reflow(list1, nav, adjustedBounds, pageBounds);
            }

            if (additionalRenderer != null)
                list1.add(additionalRenderer);

            int x1 = Integer.MAX_VALUE;
            int y1 = Integer.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            int y2 = Integer.MIN_VALUE;
            for(var e : list1)
            {
                x1 = Math.min(x1, e.position.x());
                y1 = Math.min(y1, e.position.y());
                x2 = Math.max(x2, e.position.x()+e.size.width());
                y2 = Math.max(y2, e.position.y()+e.size.height());
            }

            var panel = new VisualPanel(new Size(x2-x1,y2-y1), 0, 0, 0);
            panel.position=new Point(x1,y1);
            panel.children.addAll(list1);
            list0.add(panel);

            if (position != POS_RELATIVE)
                return bounds.position.y();
            return adjustedPosition.y() + height;
        }
        catch (Exception e)
        {
            ElementSpan s = ElementSpan.of(e.getMessage(), TextStyle.ERROR);
            return s.reflow(list0, nav, bounds, pageBounds);
        }
    }

    @Override
    public void parse(ParsingContext context, AttributeGetter attributes)
    {
        String attr = attributes.getAttribute("type");
        if (attr != null)
        {
            String registryName = attr;
            // If no domain is specified, insert Guidebook's modid (mostly needed for default recipe providers)
            recipeProviderKey = ResourceLocation.parse(registryName);
        }

        attr = attributes.getAttribute("key");
        if (attr != null)
        {
            recipeKey = ResourceLocation.parse(attr);
        }

        attr = attributes.getAttribute("indent");
        if (attr != null)
        {
            Integer indentObj = Ints.tryParse(attr);
            if (indentObj != null) indent = indentObj;
        }

        attr = attributes.getAttribute("index");
        if (attr != null)
        {
            Integer recipeIndexObj = Ints.tryParse(attr);
            if (recipeIndexObj != null) recipeIndex = recipeIndexObj;
        }
    }

    /**
     * Parses each child node of the <recipe> tag in order to move two tree-layers down to find the <stack> tag
     *
     */
    @Override
    public void parseChildNodes(ParsingContext context, NodeList childNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        for (int i = 0; i < childNodes.getLength(); ++i)
        {
            Node childNode = childNodes.item(i);
            String nodeName = childNode.getNodeName();
            if (nodeName.equals("recipe.result"))
            {
                if (childNode.hasChildNodes())
                {
                    for (int j = 0; j < childNode.getChildNodes().getLength(); ++j)
                    {
                        Node stackNode = childNode.getChildNodes().item(j);
                        String stackNodeName = stackNode.getNodeName();
                        if (stackNodeName.equals("stack") || stackNodeName.equals("element"))
                        {
                            recipeOutput = BookDocument.parseParagraphElement(context, stackNode, stackNodeName, false, false, defaultStyle);
                        }
                    }
                }
                else
                    GuidebookMod.logger.warn("<recipe.result> sub-node is empty; Must contain exactly one <stack> node child");
            }
        }
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO
        return "<recipe .../>";
    }

    @Nullable
    @Override
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        ElementRecipe elementRecipe = super.copy(new ElementRecipe());
        if (recipeOutput != null)
        {
            elementRecipe.recipeOutput = recipeOutput.applyTemplate(context, sourceElements);
        }
        elementRecipe.recipeIndex = recipeIndex;
        if (recipeKey != null)
        {
            elementRecipe.recipeKey = recipeKey;
        }
        elementRecipe.indent = indent;
        return elementRecipe;
    }

    @Override
    public Element copy()
    {
        ElementRecipe elementRecipe = super.copy(new ElementRecipe());
        if (recipeOutput != null)
        {
            elementRecipe.recipeOutput = recipeOutput.copy();
        }
        elementRecipe.recipeIndex = recipeIndex;
        if (recipeKey != null)
        {
            elementRecipe.recipeKey = recipeKey;
        }
        elementRecipe.indent = indent;
        return elementRecipe;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    @Override
    public boolean supportsSpanLevel()
    {
        return false;
    }
}
