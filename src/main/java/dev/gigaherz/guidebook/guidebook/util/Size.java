package dev.gigaherz.guidebook.guidebook.util;

public record Size(int width, int height) implements Cloneable
{
    public static final Size ZERO = new Size();

    public Size()
    {
        this(0, 0);
    }

    public Size(Size other)
    {
        this(other.width, other.height);
    }

    @Override
    public Size clone() throws CloneNotSupportedException
    {
        return (Size) super.clone();
    }
}
