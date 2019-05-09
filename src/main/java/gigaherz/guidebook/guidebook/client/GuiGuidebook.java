package gigaherz.guidebook.guidebook.client;

import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.conditions.ConditionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.glfw.GLFW;

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

    private ItemModelMesher mesher = Minecraft.getInstance().getItemRenderer().getItemModelMesher();
    private TextureManager renderEngine = Minecraft.getInstance().textureManager;

    private BookRendering book;
    private IAnimatedBookBackground background;
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

            EntityPlayerSP player = Minecraft.getInstance().player;
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

            int btnId = 0;

            int left = (this.width - BookRendering.DEFAULT_BOOK_WIDTH) / 2;
            int right = left + BookRendering.DEFAULT_BOOK_WIDTH;
            int top = (this.height - BookRendering.DEFAULT_BOOK_HEIGHT) / 2 - 9;
            int bottom = top + BookRendering.DEFAULT_BOOK_HEIGHT;
            this.addButton(this.buttonHome = new SpriteButton(btnId++, left - 10, top - 8, 6)
            {
                @Override
                public void onClick(double mouseX, double mouseY)
                {
                    super.onClick(mouseX, mouseY);
                    book.navigateHome();
                    updateButtonStates();
                }
            });
            this.addButton(this.buttonBack = new SpriteButton(btnId++, left + 8, top - 5, 2)
            {
                @Override
                public void onClick(double mouseX, double mouseY)
                {
                    super.onClick(mouseX, mouseY);
                    book.navigateBack();
                    updateButtonStates();
                }
            });
            this.addButton(this.buttonClose = new SpriteButton(btnId++, right - 6, top - 6, 3)
            {
                @Override
                public void onClick(double mouseX, double mouseY)
                {
                    super.onClick(mouseX, mouseY);
                    background.startClosing();
                    updateButtonStates();
                }
            });
            if (useNaturalArrows)
            {
                this.addButton(this.buttonPreviousPage = new SpriteButton(btnId++, left + 24, bottom - 13, 1)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.prevPage();
                        updateButtonStates();
                    }
                });
                this.addButton(this.buttonNextPage = new SpriteButton(btnId++, right - 42, bottom - 13, 0)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.nextPage();
                        updateButtonStates();
                    }
                });
                this.buttonPreviousChapter = new SpriteButton(btnId++, left + 2, bottom - 13, 5)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.prevChapter();
                        updateButtonStates();
                    }
                };
                this.addButton(this.buttonNextChapter = new SpriteButton(btnId++, right - 23, bottom - 13, 4)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.nextChapter();
                        updateButtonStates();
                    }
                });
            }
            else
            {
                this.addButton(this.buttonPreviousPage = new SpriteButton(btnId++, left + 24, bottom - 13, 0)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.prevPage();
                        updateButtonStates();
                    }
                });
                this.addButton(this.buttonNextPage = new SpriteButton(btnId++, right - 42, bottom - 13, 1)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.nextPage();
                        updateButtonStates();
                    }
                });
                this.addButton(this.buttonPreviousChapter = new SpriteButton(btnId++, left + 2, bottom - 13, 4)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.prevChapter();
                        updateButtonStates();
                    }
                });
                this.addButton(this.buttonNextChapter = new SpriteButton(btnId++, right - 23, bottom - 13, 5)
                {
                    @Override
                    public void onClick(double mouseX, double mouseY)
                    {
                        super.onClick(mouseX, mouseY);
                        book.nextChapter();
                        updateButtonStates();
                    }
                });
            }
            GuidebookMod.logger.info("Showing gui with " + btnId + " buttons.");
        }

        updateButtonStates();

        repositionButtons();
    }

    private void setupConditionsAndPosition()
    {
        this.width = this.mc.mainWindow.getScaledWidth();
        this.height = this.mc.mainWindow.getScaledHeight();
        if(book.refreshScalingFactor())
        {
            book.resetRendering(false);
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
    public void tick()
    {
        super.tick();

        if (background.update())
            mc.displayGuiScreen(null);

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
        int topTop = top - 16 + (int)(8*bookScale);
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

        background.draw(partialTicks, (int) bookHeight, (float)backgroundScale);

        if (background.isFullyOpen())
        {
            book.drawCurrentPages();
        }

        super.render(mouseX, mouseY, partialTicks);

        if (background.isFullyOpen())
        {
            book.mouseHover(mouseX, mouseY);
        }

        /*
        ScaledResolution sr = new ScaledResolution(mc);
        float mcScale = sr.getScaleFactor();
        double bookScale = book.getScalingFactor();

        drawString(fontRenderer, String.format("Gui scale: %f, Book scale: %f, Total: %f, background scale: %f",
                mcScale, bookScale, bookScale * mcScale,
                backgroundScale), 5, 5, 0xFFFFFFFF);
        */
    }

    public void drawTooltip(ItemStack stack, int x, int y)
    {
        renderToolTip(stack, x, y);
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

    private static final int[] xPixel = {5, 5, 4, 4, 4, 4, 4, 29};
    private static final int[] yPixel = {2, 16, 30, 64, 79, 93, 107, 107};
    private static final int[] xSize = {17, 17, 18, 13, 21, 21, 15, 15};
    private static final int[] ySize = {11, 11, 11, 13, 11, 11, 15, 15};

    class SpriteButton extends GuiButton
    {
        private final int whichIcon;

        public SpriteButton(int buttonId, int x, int y, int iconIndex)
        {
            super(buttonId, x, y, xSize[iconIndex], ySize[iconIndex], "");
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
