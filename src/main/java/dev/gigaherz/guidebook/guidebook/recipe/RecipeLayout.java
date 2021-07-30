package dev.gigaherz.guidebook.guidebook.recipe;

import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.elements.ElementImage;
import dev.gigaherz.guidebook.guidebook.elements.ElementStack;

public class RecipeLayout
{
    public int height = 0;
    public ElementStack[] recipeComponents;
    public ElementImage background;
    public VisualElement delegate;

    public RecipeLayout(int h, ElementStack[] rc, ElementImage background, VisualElement ird)
    {
        this.height = h;
        this.recipeComponents = rc;
        this.background = background;
        this.delegate = ird;
    }
}
