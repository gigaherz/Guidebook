package gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.platform.GlStateManager;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

public class GuiGuidebook extends Screen
{
    private static final ResourceLocation BOOK_GUI_TEXTURES = GuidebookMod.location("textures/gui/book.png");

    public final ResourceLocation bookLocation;

    private Button buttonClose;
    private Button buttonNextPage;
    private Button buttonPreviousPage;
    private Button buttonNextChapter;
    private Button buttonPreviousChapter;
    private Button buttonBack;
    private Button buttonHome;

    private ItemModelMesher mesher = Minecraft.getInstance().getItemRenderer().getItemModelMesher();
    private TextureManager renderEngine = Minecraft.getInstance().textureManager;

    private BookRendering book;
    private IAnimatedBookBackground background;
    public static boolean useNaturalArrows = false;

    public GuiGuidebook(ResourceLocation book)
    {
        super(new TranslationTextComponent("text.gbook.book.title"));
        bookLocation = book;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private boolean initialized = false;

    @Override
    public void init()
    {
        // Called on initial open, and on changing resolution
        if (!initialized)
        {
            initialized = true;

            ClientPlayerEntity player = Minecraft.getInstance().player;
            ConditionContext conditionContext = new ConditionContext();
            conditionContext.setPlayer(player);

            BookDocument theBook = BookRegistry.get(bookLocation);
            book = (BookRendering) theBook.getRendering();

            boolean conditions = theBook.reevaluateConditions(conditionContext);
            if (book == null)
            {
                book = new BookRendering(theBook, this);
                theBook.setRendering(book);
            }
            else
            {
                book.setGui(this);
                if (conditions || book.refreshScalingFactor())
                {
                    book.resetRendering(conditions);
                }
            }

            background = book.createBackground(this);

            int left = (this.width - BookRendering.DEFAULT_BOOK_WIDTH) / 2;
            int right = left + BookRendering.DEFAULT_BOOK_WIDTH;
            int top = (this.height - BookRendering.DEFAULT_BOOK_HEIGHT) / 2 - 9;
            int bottom = top + BookRendering.DEFAULT_BOOK_HEIGHT;
            this.addButton(this.buttonHome = new SpriteButton(left - 10, top - 8, 6, this::onHomeClicked));
            this.addButton(this.buttonBack = new SpriteButton(left + 8, top - 5, 2, this::onBackClicked));
            this.addButton(this.buttonClose = new SpriteButton(right - 6, top - 6, 3, this::onCloseClicked));
            if (useNaturalArrows)
            {
                this.addButton(this.buttonPreviousPage = new SpriteButton(left + 24, bottom - 13, 1, this::onPrevPageClicked));
                this.addButton(this.buttonNextPage = new SpriteButton(right - 42, bottom - 13, 0, this::onNextPageClicked));
                this.addButton(this.buttonPreviousChapter = new SpriteButton(left + 2, bottom - 13, 5, this::onPrevChapterClicked));
                this.addButton(this.buttonNextChapter = new SpriteButton(right - 23, bottom - 13, 4, this::onNextChapterClicked));
            }
            else
            {
                this.addButton(this.buttonPreviousPage = new SpriteButton(left + 24, bottom - 13, 0, this::onPrevPageClicked));
                this.addButton(this.buttonNextPage = new SpriteButton(right - 42, bottom - 13, 1, this::onNextPageClicked));
                this.addButton(this.buttonPreviousChapter = new SpriteButton(left + 2, bottom - 13, 4, this::onPrevChapterClicked));
                this.addButton(this.buttonNextChapter = new SpriteButton(right - 23, bottom - 13, 5, this::onNextChapterClicked));
            }
        }

        updateButtonStates();

        repositionButtons();
    }

    private void setupConditionsAndPosition()
    {
        this.width = minecraft.mainWindow.getScaledWidth();
        this.height = minecraft.mainWindow.getScaledHeight();
        if (book.refreshScalingFactor())
        {
            book.resetRendering(false);
        }
    }

    private void updateButtonStates()
    {
        buttonClose.visible = background.isFullyOpen();
        buttonHome.visible = background.isFullyOpen() && book.getBook().home != null;
        buttonBack.visible = background.isFullyOpen() && book.canGoBack();
        buttonNextPage.visible = background.isFullyOpen() && book.canGoNextPage();
        buttonPreviousPage.visible = background.isFullyOpen() && book.canGoPrevPage();
        buttonNextChapter.visible = background.isFullyOpen() && book.canGoNextChapter();
        buttonPreviousChapter.visible = background.isFullyOpen() && book.canGoPrevChapter();
    }

    @Override
    public void tick()
    {
        super.tick();

        if (background.update())
            minecraft.displayGuiScreen(null);

        updateButtonStates();
    }

    @Override
    public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            background.startClosing();
            return true;
        }
        else if (keyCode == GLFW.GLFW_KEY_BACKSPACE)
        {
            book.navigateBack();
            return true;
        }

