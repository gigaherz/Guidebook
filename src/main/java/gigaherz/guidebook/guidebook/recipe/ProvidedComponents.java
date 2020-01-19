package gigaherz.guidebook.guidebook.recipe;

import gigaherz.guidebook.guidebook.drawing.VisualElement;
import gigaherz.guidebook.guidebook.elements.ElementImage;
import gigaherz.guidebook.guidebook.elements.ElementStack;

/**
 * A helper packaging class that allows RecipeProvider.provideRecipeComponents(...) to return multiple GUI components and values
 */
public class ProvidedComponents
{
    public int height = 0;
    public ElementStack[] recipeComponents;
    public ElementImage background;
    public VisualElement delegate;

    public ProvidedComponents(int h, ElementStack[] rc, ElementImage background, VisualElement ird)
    {
        this.height = h;
        this.recipeComponents = rc;
        this.background = background;
        this.delegate = ird;
    }
}
