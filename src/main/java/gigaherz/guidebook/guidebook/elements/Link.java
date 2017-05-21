package gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.PageRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.util.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class Link extends Paragraph implements IClickablePageElement
{
    private static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");

    public String webTarget;
    public PageRef target;
    public int colorHover = 0xFF77cc66;

    public boolean isHovering;
    public Rectangle bounds;

    public Link(String text)
    {
        super(text);
        underline = true;
        color = 0xFF7766cc;
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
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
        GuiScreen parent = (GuiScreen)nav.owner();
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

    @Override
    public int apply(IBookGraphics nav, int left, int top)
    {
        bounds = nav.getStringBounds(text, left, top);

        return nav.addStringWrapping(left + indent, top, text, isHovering ? colorHover : color, alignment) + space;
    }

    @Override
    public void parse(NamedNodeMap attributes)
    {
        super.parse(attributes);

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();
            target=PageRef.fromString(ref,true);
        }

        attr = attributes.getNamedItem("href");
        if (attr != null)
        {
            webTarget = attr.getTextContent();
        }
    }

    @Override
    public IPageElement copy()
    {
        Link link = new Link(text);
        link.alignment = alignment;
        link.color = color;
        link.indent = indent;
        link.space = space;
        link.bold = bold;
        link.italics = italics;
        link.underline = underline;

        link.target = target.copy();
        link.webTarget = webTarget;
        link.colorHover = colorHover;

        return link;
    }

    private static void openWebLink(URI url)
    {
        try
        {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null);
            oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, url);
        }
        catch (Throwable throwable1)
        {
            GuidebookMod.logger.error("Can't open url {}", url, throwable1);
        }
    }

}
