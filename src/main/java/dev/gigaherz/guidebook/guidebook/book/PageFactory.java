package dev.gigaherz.guidebook.guidebook.book;

@FunctionalInterface
public interface PageFactory
{
    PageData newInstance(SectionRef ref);
}
