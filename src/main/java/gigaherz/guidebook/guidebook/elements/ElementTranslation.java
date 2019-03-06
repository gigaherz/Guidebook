package gigaherz.guidebook.guidebook.elements;

import net.minecraft.client.resources.I18n;

public class ElementTranslation extends ElementText
{
    public ElementTranslation(String text, boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        super(text, isFirstElement, isLastElement, style);
    }

    @Override
    protected String getActualString()
    {
        return I18n.format(text);
    }
}
