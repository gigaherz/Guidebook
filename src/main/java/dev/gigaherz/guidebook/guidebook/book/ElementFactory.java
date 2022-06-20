package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.guidebook.elements.Element;

@FunctionalInterface
public interface ElementFactory
{
    Element newInstance();
}
