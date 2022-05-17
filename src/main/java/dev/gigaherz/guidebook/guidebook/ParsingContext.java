package dev.gigaherz.guidebook.guidebook;

import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;

import javax.xml.parsers.DocumentBuilder;
import java.util.function.Predicate;

public interface ParsingContext
{
    Predicate<ConditionContext> getCondition(String name);
    boolean loadedFromConfigFolder();
    DocumentBuilder xmlDocumentBuilder();
}
