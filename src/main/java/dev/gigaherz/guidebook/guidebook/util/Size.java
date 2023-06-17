package dev.gigaherz.guidebook.guidebook.util;

public record Size(int width, int height)
{
    public Size()
    {
        this(0,0);
    }
}
