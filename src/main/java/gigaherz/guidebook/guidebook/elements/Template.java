package gigaherz.guidebook.guidebook.elements;

import java.util.List;

public class Template extends Space
{
    public Template()
    {
        super();
    }

    public Template(List<IPageElement> innerElements)
    {
        super(innerElements);
    }

    @Override
    public IPageElement copy()
    {
        return new Template(innerElements);
    }
}
