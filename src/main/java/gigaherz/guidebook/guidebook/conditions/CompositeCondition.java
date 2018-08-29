package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookParsingException;

import java.util.List;
import java.util.function.Predicate;
import org.w3c.dom.Node;

import com.google.common.base.Strings;

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
        IDisplayConditionFactory exac = (doc, node) -> new Exac(BookDocument.parseChildConditions(doc, node),node);
        IDisplayConditionFactory min = (doc, node) -> new Min(BookDocument.parseChildConditions(doc, node),node);
        IDisplayConditionFactory max = (doc, node) -> new Max(BookDocument.parseChildConditions(doc, node),node);
        ConditionManager.register("any", any);
        ConditionManager.register("and", all);
        ConditionManager.register("all", all);
        ConditionManager.register("or", any);
        ConditionManager.register("not", not);
        ConditionManager.register("exac", exac);
        ConditionManager.register("min", min);
        ConditionManager.register("max", max);
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
    
    public String CheckAttribute(Node xmlNode, String key) 
    {
    	Node attr = xmlNode.getAttributes().getNamedItem(key);
        if (attr == null)
            throw new BookParsingException("Missing required XML attribute '"+key+"'.");

        String name = attr.getTextContent();
        if (Strings.isNullOrEmpty(name))
            throw new BookParsingException("Missing required XML attribute '"+key+"'.");
        return name;
    }
    
    public static class Exac extends CompositeCondition
    {
    	private final int value;

        public Exac(List<Predicate<ConditionContext>> children, Node xmlNode)
        {
            super(children);
        	this.value = Integer.parseInt(CheckAttribute(xmlNode, "value"));
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
        	int cnt = 0;
        	for(Predicate<ConditionContext> t: children) {
        		if(t.test(conditionContext)) {
        			cnt++;
        			if(cnt>this.value) return false;
        		}
        	}
            return cnt == this.value;
        }
    }
    
    public static class Min extends CompositeCondition
    {
    	private final int value;

        public Min(List<Predicate<ConditionContext>> children, Node xmlNode)
        {
            super(children);
        	this.value = Integer.parseInt(CheckAttribute(xmlNode, "value"));
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
        	int cnt = 0;
        	for(Predicate<ConditionContext> t: children) {
        		if(t.test(conditionContext)) {
        			cnt++;
        			if(cnt>=this.value) return true;
        		}
        	}
            return cnt >= this.value;
        }
    }
    
    public static class Max extends CompositeCondition
    {
    	private final int value;

        public Max(List<Predicate<ConditionContext>> children, Node xmlNode)
        {
            super(children);
        	this.value = Integer.parseInt(CheckAttribute(xmlNode, "value"));
        }

        @Override
        public boolean test(ConditionContext conditionContext)
        {
        	int cnt = 0;
        	for(Predicate<ConditionContext> t: children) {
        		if(t.test(conditionContext)) {
        			cnt++;
        			if(cnt>this.value) return false;
        		}
        	}
            return cnt <= this.value;
        }
    }
}
