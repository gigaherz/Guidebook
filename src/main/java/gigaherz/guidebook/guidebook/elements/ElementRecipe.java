package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IConditionSource;
import gigaherz.guidebook.guidebook.util.Point;
import gigaherz.guidebook.guidebook.util.Rect;
import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.recipe.RecipeProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author joazlazer
 * A section element that will display a recipe provided by the specified recipe type's RecipeProvider and will render hoverable stacks,
 * a background image, and additional components to display said recipe
 */
public class ElementRecipe extends Element
{
    private ResourceLocation recipeProviderKey = new ResourceLocation(GuidebookMod.MODID, "shaped");
    private ResourceLocation recipeKey;
    private Element recipeOutput;
    private int recipeIndex = 0; // An index to use to specify a certain recipe when multiple ones exist for the target output item
    private int indent = 0;

    private RecipeProvider.ProvidedComponents retrieveRecipe(RecipeProvider recipeProvider, ElementStack output)
    {
        if (output == null || output.stacks == null || output.stacks.size() == 0)
            return null;

        ItemStack targetOutput = output.stacks.get(0);

        return recipeProvider.provideRecipeComponents(targetOutput, recipeIndex);
    }

    private RecipeProvider.ProvidedComponents retrieveRecipe(RecipeProvider recipeProvider, ResourceLocation recipeKey)
    {
        return recipeProvider.provideRecipeComponents(recipeKey);
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        RecipeProvider recipeProvider = RecipeProvider.registry.get(recipeProviderKey);
        RecipeProvider.ProvidedComponents components = null;

        if (recipeProvider != null && (recipeKey != null || recipeOutput instanceof ElementStack))
        {
            components = recipeKey != null
                    ? retrieveRecipe(recipeProvider, recipeKey)
                    : retrieveRecipe(recipeProvider, (ElementStack) recipeOutput);
        }

        if (components == null)
        {
            ElementSpan s;
            if (recipeProvider == null)
                s = ElementSpan.of(String.format("Recipe type specifies a RecipeProvider with key '%s', which hasn't been registered.", recipeProviderKey), TextStyle.ERROR);
            else if (recipeKey != null)
                s = ElementSpan.of(String.format("Recipe not found for registry name: %s", recipeKey), TextStyle.ERROR);
            else if (recipeOutput != null)
            {
                if (recipeOutput instanceof ElementStack)
                    s = ElementSpan.of("Recipe not found for provided output item.", TextStyle.ERROR);
                else
                    s = ElementSpan.of("Recipe output is not a stack element.", TextStyle.ERROR);
            }
            else
                s = ElementSpan.of("Recipe name or output not provided, could not identify recipe.", TextStyle.ERROR);
            return s.reflow(list, nav, bounds, pageBounds);
        }

        ElementImage background = components.background;
        VisualElement additionalRenderer = components.delegate;
        ElementStack[] ingredients = components.recipeComponents;
        int height = h != 0 ? h : components.height;

        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

        for (int i = 0; i < ingredients.length; ++i)
        {
            ingredients[i].reflow(list, nav, adjustedBounds, pageBounds);
        }

        background.reflow(list, nav, adjustedBounds, pageBounds);
        if (additionalRenderer != null)
            list.add(additionalRenderer);
        if (position != POS_RELATIVE)
            return bounds.position.y;
        return adjustedPosition.y + height;
    }

    @Override
    public void parse(IConditionSource book, NamedNodeMap attributes)
    {
        Node attr = attributes.getNamedItem("type");
        if (attr != null)
        {
            String registryName = attr.getTextContent();
            // If no domain is specified, insert Guidebook's modid (mostly needed for default recipe providers)
            recipeProviderKey = new ResourceLocation((registryName.indexOf(':') == -1 ? GuidebookMod.MODID + ":" : "") + registryName);
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
