package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.guidebook.elements.ElementInline;

@FunctionalInterface
public interface InlineElementFactory
{
    ElementInline newInstance(boolean isFirstElement, boolean isLastElement);
}
