package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.recipe.IRecipeLayoutProvider;
import gigaherz.guidebook.guidebook.recipe.RecipeLayout;
import gigaherz.guidebook.guidebook.recipe.RecipeLayoutProviders;
import gigaherz.guidebook.guidebook.util.Point;
import gigaherz.guidebook.guidebook.util.Rect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author joazlazer
 * A section element that will display a recipe provided by the specified recipe type's RecipeProvider and will render hoverable stacks,
 * a background image, and additional components to display said recipe
 */
public class ElementRecipe extends Element
{
    private ResourceLocation recipeProviderKey = new ResourceLocation("crafting");
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
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        try {
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
            VisualElement additionalRenderer = recipeLayout.delegate;
            ElementStack[] ingredients = recipeLayout.recipeComponents;
            int height = h != 0 ? h : recipeLayout.height;

            Point adjustedPosition = applyPosition(bounds.position, bounds.position);
            Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

            for (ElementStack ingredient : ingredients)
            {
                ingredient.reflow(list, nav, adjustedBounds, pageBounds);
            }

            background.reflow(list, nav, adjustedBounds, pageBounds);
            if (additionalRenderer != null)
                list.add(additionalRenderer);
            if (position != POS_RELATIVE)
                return bounds.position.y;
            return adjustedPosition.y + height;
        }
        catch(Exception e)
        {
            ElementSpan s = ElementSpan.of(e.getMessage(), TextStyle.ERROR);
            return s.reflow(list, nav, bounds, pageBounds);
        }
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("type");
        if (attr != null)
        {
            String registryName = attr.getTextContent();
            // If no domain is specified, insert Guidebook's modid (mostly needed for default recipe providers)
            recipeProviderKey = new ResourceLocation(registryName);
        }

        attr = attributes.getNamedItem("key");
        if (attr != null)
        {
            recipeKey = new ResourceLocation(attr.getTextContent());
        }

        attr = attributes.getNamedItem("indent");
        if (attr != null)
        {
            Integer indentObj = Ints.tryParse(attr.getTextContent());
            if (indentObj != null) indent = indentObj;
        }

        attr = attributes.getNamedItem("index");
        if (attr != null)
        {
            Integer recipeIndexObj = Ints.tryParse(attr.getTextContent());
            if (recipeIndexObj != null) recipeIndex = recipeIndexObj;
        }
    }

    /**
     * Parses each child node of the <recipe> tag in order to move two tree-layers down to find the <stack> tag
     *
     * @param element The base <recipe> tag
     */
    @Override
    public void parseChildNodes(IConditionSource book, Node element)
    {
        for (int i = 0; i < element.getChildNodes().getLength(); ++i)
        {
            Node childNode = element.getChildNodes().item(i);
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
                            recipeOutput = BookDocument.parseParagraphElement(book, stackNode, stackNodeName, false, false, TextStyle.DEFAULT);
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
    public Element applyTemplate(IConditionSource book, List<Element> sourceElements)
    {
        ElementRecipe elementRecipe = super.copy(new ElementRecipe());
        if (recipeOutput != null)
        {
            elementRecipe.recipeOutput = recipeOutput.applyTemplate(book, sourceElements);
        }
        elementRecipe.recipeIndex = recipeIndex;
        if (recipeKey != null)
        {
            elementRecipe.recipeKey = new ResourceLocation(recipeKey.toString());
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
            elementRecipe.recipeKey = new ResourceLocation(recipeKey.toString());
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
