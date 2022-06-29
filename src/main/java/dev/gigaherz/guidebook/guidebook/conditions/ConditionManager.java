package dev.gigaherz.guidebook.guidebook.conditions;

import com.google.common.collect.Maps;
import dev.gigaherz.guidebook.guidebook.book.BookDocument;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.Predicate;

public class ConditionManager
{
    public static final Map<String, IDisplayConditionFactory> REGISTRY = Maps.newHashMap();

    public static synchronized void register(String id, IDisplayConditionFactory factory)
    {
        REGISTRY.put(id, factory);
    }

    public static Predicate<ConditionContext> parseCondition(Node node)
    {
        IDisplayConditionFactory factory = REGISTRY.get(node.getNodeName());
        if (factory == null)
            return null;
        return factory.parse(node);
    }
}
