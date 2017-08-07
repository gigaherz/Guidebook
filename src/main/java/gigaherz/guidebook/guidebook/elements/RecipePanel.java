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
import org.w3c.dom.NodeList;

import java.util.Set;

public class RecipePanel implements IPageElement {
    public Stack[] recipeComponents;
    public Image background;
    public int recipeIndex = 0; // An index to use to specify a certain recipe when multiple ones exist for the target output item
    public IRenderDelegate additionalRenderer;
    private RecipeProvider recipeProvider;
    public int height = 10;

    @Override
    public int apply(IBookGraphics nav, int left, int top) {
        if(recipeComponents != null) {
            for(Stack stack : recipeComponents) {
                stack.apply(nav, left, top);
            }
            background.apply(nav, left, top);
            additionalRenderer.render(nav, left, top);
        } // If the recipe was never found, return a height of 10 by default and don't render
        return height;
    }

    @Override
    public void parse(NamedNodeMap attributes) {
        // If a RecipeProvider was not loaded correctly, fallback to the default
        recipeProvider = RecipeProvider.registry.getValue(new ResourceLocation("shaped"));

        Node attr = attributes.getNamedItem("type");
        if(attr != null) {
            ResourceLocation recipeProviderKey = new ResourceLocation(attr.getTextContent());
            if(RecipeProvider.registry.containsKey(recipeProviderKey)) {
                recipeProvider = RecipeProvider.registry.getValue(recipeProviderKey);
            } else GuidebookMod.logger.warn(String.format("<recipe> type specifies a RecipeProvider with key '%s', which hasn't been registered.", recipeProviderKey.toString()));
        }

        attr = attributes.getNamedItem("index");
        if(attr != null) {
            recipeIndex = Ints.tryParse(attr.getTextContent());
        }
    }

    public void parseChildNodes(NodeList childNodes) {
        Node recipeResult = childNodes.item(0);
        if(recipeResult.getNodeName().equals("recipe.result")) {
            if(recipeResult.hasChildNodes()) {
                Node stackNode = recipeResult.getFirstChild();
                if(stackNode.hasAttributes()) {
                    Stack targetOutput = new Stack();
                    targetOutput.parse(stackNode.getAttributes());
                    ItemStack targetOutputItem = targetOutput.stacks[0];
                    retrieveRecipe(targetOutputItem);
                } else GuidebookMod.logger.warn("<recipe.result>'s <stack> sub-node has no attributes. Recipe not loaded");
            } else GuidebookMod.logger.warn("<recipe.result> sub-node is empty; Must contain exactly one <stack> node child");
        } else GuidebookMod.logger.warn("<recipe> sub-node is an invalid type");
    }

    private void retrieveRecipe(ItemStack targetOutput) {
        if(recipeProvider.hasRecipe(targetOutput)) {
            height = recipeProvider.provideRecipeComponents(targetOutput, recipeIndex, recipeComponents, background, additionalRenderer);
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
        return recipePanel;
    }

    @Override
    public void findTextures(Set<ResourceLocation> textures) {
        if (background != null) {
            textures.add(background.textureLocation);
        }
    }
}
