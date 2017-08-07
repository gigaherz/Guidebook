package gigaherz.guidebook.guidebook.elements;

import com.google.common.primitives.Ints;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.IRenderDelegate;
import gigaherz.guidebook.guidebook.recipe.RecipeProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Arrays;

public class RecipePanel extends Space {
    public Stack[] recipeComponents;
    public Image background;
    public int recipeIndex = 0; // An index to use to specify a certain recipe when multiple ones exist for the target output item
    public IRenderDelegate additionalRenderer;
    public int indent = 0;
    public int height = 10;

    // Only used to communicate across parsing methods at resource load; not needed to be duplicated
    private RecipeProvider recipeProvider;

    @Override
    public int apply(IBookGraphics nav, int left, int top) {
        if(recipeComponents != null) {
            left += indent;
            super.apply(nav, left, top);
        } // If the recipe was never found, return a height of 10 by default and don't render
        return height;
    }

    @Override
    public void parse(NamedNodeMap attributes) {
        // If a RecipeProvider was not loaded correctly or was not specified, fallback to the default
        recipeProvider = RecipeProvider.registry.getValue(new ResourceLocation(GuidebookMod.MODID,"shaped"));

        Node attr = attributes.getNamedItem("type");
        if(attr != null) {
            String registryName = attr.getTextContent();
            // If no domain is specified, insert Guidebook's modid (mostly needed for default recipe providers)
            ResourceLocation recipeProviderKey = new ResourceLocation((registryName.indexOf(':') == -1 ? GuidebookMod.MODID + ":" : "") + registryName);
            if(RecipeProvider.registry.containsKey(recipeProviderKey)) {
                recipeProvider = RecipeProvider.registry.getValue(recipeProviderKey);
            } else GuidebookMod.logger.warn(String.format("<recipe> type specifies a RecipeProvider with key '%s', which hasn't been registered.", recipeProviderKey.toString()));
        }

        attr = attributes.getNamedItem("index");
        if(attr != null) {
            recipeIndex = Ints.tryParse(attr.getTextContent());
        }

        attr = attributes.getNamedItem("indent");
        if(attr != null) {
            indent = Ints.tryParse(attr.getTextContent());
        }
    }

    public void parseChildNodes(Node element) {
        for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
            Node childNode = element.getChildNodes().item(i);
            String nodeName = childNode.getNodeName();
            if(nodeName.equals("recipe.result")) {
                if(childNode.hasChildNodes()) {
                    for(int j = 0; j < childNode.getChildNodes().getLength(); ++j) {
                        Node stackNode = childNode.getChildNodes().item(j);
                        if(stackNode.getNodeName().equals("stack")) {
                            if(stackNode.hasAttributes()) {
                                Stack targetOutput = new Stack();
                                targetOutput.parse(stackNode.getAttributes());
                                ItemStack targetOutputItem = targetOutput.stacks[0];
                                retrieveRecipe(targetOutputItem);
                            } else GuidebookMod.logger.warn("<recipe.result>'s <stack> sub-node has no attributes. Recipe not loaded");
                        }
                    }
                } else GuidebookMod.logger.warn("<recipe.result> sub-node is empty; Must contain exactly one <stack> node child");
            }
        }
    }

    private void retrieveRecipe(ItemStack targetOutput) {
        if(recipeProvider.hasRecipe(targetOutput)) {
            RecipeProvider.ProvidedComponents components = recipeProvider.provideRecipeComponents(targetOutput, recipeIndex);
            this.height = components.height;
            this.background = components.background;
            this.additionalRenderer = components.delegate;
            this.recipeComponents = components.recipeComponents;

            // Add to child element list in order to support hover
            this.innerElements.add(background);
            this.innerElements.addAll(Arrays.asList(recipeComponents));
        } else GuidebookMod.logger.warn(String.format("<recipe>'s specified output item '%s' does not have a recipe for type '%s'", targetOutput.toString(), recipeProvider.getRegistryName().toString()));
    }

    @Override
    public IPageElement copy() {
        RecipePanel recipePanel = new RecipePanel();
        recipePanel.recipeComponents = new Stack[recipeComponents.length];
        for(int i = 0; i < recipeComponents.length; ++i) {
            recipePanel.recipeComponents[i] = (Stack)recipeComponents[i].copy();
        }
        recipePanel.background = (Image)background.copy();
        recipePanel.recipeIndex = recipeIndex;
        recipePanel.additionalRenderer = additionalRenderer;
        recipePanel.height = height;
        recipePanel.indent = indent;
        return recipePanel;
    }
}
