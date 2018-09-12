package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;

import java.util.List;
import java.util.function.Predicate;

public abstract class CompositeCondition implements Predicate<ConditionContext>
{
    public final List<Predicate<ConditionContext>> children;

    protected CompositeCondition(List<Predicate<ConditionContext>> children)
    {
        this.children = children;
    }

    public static void register()
    {
        IDisplayConditionFactory any = (doc, node) -> new Any(BookDocument.parseChildConditions(doc, node));
        IDisplayConditionFactory all = (doc, node) -> new All(BookDocument.parseChildConditions(doc, node));
        IDisplayConditionFactory not = (doc, node) -> new Not(BookDocument.parseChildConditions(doc, node));
        ConditionManager.register("any", any);
        ConditionManager.register("or", any);
        ConditionManager.register("all", all);
        ConditionManager.register("and", all);
        ConditionManager.register("not", not);
    }

    public static class Any extends CompositeCondition
    {
        public Any(List<Predicate<ConditionContext>> children)
        {
            super(children);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return children.stream().anyMatch(t -> t.test(conditionContext));
        }
    }

    public static class All extends CompositeCondition
    {

        public All(List<Predicate<ConditionContext>> children)
        {
            super(children);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return children.stream().allMatch(t -> t.test(conditionContext));
        }
    }

    public static class Not extends CompositeCondition
    {

        public Not(List<Predicate<ConditionContext>> children)
        {
            super(children);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return !children.stream().allMatch(t -> t.test(conditionContext));
        }
    }
}
