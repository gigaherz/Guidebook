package gigaherz.guidebook.guidebook.conditions;

import gigaherz.guidebook.guidebook.BookDocument;
import org.w3c.dom.Node;

@FunctionalInterface
public interface IDisplayConditionFactory
{
    IDisplayCondition parse(BookDocument document, Node xmlNode);
}
