package dev.gigaherz.guidebook.guidebook.util;

public record Point2I(int x, int y) implements Cloneable
{
    public static final Point2I ZERO = new Point2I();

    public Point2I()
    {
        this(0, 0);
    }

    public Point2I(Point2I other)
    {
        this(other.x, other.y);
    }

    @Override
    public Point2I clone() throws CloneNotSupportedException
    {
        return (Point2I) super.clone();
    }
}
