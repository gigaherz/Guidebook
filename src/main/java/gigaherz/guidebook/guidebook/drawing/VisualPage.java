package gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import gigaherz.guidebook.guidebook.SectionRef;

import java.util.List;

public class VisualPage
{
    public final SectionRef ref;
    public final List<VisualElement> children = Lists.newArrayList();

    public VisualPage(SectionRef ref)
    {
        this.ref = ref;
    }
}
