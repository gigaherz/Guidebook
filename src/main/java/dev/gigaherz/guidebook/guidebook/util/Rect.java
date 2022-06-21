package dev.gigaherz.guidebook.guidebook.util;

public class Rect implements Cloneable
{
    public Point2I position;
    public Size size;

    public Rect()
    {
        this.position = new Point2I();
        this.size = new Size();
    }

    public Rect(Point2I point, Size size)
    {
        this.position = point;
        this.size = size;
    }

    public Rect(int x, int y, int width, int height)
    {
        this.position = new Point2I(x, y);
        this.size = new Size(width, height);
    }

    public boolean contains(double x, double y)
    {
        return x >= position.x() && x <= (position.x() + size.width()) && y >= position.y() && y <= (position.y() + size.height());
    }

    public boolean contains(Point2I point)
    {
        return contains(point.x(), point.y());
    }

    public boolean contains(Point2D point)
    {
        return contains(point.x(), point.y());
    }

    public boolean contains(Point2F point)
    {
        return contains(point.x(), point.y());
    }

    @Override
    public Rect clone() throws CloneNotSupportedException
    {
        return (Rect) super.clone();
    }
}
