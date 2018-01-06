package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;

public class VisualText extends VisualElement
{
    public String text;
    public int color;

    public VisualText(String text, Size size)
    {
        super(size);
        this.text = text;
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.addString(position.x, position.y, text, color);
    }
}
