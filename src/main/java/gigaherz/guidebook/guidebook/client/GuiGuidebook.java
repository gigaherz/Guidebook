package gigaherz.guidebook.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiGuidebook extends GuiScreen
{
    private static final ResourceLocation BOOK_GUI_TEXTURES = new ResourceLocation("guidebook:textures/gui/book.png");

    public final ResourceLocation bookLocation;

    private GuiButton buttonClose;
    private GuiButton buttonNextPage;
    private GuiButton buttonPreviousPage;
    private GuiButton buttonNextChapter;
    private GuiButton buttonPreviousChapter;
    private GuiButton buttonBack;

    private ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
    private TextureManager renderEngine = Minecraft.getMinecraft().renderEngine;

    private BookDocument book;
    private AnimatedBookBackground background;

    public GuiGuidebook(ResourceLocation book)
    {
        bookLocation = book;
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    public void initGui()
    {
        book = BookDocument.get(bookLocation);
        background = new AnimatedBookBackground(this, book.getBookHeight());

        this.buttonList.clear();

        int btnId = 0;

        int left = (this.width - book.getBookWidth()) / 2;
        int right = left + book.getBookWidth();
        int top = (this.height - book.getBookHeight()) / 2 - 9;
        int bottom = top + book.getBookHeight();
        this.buttonList.add(this.buttonBack = new SpriteButton(btnId++, left - 9, top - 5, 2));
        this.buttonList.add(this.buttonClose = new SpriteButton(btnId++, right - 6, top - 6, 3));
        this.buttonList.add(this.buttonPreviousPage = new SpriteButton(btnId++, left + 24, bottom - 13, 1));
        this.buttonList.add(this.buttonNextPage = new SpriteButton(btnId++, right - 42, bottom - 13, 0));
        this.buttonList.add(this.buttonPreviousChapter = new SpriteButton(btnId++, left + 2, bottom - 13, 5));
        this.buttonList.add(this.buttonNextChapter = new SpriteButton(btnId++, right - 23, bottom - 13, 4));
        GuidebookMod.logger.info("Showing gui with " + btnId + " buttons.");


        updateButtonStates();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id == buttonClose.id)
            {
                background.startClosing();
            }
            else if (button.id == buttonBack.id)
            {
                book.navigateBack();
            }
            else if (button.id == buttonNextPage.id)
            {
                book.nextPage();
            }
            else if (button.id == buttonPreviousPage.id)
            {
                book.prevPage();
            }
            else if (button.id == buttonNextChapter.id)
            {
                book.nextChapter();
            }
            else if (button.id == buttonPreviousChapter.id)
            {
                book.prevChapter();
            }

            updateButtonStates();
        }
    }

    private void updateButtonStates()
    {
        buttonClose.enabled = background.isFullyOpen();
        buttonBack.enabled = background.isFullyOpen() && book.canGoBack();
        buttonNextPage.enabled = background.isFullyOpen() && book.canGoNextPage();
        buttonPreviousPage.enabled = background.isFullyOpen() && book.canGoPrevPage();
        buttonNextChapter.enabled = background.isFullyOpen() && book.canGoNextChapter();
        buttonPreviousChapter.enabled = background.isFullyOpen() && book.canGoPrevChapter();

        buttonClose.visible = buttonClose.enabled;
        buttonBack.visible = buttonBack.enabled;
        buttonNextPage.visible = buttonNextPage.enabled;
        buttonPreviousPage.visible = buttonPreviousPage.enabled;
        buttonNextChapter.visible = buttonNextChapter.enabled;
        buttonPreviousChapter.visible = buttonPreviousChapter.enabled;
    }

    @Override
    public void updateScreen()
    {
        if (background.update())
            mc.displayGuiScreen(null);

        updateButtonStates();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            background.startClosing();
            return;
        }
        else if (keyCode == Keyboard.KEY_BACK)
        {
            book.navigateBack();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        background.draw(partialTicks);

        if (background.isFullyOpen())
        {
            book.drawCurrentPages(this);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (book.mouseClicked(mouseX, mouseY, mouseButton))
            return;

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public FontRenderer getFontRenderer()
    {
        return fontRendererObj;
    }

    public ItemModelMesher getMesher()
    {
        return mesher;
    }

    public TextureManager getRenderEngine()
    {
        return renderEngine;
    }

    @SideOnly(Side.CLIENT)
    static class SpriteButton extends GuiButton
    {
        private final int whichIcon;

        private static final int[] xPixel = {5, 5, 4, 4, 4, 4};
        private static final int[] yPixel = {2, 16, 30, 64, 79, 93};
        private static final int[] xSize = {17, 17, 18, 13, 21, 21};
        private static final int[] ySize = {11, 11, 11, 13, 11, 11};

        public SpriteButton(int buttonId, int x, int y, int back)
        {
            super(buttonId, x, y, xSize[back], ySize[back], "");
            this.whichIcon = back;
        }

        /**
         * Draws this button to the screen.
         */
        public void drawButton(Minecraft mc, int mouseX, int mouseY)
        {
            if (this.visible)
            {
                boolean hover =
                        mouseX >= this.xPosition &&
                                mouseY >= this.yPosition &&
                                mouseX < this.xPosition + this.width &&
                                mouseY < this.yPosition + this.height;

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(BOOK_GUI_TEXTURES);
                int x = xPixel[whichIcon];
                int y = yPixel[whichIcon];
                int w = xSize[whichIcon];
                int h = ySize[whichIcon];

                if (hover)
                {
                    x += 25;
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, x, y, w, h);
            }
        }
    }
}
