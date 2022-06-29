package dev.gigaherz.guidebook.guidebook.book;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.elements.Element;
import dev.gigaherz.guidebook.guidebook.elements.ElementParagraph;
import dev.gigaherz.guidebook.guidebook.elements.TextStyle;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class BookDocument
{

    float fontSize = 1.0f;

    public SectionRef home = new SectionRef(0, 0);

    private final ResourceLocation bookLocation;
    String bookName;
    ResourceLocation bookCover;
    ResourceLocation bookModel;

    final List<ChapterData> chapters = Lists.newArrayList();
    final Map<Item, SectionRef> stackLinks = Maps.newHashMap();

    final Map<String, Integer> chaptersByName = Maps.newHashMap();
    final Map<String, SectionRef> sectionsByName = Maps.newHashMap();

    final Map<String, TemplateDefinition> templates = Maps.newHashMap();
    final Map<String, Predicate<ConditionContext>> conditions = Maps.newHashMap();

    private IBookGraphics renderingManager;

    ResourceLocation background;
    ResourceLocation widgets;

    public BookDocument(ResourceLocation bookLocation)
    {
        this.bookLocation = bookLocation;
    }

    public ResourceLocation getLocation()
    {
        return bookLocation;
    }

    @Nullable
    public String getName()
    {
        return bookName;
    }

    @Nullable
    public ResourceLocation getCover()
    {
        return bookCover;
    }

    @Nullable
    public ResourceLocation getModel()
    {
        return bookModel;
    }

    @Nullable
    public ResourceLocation getBackground()
    {
        return background;
    }

    @Nullable
    public ResourceLocation getWidgets()
    {
        return widgets;
    }

    @Nullable
    public IBookGraphics getRendering()
    {
        return renderingManager;
    }

    public void setRendering(IBookGraphics rendering)
    {
        this.renderingManager = rendering;
    }

    public ChapterData getChapter(int i)
    {
        return chapters.get(i);
    }

    @Nullable
    public SectionRef getStackLink(ItemStack stack)
    {
        Item item = stack.getItem();
        return stackLinks.get(item);
    }

    public float getFontSize()
    {
        return fontSize;
    }

    public int chapterCount()
    {
        return chapters.size();
    }

    public void findTextures(Set<Material> textures)
    {
        if (bookCover != null)
            textures.add(new Material(InventoryMenu.BLOCK_ATLAS, bookCover));

        for (ChapterData chapter : chapters)
        {
            for (PageData page : chapter.sections)
            {
                for (Element element : page.elements)
                {
                    element.findTextures(textures);
                }
            }
        }
    }

    public void initializeWithLoadError(String error)
    {
        ChapterData ch = new ChapterData(0);
        chapters.add(ch);

        PageData pg = new PageData(new SectionRef(0, 0));
        ch.sections.add(pg);

        pg.elements.add(ElementParagraph.of("Error loading book:", TextStyle.ERROR));
        pg.elements.add(ElementParagraph.of(error, TextStyle.ERROR));
    }

    public Predicate<ConditionContext> getCondition(String name)
    {
        return conditions.get(name);
    }

    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean anyChanged = false;
        for (ChapterData chapter : chapters)
        {
            anyChanged |= chapter.reevaluateConditions(ctx);
        }

        return anyChanged;
    }
}
