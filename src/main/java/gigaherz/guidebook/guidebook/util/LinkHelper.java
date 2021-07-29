package gigaherz.guidebook.guidebook.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.elements.LinkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.Util;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LinkHelper
{
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
                case "openUrl":
                    clickWeb(nav, context.textTarget);
                    break;
                case "copyText":
                    clickCopyToClipboard(nav, context.textTarget);
                    break;
                case "copyToChat":
                    clickCopyToChat(nav, context.textTarget);
                    break;
            }
        }
        if (context.target != null)
        {
            nav.navigateTo(context.target);
        }
    }

    public static void clickCopyToClipboard(IBookGraphics nav, String textTarget)
    {
        Screen parent = (Screen) nav.owner();
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConfirmScreen((result) -> {
            if (result)
            {
                GLFW.glfwSetClipboardString(mc.getWindow().getWindow(), textTarget);
                mc.gui.getChat().addMessage(new TranslatableComponent("text.gbook.actions.copy_to_clipboard.success"));
            }
            mc.setScreen(parent);
        },
                new TranslatableComponent("text.gbook.actions.copy_to_clipboard.line1"),
                new TranslatableComponent("text.gbook.actions.copy_to_clipboard.line2"))
        {
            @Override
            public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
            {
                parent.render(matrixStack,-1, -1, partialTicks);
                super.render(matrixStack, mouseX, mouseY, partialTicks);
            }
        });
    }

    public static void clickCopyToChat(IBookGraphics nav, String textTarget)
    {
        Screen parent = (Screen) nav.owner();
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ChatScreen(textTarget)
        {
            @Override
            public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
            {
                parent.render(matrixStack,-1, -1, partialTicks);
                String text = "Temporary chat window open, press ESCAPE to cancel.";
                int textWidth = Math.max(font.width(text) + 40, width / 2);
                fill(matrixStack,(width - textWidth) / 2, height / 4, (width + textWidth) / 2, height * 3 / 4, 0x7F000000);
                drawCenteredString(matrixStack,font, text, width / 2, (height - font.lineHeight) / 2, 0xFFFFFFFF);
                super.render(matrixStack,mouseX, mouseY, partialTicks);
            }

            @Override
            public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_)
            {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE)
                {
                    mc.setScreen(parent);
                    return true;
                }

                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                {
                    String s = this.input.getValue().trim();

                    if (!s.isEmpty())
                    {
                        this.sendMessage(s);
                    }

                    this.minecraft.setScreen(parent);
                    return true;
                }

                return super.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
            }
        });
    }

    public static void clickWeb(IBookGraphics nav, String textTarget)
    {
        Screen parent = (Screen) nav.owner();
        Minecraft mc = Minecraft.getInstance();

        if (!mc.options.chatLinks)
        {
            List<ItemStack> stacks = Lists.newArrayList();
            for (int i=0;i<54;i++)
            {
                stacks.add(ItemStack.EMPTY);
            }
            Stream.generate(() -> ItemStack.EMPTY).limit(54).forEach(stacks::add);
            stacks = IntStream.range(0,54).mapToObj(i -> ItemStack.EMPTY).collect(Collectors.toList());
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

            if (mc.options.chatLinksPrompt)
            {
                mc.setScreen(new ConfirmLinkScreen((result) -> {
                    if (result)
                    {
                        openWebLink(uri);
                    }

                    mc.setScreen(parent);
                }, textTarget, true)
                {
                    @Override
                    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
                    {
                        parent.render(matrixStack,-1, -1, partialTicks);
                        super.render(matrixStack,mouseX, mouseY, partialTicks);
                    }
                });
            }
            else
            {
                openWebLink(uri);
            }
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
