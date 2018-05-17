package gigaherz.guidebook.guidebook.conditions;

import java.util.function.Predicate;

@FunctionalInterface
public interface IDisplayCondition extends Predicate<ConditionContext>
{
}
