package dev.gigaherz.guidebook.guidebook.conditions;

import org.w3c.dom.Node;

import java.util.function.Predicate;

@FunctionalInterface
public interface IDisplayConditionFactory
{
    Predicate<ConditionContext> parse(Node xmlNode);
}
