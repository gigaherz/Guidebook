package dev.gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.SectionRef;

import java.util.List;

public class VisualPage
{
    public final SectionRef ref;
    public final List<VisualElement> children = Lists.newArrayList();

    public VisualPage(SectionRef ref)
    {
        this.ref = ref;
    }

    public void updateDebugIndices()
    {
        for(int i=0;i<children.size();i++)
        {
            children.get(i).updateDebugIndices(i,0);
        }
    }
}
