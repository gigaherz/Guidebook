package gigaherz.guidebook.guidebook.util;

public class Rect
{
    public Point position;
    public Size size;

    public Rect()
    {
        this.position = new Point();
        this.size = new Size();
    }

    public Rect(Point point, Size size)
    {
        this.position = point;
        this.size = size;
    }

    public Rect(int x, int y, int width, int height)
    {
        this.position = new Point(x, y);
        this.size = new Size(width, height);
    }
}