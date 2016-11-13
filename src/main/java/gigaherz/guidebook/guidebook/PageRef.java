package gigaherz.guidebook.guidebook;

import com.google.common.primitives.Ints;

import javax.annotation.Nullable;

public class PageRef
{
    public int chapter;
    public int page;

    public boolean resolvedNames = false;
    public String chapterName;
    public String pageName;

    public PageRef(int chapter, int page)
    {
        this.chapter = chapter;
        this.page = page;
        resolvedNames = true;
    }

    public PageRef(String chapter, @Nullable String page)
    {
        this.chapterName = chapter;
        this.pageName = page;
    }

    public void resolve(BookDocument bookDocument)
    {
        if (!resolvedNames)
        {
            if (chapterName != null)
            {
                Integer ch = Ints.tryParse(chapterName);
                if (ch != null)
                {
                    chapter = ch;
                }
                else
                {
                    chapter = bookDocument.chaptersByName.get(chapterName);
                }

                if (pageName != null)
                {
                    Integer pg = Ints.tryParse(pageName);
                    if (pg != null)
                    {
                        page = pg;
                    }
                    else
                    {
                        page = bookDocument.chapters.get(chapter).pagesByName.get(pageName);
                    }
                }
            }
            else if (pageName != null)
            {
                PageRef temp = bookDocument.pagesByName.get(pageName);
                temp.resolve(bookDocument);
                chapter = temp.chapter;
                page = temp.page;
            }
        }
    }

    public PageRef copy()
    {
        return new PageRef(chapter, page);
    }
}
