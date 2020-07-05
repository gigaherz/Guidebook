package gigaherz.guidebook.guidebook.elements;

import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.TranslationTextComponent;

public class ElementTranslation extends ElementText
{
    public ElementTranslation(String text, boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        super(text, isFirstElement, isLastElement, style);
    }

    @Override
    protected ITextProperties getActualString()
    {
        return new TranslationTextComponent(text);
    }
}
