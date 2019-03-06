package gigaherz.guidebook.guidebook.elements;

import gigaherz.guidebook.guidebook.SectionRef;

public class LinkContext
{
    public String textTarget;
    public String textAction;
    public SectionRef target;
    public int colorHover;
    public boolean isHovering;

    public LinkContext copy()
    {
        LinkContext link = new LinkContext();
        if (target != null)
            link.target = target.copy();
        link.colorHover = colorHover;
        link.textTarget = textTarget;
        link.textAction = textAction;
        return null;
    }
}
