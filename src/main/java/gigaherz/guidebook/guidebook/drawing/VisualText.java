package gigaherz.guidebook.guidebook.drawing;

import gigaherz.guidebook.guidebook.IBookGraphics;

public class VisualText extends VisualElement
{
    public String text;
    public int color;
    public float scale;

    public VisualText(String text, Size size, int positionMode, float baseline, int verticalAlign, float scale)
    {
        super(size, positionMode, baseline, verticalAlign);
        this.text = text;
        this.scale = scale;
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.addString(position.x, position.y, text, color, scale);
    }

    @Override
    public String getText()
    {
        return text;
    }
}
