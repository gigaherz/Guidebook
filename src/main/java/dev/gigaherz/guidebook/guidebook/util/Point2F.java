package dev.gigaherz.guidebook.guidebook.util;

public record Point2F(float x, float y)
{
    public static final Point2F ZERO = new Point2F();

    public Point2F()
    {
        this(0, 0);
    }

    public Point2F(Point2F other)
    {
        this(other.x, other.y);
    }

    @Override
    public Point2F clone() throws CloneNotSupportedException
    {
        return (Point2F) super.clone();
    }
}
