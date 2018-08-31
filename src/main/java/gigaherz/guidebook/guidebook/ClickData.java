package gigaherz.guidebook.guidebook;

import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.elements.Element;
import gigaherz.guidebook.guidebook.elements.ElementLink;
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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ClickData
{
    private static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");
    
    public String urlTarget;
    public String clipboardText;
    public SectionRef target;

    public ClickData() 
    {
    	
    }
    public ClickData(NamedNodeMap attributes)
    {

        Node attr = attributes.getNamedItem("ref");
        if (attr != null)
        {
            String ref = attr.getTextContent();
            target = SectionRef.fromString(ref);
        }

        attr = attributes.getNamedItem("href");
        if (attr != null)
        {
            urlTarget = attr.getTextContent();
        }

        attr = attributes.getNamedItem("copy-text");
        if (attr != null)
        {
        	clipboardText = attr.getTextContent();
        }
    }
    
    public ClickData copy()
    {
        ClickData newData = new ClickData();
    	newData.urlTarget = urlTarget;
    	newData.clipboardText = clipboardText;
    	newData.target = target.copy();
        return newData;
    }
    
    public void click(IBookGraphics nav)
    {
        if (urlTarget != null)
            clickWeb(nav);
        if (clipboardText != null)
            clickCopyToClipboard(nav);
        if (target != null)
            nav.navigateTo(target);
    }

    public void clickCopyToClipboard(IBookGraphics nav)
    {
        GuiScreen parent = (GuiScreen) nav.owner();
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiYesNo((result, id) -> {
            if (result)
            {
                GuiScreen.setClipboardString(clipboardText);
                mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("text.copyToClipboard.success"));
            }
            mc.displayGuiScreen(parent);
        },
                I18n.format("text.copyToClipboard.line1"),
                I18n.format("text.copyToClipboard.line2"),
                0)
        {
            @Override
            public void drawScreen(int mouseX, int mouseY, float partialTicks)
            {
                parent.drawScreen(-1, -1, partialTicks);
                super.drawScreen(mouseX, mouseY, partialTicks);
            }
        });
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
            URI uri = new URI(urlTarget);
            String s = uri.getScheme();

            if (s == null)
            {
                throw new URISyntaxException(urlTarget, "Missing protocol");
            }

            if (!PROTOCOLS.contains(s.toLowerCase(Locale.ROOT)))
            {
                throw new URISyntaxException(urlTarget, "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
            }

            if (mc.gameSettings.chatLinksPrompt)
            {
                ReflectionHelper.setPrivateValue(GuiScreen.class, parent, uri, "field_175286_t", "clickedLinkURI");
                mc.displayGuiScreen(new GuiConfirmOpenLink(parent, urlTarget, 31102009, false));
            }
            else
            {
                openWebLink(uri);
            }
        }
        catch (URISyntaxException urisyntaxexception)
        {
            GuidebookMod.logger.error("Can't open url {}", urlTarget, urisyntaxexception);
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
