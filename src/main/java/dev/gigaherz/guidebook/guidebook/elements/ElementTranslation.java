package dev.gigaherz.guidebook.guidebook.elements;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class ElementTranslation extends ElementText
{
    public ElementTranslation(String text, boolean isFirstElement, boolean isLastElement, TextStyle style)
    {
        super(text, isFirstElement, isLastElement, style);
    }

    @Override
    protected FormattedText getActualString()
    {
        return Component.translatable(text);
    }
}