        return super.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
    }

    private void repositionButtons()
    {
        setupConditionsAndPosition();

        double bookScale = book.getScalingFactor() / book.getBook().getFontSize();
        double bookWidth = (BookRendering.DEFAULT_BOOK_WIDTH) * bookScale;
        double bookHeight = (BookRendering.DEFAULT_BOOK_HEIGHT) * bookScale;

        int left = (int) ((this.width - bookWidth) / 2);
        int right = (int) (left + bookWidth);
        int top = (int) ((this.height - bookHeight) / 2);
        int bottom = (int) (top + bookHeight);

        int leftLeft = left;
        int rightRight = right;
        int topTop = top - 16 + (int) (8 * bookScale);
        int bottomBottom = bottom + 2;

        buttonHome.x = leftLeft;
        buttonHome.y = topTop;
        buttonBack.x = leftLeft + 18;
        buttonBack.y = topTop + 3;

        buttonClose.x = rightRight - 12;
        buttonClose.y = topTop;

        buttonPreviousPage.x = leftLeft + 22;
        buttonPreviousPage.y = bottomBottom;
        buttonPreviousChapter.x = leftLeft;
        buttonPreviousChapter.y = bottomBottom;

        buttonNextPage.x = rightRight - 16 - 18;
        buttonNextPage.y = bottomBottom;
        buttonNextChapter.x = rightRight - 16;
        buttonNextChapter.y = bottomBottom;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        double backgroundScale = book.getScalingFactor() / book.getBook().getFontSize();
        double bookHeight = BookRendering.DEFAULT_BOOK_HEIGHT * backgroundScale;

        renderBackground();

        background.draw(partialTicks, (int) bookHeight, (float) backgroundScale);

        itemRenderer.zLevel += 500;

        if (background.isFullyOpen())
        {
            book.drawCurrentPages();
        }

        super.render(mouseX, mouseY, partialTicks);

        if (background.isFullyOpen())
        {
            book.mouseHover(mouseX, mouseY);
        }

        itemRenderer.zLevel -= 500;
    }

    public void drawTooltip(ItemStack stack, int x, int y)
    {
        renderTooltip(stack, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouseButton)
    {
        if (book.mouseClicked(mouseButton))
            return true;

        if (mouseButton == 3)
        {
            book.navigateBack();
            return true;
        }

        return super.mouseClicked(x, y, mouseButton);
    }

    public FontRenderer getFontRenderer()
    {
        return this.font;
    }

    public ItemModelMesher getMesher()
    {
        return mesher;
    }

    public TextureManager getRenderEngine()
    {
        return renderEngine;
    }

    private static final int[] xPixel = {5, 5, 4, 4, 4, 4, 4, 29};
    private static final int[] yPixel = {2, 16, 30, 64, 79, 93, 107, 107};
    private static final int[] xSize = {17, 17, 18, 13, 21, 21, 15, 15};
    private static final int[] ySize = {11, 11, 11, 13, 11, 11, 15, 15};

    private void onPrevPageClicked(Button btn)
    {
        book.prevPage();
        updateButtonStates();
    }

    private void onNextPageClicked(Button btn)
    {
        book.nextPage();
        updateButtonStates();
    }

    private void onPrevChapterClicked(Button btn)
    {
        book.prevChapter();
        updateButtonStates();
    }

    private void onNextChapterClicked(Button btn)
    {
        book.nextChapter();
        updateButtonStates();
    }

    private void onCloseClicked(Button btn)
    {
        background.startClosing();
        updateButtonStates();
    }

    private void onBackClicked(Button btn)
    {
        book.navigateBack();
        updateButtonStates();
    }

    private void onHomeClicked(Button btn)
    {
        book.navigateHome();
        updateButtonStates();
    }

    class SpriteButton extends Button
    {
        private final int whichIcon;

        public SpriteButton(int x, int y, int iconIndex, IPressable press)
        {
            super(x, y, xSize[iconIndex], ySize[iconIndex], "", press);
            this.whichIcon = iconIndex;
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks)
        {
            if (this.visible)
            {
                boolean hover =
                        mouseX >= this.x &&
                                mouseY >= this.y &&
                                mouseX < this.x + this.width &&
                                mouseY < this.y + this.height;

                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                minecraft.getTextureManager().bindTexture(BOOK_GUI_TEXTURES);
                int x = xPixel[whichIcon];
                int y = yPixel[whichIcon];
                int w = xSize[whichIcon];
                int h = ySize[whichIcon];

                if (hover)
                {
                    x += 25;
                }

                this.blit(this.x, this.y, x, y, w, h);
            }
        }
    }
}
