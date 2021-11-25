package dev.gigaherz.guidebook.guidebook.util;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.elements.LinkContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class LinkHelper
{
    private static final TranslatableComponent COPY_TO_CLIPBOARD_1 = new TranslatableComponent("text.gbook.actions.copy_to_clipboard.line1");
    private static final TranslatableComponent COPY_TO_CLIPBOARD_2 = new TranslatableComponent("text.gbook.actions.copy_to_clipboard.line2");
    private static final Set<String> PROTOCOLS = Sets.newHashSet("http", "https");

    public interface ILinkable
    {
        void setLinkContext(LinkContext ctx);
    }

    public static void click(IBookGraphics nav, LinkContext context)
    {
        if (context.textTarget != null && context.textAction != null)
        {
            switch (context.textAction)
            {
                case "openUrl" -> clickWeb(context.textTarget);
                case "copyText" -> clickCopyToClipboard(context.textTarget);
                case "copyToChat" -> clickCopyToChat(context.textTarget);
            }
        }
        if (context.target != null)
        {
            nav.navigateTo(context.target);
        }
    }

    public static void clickCopyToClipboard(String textTarget)
    {
        Minecraft mc = Minecraft.getInstance();
        mc.pushGuiLayer(new ConfirmScreen((result) -> {
            if (result)
            {
                GLFW.glfwSetClipboardString(mc.getWindow().getWindow(), textTarget);
                mc.gui.getChat().addMessage(new TranslatableComponent("text.gbook.actions.copy_to_clipboard.success"));
            }
            mc.popGuiLayer();
        }, COPY_TO_CLIPBOARD_1, COPY_TO_CLIPBOARD_2));
    }

    public static void clickCopyToChat(String textTarget)
    {
        Minecraft mc = Minecraft.getInstance();
        mc.pushGuiLayer(new ChatScreen(textTarget)
        {
            @Override
            public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
            {
                String text = "Temporary chat window open, press ESCAPE to cancel.";
                int textWidth = Math.max(font.width(text) + 40, width / 2);
                fill(matrixStack, (width - textWidth) / 2, height / 4, (width + textWidth) / 2, height * 3 / 4, 0x7F000000);
                drawCenteredString(matrixStack, font, text, width / 2, (height - font.lineHeight) / 2, 0xFFFFFFFF);
                super.render(matrixStack, mouseX, mouseY, partialTicks);
            }

            @Override
            public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_)
            {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE)
                {
                    mc.popGuiLayer();
                    return true;
                }

                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                {
                    String s = this.input.getValue().trim();

                    if (!s.isEmpty())
                    {
                        this.sendMessage(s);
                    }

                    mc.popGuiLayer();
                    return true;
                }

                return super.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
            }
        });
    }

    public static void clickWeb(String textTarget)
    {
        Minecraft mc = Minecraft.getInstance();

        if (!mc.options.chatLinks)
        {
            return;
        }

        try
        {
            URI uri = new URI(textTarget);
            String s = uri.getScheme();

            if (s == null)
            {
                throw new URISyntaxException(textTarget, "Missing protocol");
            }

            if (!PROTOCOLS.contains(s.toLowerCase(Locale.ROOT)))
            {
                throw new URISyntaxException(textTarget, "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
            }

            if (!mc.options.chatLinksPrompt)
            {
                openWebLink(uri);
                return;
            }

            mc.pushGuiLayer(new ConfirmLinkScreen((result) -> {
                if (result)
                {
                    openWebLink(uri);
                }
                mc.popGuiLayer();
            }, textTarget, true));
        }
        catch (URISyntaxException urisyntaxexception)
        {
            GuidebookMod.logger.error("Can't open url {}", textTarget, urisyntaxexception);
        }
    }

    private static void openWebLink(URI url)
    {
        Util.getPlatform().openUri(url);
    }
}
