package dev.gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class VisualChapter
{
    public final List<VisualPage> pages = Lists.newArrayList();
    public final Map<String, Integer> pagesByName = Maps.newHashMap();
    public int startPair;
    public int totalPairs;
}
