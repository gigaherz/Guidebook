package gigaherz.guidebook.guidebook.drawing;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class VisualLink extends VisualText
{
    private static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");

    public String webTarget;
    public PageRef target;
    public int colorHover = 0xFF77cc66;

    public boolean isHovering;

    public VisualLink(String text, Size size)
    {
        super(text, size);
    }

    @Override
    public void draw(IBookGraphics nav)
    {
        nav.addString(position.x, position.y, text, isHovering ? colorHover : color);
    }

    @Override
    public void click(IBookGraphics nav)
    {
        if (webTarget != null)
            clickWeb(nav);
        if (target != null)
            nav.navigateTo(target);
    }

    public void clickWeb(IBookGraphics nav)
    {
        GuiScreen parent = (GuiScreen) nav.owner();
        Minecraft mc = Minecraft.getMinecraft();

        if (!mc.gameSettings.chatLinks)
        {
            return;
        }

        try
        {
            URI uri = new URI(webTarget);
            String s = uri.getScheme();

            if (s == null)
            {
                throw new URISyntaxException(webTarget, "Missing protocol");
            }

            if (!PROTOCOLS.contains(s.toLowerCase(Locale.ROOT)))
            {
                throw new URISyntaxException(webTarget, "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
            }

            if (mc.gameSettings.chatLinksPrompt)
            {
                ReflectionHelper.setPrivateValue(GuiScreen.class, parent, uri, "field_175286_t", "clickedLinkURI");
                mc.displayGuiScreen(new GuiConfirmOpenLink(parent, webTarget, 31102009, false));
            }
            else
            {
                openWebLink(uri);
            }
        }
        catch (URISyntaxException urisyntaxexception)
        {
            GuidebookMod.logger.error("Can't open url {}", webTarget, urisyntaxexception);
        }
    }

    private static void openWebLink(URI url)
    {
        try
        {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null);
            oclass.getMethod("browse", new Class[]{URI.class}).invoke(object, url);
        }
        catch (Throwable throwable1)
        {
            GuidebookMod.logger.error("Can't open url {}", url, throwable1);
        }
    }
}
