package gigaherz.guidebook.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.IBookGraphics;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import jdk.nashorn.internal.ir.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiGuidebook extends GuiScreen
{
    private static final ResourceLocation BOOK_GUI_TEXTURES = GuidebookMod.location("textures/gui/book.png");

    public final ResourceLocation bookLocation;

    private GuiButton buttonClose;
    private GuiButton buttonNextPage;
    private GuiButton buttonPreviousPage;
    private GuiButton buttonNextChapter;
    private GuiButton buttonPreviousChapter;
    private GuiButton buttonBack;
    private GuiButton buttonHome;

    private ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
    private TextureManager renderEngine = Minecraft.getMinecraft().renderEngine;

    private IBookGraphics book;
    private AnimatedBookBackground background;
    public static boolean useNaturalArrows = false;

    public GuiGuidebook(ResourceLocation book)
    {
        bookLocation = book;
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    private boolean initialized = false;

    @Override
    public void initGui()
    {
        // Called on initial open, and on changing resolution
        if (!initialized)
        {
            initialized = true;

            background = new AnimatedBookBackground(this);

            EntityPlayerSP player = Minecraft.getMinecraft().player;
            ConditionContext conditionContext = new ConditionContext();
            conditionContext.setPlayer(player);

            BookDocument theBook = BookRegistry.get(bookLocation);
            book = theBook.getRendering();
            boolean conditions = theBook.reevaluateConditions(conditionContext);
            if (book == null)
            {
                book = new BookRendering(theBook, this);
                theBook.setRendering(book);
            }
            else if(conditions || book.refreshScalingFactor())
            {
                book.resetRendering(conditions);
            }

            int btnId = 0;

            int left = (this.width - BookRendering.DEFAULT_BOOK_WIDTH) / 2;
            int right = left + BookRendering.DEFAULT_BOOK_WIDTH;
            int top = (this.height - BookRendering.DEFAULT_BOOK_HEIGHT) / 2 - 9;
            int bottom = top + BookRendering.DEFAULT_BOOK_HEIGHT;
            this.buttonHome = new SpriteButton(btnId++, left - 10, top - 8, 6);
            this.buttonBack = new SpriteButton(btnId++, left + 8, top - 5, 2);
            this.buttonClose = new SpriteButton(btnId++, right - 6, top - 6, 3);
            if (useNaturalArrows)
            {
                this.buttonPreviousPage = new SpriteButton(btnId++, left + 24, bottom - 13, 1);
                this.buttonNextPage = new SpriteButton(btnId++, right - 42, bottom - 13, 0);
                this.buttonPreviousChapter = new SpriteButton(btnId++, left + 2, bottom - 13, 5);
                this.buttonNextChapter = new SpriteButton(btnId++, right - 23, bottom - 13, 4);
            }
            else
            {
                this.buttonPreviousPage = new SpriteButton(btnId++, left + 24, bottom - 13, 0);
                this.buttonNextPage = new SpriteButton(btnId++, right - 42, bottom - 13, 1);
                this.buttonPreviousChapter = new SpriteButton(btnId++, left + 2, bottom - 13, 4);
                this.buttonNextChapter = new SpriteButton(btnId++, right - 23, bottom - 13, 5);
            }
            GuidebookMod.logger.info("Showing gui with " + btnId + " buttons.");
        }

        this.buttonList.add(this.buttonHome);
        this.buttonList.add(this.buttonBack);
        this.buttonList.add(this.buttonClose);
        this.buttonList.add(this.buttonPreviousPage);
        this.buttonList.add(this.buttonNextPage);
        this.buttonList.add(this.buttonPreviousChapter);
        this.buttonList.add(this.buttonNextChapter);

        updateButtonStates();

        repositionButtons();

        setupConditionsAndPosition();
    }

    private void setupConditionsAndPosition()
    {
        book.refreshScalingFactor();
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.enabled)
        {
            if (button.id == buttonClose.id)
            {
                background.startClosing();
            }
            else if (button.id == buttonHome.id)
            {
                book.navigateHome();
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
        buttonHome.enabled = background.isFullyOpen() && book.getBook().home != null;
        buttonBack.enabled = background.isFullyOpen() && book.canGoBack();
        buttonNextPage.enabled = background.isFullyOpen() && book.canGoNextPage();
        buttonPreviousPage.enabled = background.isFullyOpen() && book.canGoPrevPage();
        buttonNextChapter.enabled = background.isFullyOpen() && book.canGoNextChapter();
        buttonPreviousChapter.enabled = background.isFullyOpen() && book.canGoPrevChapter();

        buttonClose.visible = buttonClose.enabled;
        buttonHome.visible = buttonHome.enabled;
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

    private void repositionButtons()
    {
        book.refreshScalingFactor();

        float bookScale = book.getScalingFactor() / book.getBook().getFontSize();
        float bookWidth = BookRendering.DEFAULT_BOOK_WIDTH * bookScale;
        float bookHeight = BookRendering.DEFAULT_BOOK_HEIGHT * bookScale;

        int left = (int) ((this.width - bookWidth) / 2);
        int right = (int) (left + bookWidth);
        int top = (int) ((this.height - bookHeight) / 2 - 9);
        int bottom = (int) (top + bookHeight);
        buttonHome.x = left - 10;
        buttonHome.y = top - 8;
        buttonBack.x = left + 8;
        buttonBack.y = top - 5;
        buttonClose.x = right - 6;
        buttonClose.y = top - 6;
        buttonPreviousPage.x = left + 24;
        buttonPreviousPage.y = bottom - 13;
        buttonNextPage.x = right - 42;
        buttonNextPage.y = bottom - 13;
        buttonPreviousChapter.x = left + 2;
        buttonPreviousChapter.y = bottom - 13;
        buttonNextChapter.x = right - 23;
        buttonNextChapter.y = bottom - 13;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        float bookScale = book.getScalingFactor() / book.getBook().getFontSize();
        float bookHeight = BookRendering.DEFAULT_BOOK_HEIGHT * bookScale;

        background.draw(partialTicks, (int) bookHeight, bookScale);

        if (background.isFullyOpen())
        {
            book.drawCurrentPages();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (background.isFullyOpen())
        {
            book.mouseHover(mouseX, mouseY);
        }
    }

    public void drawTooltip(ItemStack stack, int x, int y)
    {
        renderToolTip(stack, x, y);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (book.mouseClicked(mouseButton))
            return;

        if (mouseButton == 3)
        {
            book.navigateBack();
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public FontRenderer getFontRenderer()
    {
        return fontRenderer;
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

        private static final int[] xPixel = {5, 5, 4, 4, 4, 4, 4, 29};
        private static final int[] yPixel = {2, 16, 30, 64, 79, 93, 107, 107};
        private static final int[] xSize = {17, 17, 18, 13, 21, 21, 15, 15};
        private static final int[] ySize = {11, 11, 11, 13, 11, 11, 15, 15};

        public SpriteButton(int buttonId, int x, int y, int iconIndex)
        {
            super(buttonId, x, y, xSize[iconIndex], ySize[iconIndex], "");
            this.whichIcon = iconIndex;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float p_191745_4_)
        {
            if (this.visible)
            {
                boolean hover =
                        mouseX >= this.x &&
                                mouseY >= this.y &&
                                mouseX < this.x + this.width &&
                                mouseY < this.y + this.height;

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

                this.drawTexturedModalRect(this.x, this.y, x, y, w, h);
            }
        }
    }
}
