package dev.gigaherz.guidebook.guidebook.util;

public record Point2D(double x, double y) implements Cloneable
{
    public static final Point2D ZERO = new Point2D();

    public Point2D()
    {
        this(0, 0);
    }

    public Point2D(Point2D other)
    {
        this(other.x, other.y);
    }

    @Override
    public Point2D clone() throws CloneNotSupportedException
    {
        return (Point2D) super.clone();
    }
}
