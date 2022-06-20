package dev.gigaherz.guidebook.guidebook.book;

import org.w3c.dom.Node;

import java.util.concurrent.atomic.AtomicInteger;

public interface DocumentLevelElementParser
{
    void parse(ParsingContext context, AtomicInteger chapterNumber, Node node);
}
