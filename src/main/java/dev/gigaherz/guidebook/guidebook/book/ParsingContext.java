package dev.gigaherz.guidebook.guidebook.book;

import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import java.util.function.Predicate;

public interface ParsingContext
{
    boolean loadedFromConfigFolder();
    DocumentBuilder xmlDocumentBuilder();
    BookDocument document();
    default Predicate<ConditionContext> getCondition(String name)
    {
        var doc = document();
        if (doc == null)
            return null;
        return doc.getCondition(name);
    }
    default ChapterData chapter()
    {
        return null;
    }
    default boolean isFirstElement()
    {
        return false;
    }
    default boolean isLastElement()
    {
        return false;
    }

    abstract class Wrapper implements ParsingContext
    {
        private final ParsingContext delegate;

        public Wrapper(ParsingContext delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public boolean loadedFromConfigFolder()
        {
            return delegate.loadedFromConfigFolder();
        }

        @Override
        public DocumentBuilder xmlDocumentBuilder()
        {
            return delegate.xmlDocumentBuilder();
        }

        @Override
        public BookDocument document()
        {
            return delegate.document();
        }

        @Override
        public Predicate<ConditionContext> getCondition(String name)
        {
            return delegate.getCondition(name);
        }

        @Override
        public ChapterData chapter()
        {
            return delegate.chapter();
        }

        @Override
        public boolean isFirstElement()
        {
            return delegate.isFirstElement();
        }

        @Override
        public boolean isLastElement()
        {
            return delegate.isLastElement();
        }
    }
}
