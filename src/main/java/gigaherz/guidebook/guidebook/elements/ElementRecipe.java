package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.drawing.Point;
import gigaherz.guidebook.guidebook.drawing.Rect;
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
 * A page element that will display a recipe provided by the specified recipe type's RecipeProvider and will render hoverable stacks,
 * a background image, and additional components to display said recipe
 */
public class ElementRecipe extends Element
{
    private ElementStack[] recipeComponents;
    private ElementImage background;
    private int recipeIndex = 0; // An index to use to specify a certain recipe when multiple ones exist for the target output item
    private VisualElement additionalRenderer;
    private int indent = 0;
    private int height = 10;

    // Only used to communicate across parsing methods at resource load; not needed to be duplicated
    private RecipeProvider recipeProvider;

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

        for (int i = 0; i < recipeComponents.length; ++i)
        {
            recipeComponents[i].reflow(list, nav, adjustedBounds, pageBounds);
        }
        background.reflow(list, nav, adjustedBounds, pageBounds);
        if (additionalRenderer != null)
            list.add(additionalRenderer);
        if (position != 0)
            return bounds.position.y;
        return adjustedPosition.y+height;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        // If a RecipeProvider was not loaded correctly or was not specified, fallback to the default
        recipeProvider = RecipeProvider.registry.getValue(new ResourceLocation(GuidebookMod.MODID, "shaped"));

        Node attr = attributes.getNamedItem("type");
        if (attr != null)
        {
            String registryName = attr.getTextContent();
            // If no domain is specified, insert Guidebook's modid (mostly needed for default recipe providers)
            ResourceLocation recipeProviderKey = new ResourceLocation((registryName.indexOf(':') == -1 ? GuidebookMod.MODID + ":" : "") + registryName);
            if (RecipeProvider.registry.containsKey(recipeProviderKey))
            {
                recipeProvider = RecipeProvider.registry.getValue(recipeProviderKey);
            }
            else
                GuidebookMod.logger.warn(String.format("<recipe> type specifies a RecipeProvider with key '%s', which hasn't been registered.", recipeProviderKey));
        }

        attr = attributes.getNamedItem("key");
        if (attr != null)
        {
            ResourceLocation recipeKey = new ResourceLocation(attr.getTextContent());
            retrieveRecipe(recipeKey);
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
            if (recipeComponents != null)
            {
                GuidebookMod.logger.warn("Recipe has index attribute '%s' specified but was already loaded via a key attribute. Ignoring index attribute.");
            }
            else
            {
                Integer recipeIndexObj = Ints.tryParse(attr.getTextContent());
                if (recipeIndexObj != null) recipeIndex = recipeIndexObj;
            }
        }
    }

    /**
     * Parses each child node of the <recipe> tag in order to move two tree-layers down to find the <stack> tag
     *
     * @param element The base <recipe> tag
     */
    public void parseChildNodes(Node element)
    {
        if (recipeComponents != null)
        {
            GuidebookMod.logger.warn("Recipe has child nodes but was already loaded via a key attribute. Ignoring child nodes.");
            return;
        }
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
                        if (stackNode.getNodeName().equals("stack"))
                        {
                            if (stackNode.hasAttributes())
                            {
                                ElementStack targetOutput = new ElementStack();
                                targetOutput.parse(stackNode.getAttributes());
                                ItemStack targetOutputItem = targetOutput.stacks[0];
                                retrieveRecipe(targetOutputItem);
                            }
                            else
                                GuidebookMod.logger.warn("<recipe.result>'s <stack> sub-node has no attributes. Recipe not loaded");
                        }
                    }
                }
                else
                    GuidebookMod.logger.warn("<recipe.result> sub-node is empty; Must contain exactly one <stack> node child");
            }
        }
    }

    private void retrieveRecipe(ItemStack targetOutput)
    {
        if (!recipeProvider.hasRecipe(targetOutput))
            GuidebookMod.logger.warn(String.format("<recipe>'s specified output item '%s' does not have a recipe for type '%s'", targetOutput, recipeProvider.getRegistryName()));

        retrieveRecipe(recipeProvider.provideRecipeComponents(targetOutput, recipeIndex));
    }

    private void retrieveRecipe(ResourceLocation recipeKey)
    {
        if (!recipeProvider.hasRecipe(recipeKey))
            GuidebookMod.logger.warn(String.format("<recipe>'s specified key '%s' does not exist for type '%s'", recipeKey, recipeProvider.getRegistryName()));

        retrieveRecipe(recipeProvider.provideRecipeComponents(recipeKey));
    }

    private void retrieveRecipe(@Nullable RecipeProvider.ProvidedComponents components)
    {
        assert components != null; // Sanity assertion

        this.height = components.height;
        this.background = components.background;
        this.additionalRenderer = components.delegate;
        this.recipeComponents = components.recipeComponents;
    }

    @Override
    public Element copy()
    {
        ElementRecipe elementRecipe = new ElementRecipe();
        elementRecipe.recipeComponents = new ElementStack[recipeComponents.length];
        for (int i = 0; i < recipeComponents.length; ++i)
        {
            elementRecipe.recipeComponents[i] = (ElementStack) recipeComponents[i].copy();
        }
        elementRecipe.background = (ElementImage) background.copy();
        elementRecipe.recipeIndex = recipeIndex;
        elementRecipe.additionalRenderer = additionalRenderer;
        elementRecipe.height = height;
        elementRecipe.indent = indent;
        return elementRecipe;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }
}
