package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;

import java.util.List;

public abstract class CompositeCondition implements IDisplayCondition
{
    public final List<IDisplayCondition> children;

    protected CompositeCondition(List<IDisplayCondition> children)
    {
        this.children = children;
    }

    public static void register()
    {
        IDisplayConditionFactory any = (doc, node) -> new Any(BookDocument.parseChildConditions(doc, node));
        IDisplayConditionFactory all = (doc, node) -> new All(BookDocument.parseChildConditions(doc, node));
        ConditionManager.register("any", any);
        ConditionManager.register("and", any);
        ConditionManager.register("all", all);
        ConditionManager.register("or", all);
    }

    public static class Any extends CompositeCondition
    {
        public Any(List<IDisplayCondition> children)
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

        public All(List<IDisplayCondition> children)
        {
            super(children);
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
            return children.stream().allMatch(t -> t.test(conditionContext));
        }
    }
}
