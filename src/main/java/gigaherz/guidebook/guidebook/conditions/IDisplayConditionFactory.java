package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;
import org.w3c.dom.Node;

import java.util.function.Predicate;

@FunctionalInterface
public interface IDisplayConditionFactory
{
    Predicate<ConditionContext> parse(BookDocument document, Node xmlNode);
}
