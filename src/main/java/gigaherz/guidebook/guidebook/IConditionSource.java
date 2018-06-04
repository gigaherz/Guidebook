package gigaherz.guidebook.guidebook;

import gigaherz.guidebook.guidebook.conditions.ConditionContext;

import java.util.function.Predicate;

public interface IConditionSource
{
    Predicate<ConditionContext> getCondition(String name);
}
