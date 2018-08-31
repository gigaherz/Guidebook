package gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.ClickData;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.SectionRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class VisualLink extends VisualText
{

    public static class SharedHoverContext
    {
        private boolean isHovering;
    }

    public ClickData clickData;
    public int colorHover = 0xFF77cc66;

    public SharedHoverContext hoverContext = new SharedHoverContext();

    public VisualLink(String text, Size size, int positionMode, float baseline, int verticalAlign, float scale)
    {
        super(text, size, positionMode, baseline, verticalAlign, scale);
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.addString(position.x, position.y, text, hoverContext.isHovering ? colorHover : color, scale);
    }

    @Override
    public boolean wantsHover()
    {
        return true;
    }

    @Override
    public void mouseOver(IBookGraphics nav, int x, int y)
    {
        hoverContext.isHovering = true;
    }

    @Override
    public void mouseOut(IBookGraphics nav, int x, int y)
    {
        hoverContext.isHovering = false;
    }

    @Override
    public void click(IBookGraphics nav)
    {
        clickData.click(nav);
    }

}
