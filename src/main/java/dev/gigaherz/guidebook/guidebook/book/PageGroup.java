package dev.gigaherz.guidebook.guidebook.book;

import com.google.common.collect.Lists;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPage;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPageBreak;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.util.Point2I;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;

import java.util.List;

/**
 * This class represents a group of pages clearly delimited by start/end of chapters, sections, or explicit section braks.
 * Example:
 * <pre>{@code
 * <book>
 *   <chapter>
 *     <section>
 *       Group 1
 *     </section>
 *     <section>
 *       Group 2
 *     </section>
 *   </chapter>
 *   <chapter>
 *     Group 3
 *     <page_break />
 *     Group 4
 *   </chapter>
 * </book>
 * }</pre>
 */
public class PageGroup extends PageData
{
    public PageGroup(SectionRef ref)
    {
        super(ref);
    }

    @Override
    public List<VisualPage> reflow(IBookGraphics rendering, Size pageSize)
    {
        List<VisualPage> pages = Lists.newArrayList();

        VisualPage page = new VisualPage(ref);
        Rect pageBounds = new Rect(new Point2I(0, 0), pageSize);

        int top = pageBounds.position.y();
        for (Element element : elements)
        {
            if (element.conditionResult)
            {
                top = element.reflow(page.children, rendering, new Rect(new Point2I(pageBounds.position.x(), top), pageBounds.size), pageBounds);
            }
        }

        boolean needsRepagination = false;
        for (VisualElement child : page.children)
        {
            if (child instanceof VisualPageBreak || (child.position.y() + child.size.height() > (pageBounds.position.y() + pageBounds.size.height())))
            {
                needsRepagination = true;
                break;
            }
        }

        if (needsRepagination)
        {
            VisualPage page2 = new VisualPage(ref);

            int offsetY = 0;
            boolean pageBreakRequired = false;
            for (VisualElement child : page.children)
            {
                int cpy = child.position.y() + offsetY;
                if (pageBreakRequired || (cpy + child.size.height() > (pageBounds.position.y() + pageBounds.size.height())
                        && child.position.y() > pageBounds.position.y()))
                {
                    pages.add(page2);
                    page2 = new VisualPage(ref);

                    offsetY = pageBounds.position.y() - child.position.y();
                    pageBreakRequired = false;
                }

                if (child instanceof VisualPageBreak)
                {
                    pageBreakRequired = true;
                }
                else
                {
                    child.position = new Point2I(
                            child.position.x(),
                            child.position.y() + offsetY);
                    page2.children.add(child);
                }
            }

            pages.add(page2);
        }
        else
        {
            pages.add(page);
        }

        return pages;
    }
}
