package gigaherz.guidebook.guidebook.drawing;

public class SizedSegment
{
    public Point position;
    public Size size;
    public String text;

    public SizedSegment(String text, Size size)
    {
        this.position= new Point();
        this.size=size;
        this.text = text;
    }
}
