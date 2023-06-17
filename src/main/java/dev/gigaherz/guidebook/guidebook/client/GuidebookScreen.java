package dev.gigaherz.guidebook.guidebook.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.guidebook.ConfigValues;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class GuidebookScreen extends Screen
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

    private ItemModelShaper mesher = Minecraft.getInstance().getItemRenderer().getItemModelShaper();
    private TextureManager renderEngine = Minecraft.getInstance().textureManager;

    private BookRendering rendering;
    private IAnimatedBookBackground background;

    public GuidebookScreen(ResourceLocation rendering)
    {
        super(Component.translatable("text.gbook.book.title"));
        bookLocation = rendering;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void init()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        ConditionContext conditionContext = new ConditionContext();
        conditionContext.setPlayer(player);

        if (rendering == null)
        {
            BookDocument theBook = BookRegistry.get(bookLocation);
            rendering = (BookRendering) theBook.getRendering();

            boolean conditions = theBook.reevaluateConditions(conditionContext);
            if (rendering == null)
            {
                rendering = new BookRendering(theBook, this);
                theBook.setRendering(rendering);
            }
            else
            {
                rendering.setGui(this);
                if (conditions || rendering.refreshScalingFactor())
                {
                    rendering.resetRendering(conditions);
                }
            }

            background = rendering.createBackground(this);
        }

        // Positions set below in repositionButtons();
        this.addRenderableWidget(this.buttonHome = new SpriteButton(0, 0, 6, Component.literal("Home"), this::onHomeClicked));
        this.addRenderableWidget(this.buttonBack = new SpriteButton(0, 0, 2, Component.literal("Back"),this::onBackClicked));
        this.addRenderableWidget(this.buttonClose = new SpriteButton(0, 0, 3, Component.literal("Close"),this::onCloseClicked));
        if (ConfigValues.useNaturalArrows)
        {
            this.addRenderableWidget(this.buttonPreviousPage = new SpriteButton(0, 0, 1, Component.literal("Previous Page"),this::onPrevPageClicked));
            this.addRenderableWidget(this.buttonNextPage = new SpriteButton(0, 0, 0, Component.literal("Next Page"),this::onNextPageClicked));
            this.addRenderableWidget(this.buttonPreviousChapter = new SpriteButton(0, 0, 5, Component.literal("Previous Chapter"),this::onPrevChapterClicked));
            this.addRenderableWidget(this.buttonNextChapter = new SpriteButton(0, 0, 4, Component.literal("Next Chapter"),this::onNextChapterClicked));
        }
        else
        {
            this.addRenderableWidget(this.buttonPreviousPage = new SpriteButton(0, 0, 0, Component.literal("Previous Page"),this::onPrevPageClicked));
            this.addRenderableWidget(this.buttonNextPage = new SpriteButton(0, 0, 1, Component.literal("Next Page"),this::onNextPageClicked));
            this.addRenderableWidget(this.buttonPreviousChapter = new SpriteButton(0, 0, 4, Component.literal("Previous Chapter"), this::onPrevChapterClicked));
            this.addRenderableWidget(this.buttonNextChapter = new SpriteButton(0, 0, 5, Component.literal("Next Chapter"),this::onNextChapterClicked));
        }

        updateButtonStates();

        repositionButtons();
    }

    private void setupConditionsAndPosition()
    {
        this.width = minecraft.getWindow().getGuiScaledWidth();
        this.height = minecraft.getWindow().getGuiScaledHeight();
        if (rendering.refreshScalingFactor())
        {
            rendering.resetRendering(false);
        }
    }

    private void updateButtonStates()
    {
        buttonClose.visible = background.isFullyOpen();
        buttonHome.visible = background.isFullyOpen() && rendering.getBook().home != null;
        buttonBack.visible = background.isFullyOpen() && rendering.canGoBack();
        buttonNextPage.visible = background.isFullyOpen() && rendering.canGoNextPage();
        buttonPreviousPage.visible = background.isFullyOpen() && rendering.canGoPrevPage();
        buttonNextChapter.visible = background.isFullyOpen() && rendering.canGoNextChapter();
        buttonPreviousChapter.visible = background.isFullyOpen() && rendering.canGoPrevChapter();
    }

    @Override
    public void tick()
    {
        super.tick();

        if (background.update())
            minecraft.setScreen(null);

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
            rendering.navigateBack();
            return true;
        }

        return super.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
    }

    private double deltaAcc = 0;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (super.mouseScrolled(mouseX, mouseY, delta))
            return true;

        deltaAcc += delta * (ConfigValues.flipScrollDirection ? -1 : 1);
        while (deltaAcc >= 1.0)
        {
            deltaAcc -= 1.0;
            if (rendering.canGoPrevPage()) rendering.prevPage();
        }
        while (deltaAcc <= -1.0)
        {
            deltaAcc += 1.0;
            if (rendering.canGoNextPage()) rendering.nextPage();
        }
        return true;
    }

    private void repositionButtons()
    {
        setupConditionsAndPosition();

        double bookScale = rendering.getScalingFactor() / rendering.getBook().getFontSize();
        double bookWidth = (BookRendering.DEFAULT_BOOK_WIDTH) * bookScale;
        double bookHeight = (BookRendering.DEFAULT_BOOK_HEIGHT) * bookScale;

        int left = (int) ((this.width - bookWidth) / 2);
        int right = (int) (left + bookWidth);
        int top = (int) ((this.height - bookHeight) / 2);
        int bottom = (int) (top + bookHeight);

        int leftLeft = left + 4;
        int rightRight = right - 4;
        int topTop = top - 16 + (int) (8 * bookScale);
        int bottomBottom = bottom - 4;

        buttonHome.setX(leftLeft);
        buttonHome.setY(topTop);
        buttonBack.setX(leftLeft + 18);
        buttonBack.setY(topTop + 3);

        buttonClose.setX(rightRight - 16);
        buttonClose.setY(topTop);

        buttonPreviousPage.setX(leftLeft + 22);
        buttonPreviousPage.setY(bottomBottom);
        buttonPreviousChapter.setX(leftLeft);
        buttonPreviousChapter.setY(bottomBottom);

        buttonNextPage.setX(rightRight - 16 - 18 - 4);
        buttonNextPage.setY(bottomBottom);
        buttonNextChapter.setX(rightRight - 16 - 4);
        buttonNextChapter.setY(bottomBottom);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        double backgroundScale = rendering.getScalingFactor() / rendering.getBook().getFontSize();
        double bookHeight = BookRendering.DEFAULT_BOOK_HEIGHT * backgroundScale;

        renderBackground(matrixStack);

        background.draw(matrixStack, partialTicks, (int) bookHeight, (float) backgroundScale);

        if (background.isFullyOpen())
        {
            rendering.drawCurrentPages(matrixStack);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (background.isFullyOpen())
        {
            rendering.mouseHover(matrixStack, mouseX, mouseY);
        }
    }

    public void drawTooltip(PoseStack matrixStack, ItemStack stack, int x, int y)
    {
        renderTooltip(matrixStack, stack, x, y);
    }

    public void drawTooltip(PoseStack matrixStack, Component text, int x, int y)
    {
        renderTooltip(matrixStack, text, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouseButton)
    {
        if (rendering.mouseClicked((int) x, (int) y, mouseButton))
            return true;

        if (mouseButton == 3)
        {
            rendering.navigateBack();
            return true;
        }

        return super.mouseClicked(x, y, mouseButton);
    }

    public Font getFontRenderer()
    {
        return this.font;
    }

    public ItemModelShaper getMesher()
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
        rendering.prevPage();
        updateButtonStates();
    }

    private void onNextPageClicked(Button btn)
    {
        rendering.nextPage();
        updateButtonStates();
    }

    private void onPrevChapterClicked(Button btn)
    {
        rendering.prevChapter();
        updateButtonStates();
    }

    private void onNextChapterClicked(Button btn)
    {
        rendering.nextChapter();
        updateButtonStates();
    }

    private void onCloseClicked(Button btn)
    {
        background.startClosing();
        updateButtonStates();
    }

    private void onBackClicked(Button btn)
    {
        rendering.navigateBack();
        updateButtonStates();
    }

    private void onHomeClicked(Button btn)
    {
        rendering.navigateHome();
        updateButtonStates();
    }

    class SpriteButton extends Button
    {
        private final int whichIcon;

        public SpriteButton(int x, int y, int iconIndex, Component narratorText, OnPress press)
        {
            super(x, y, xSize[iconIndex], ySize[iconIndex], narratorText, press, DEFAULT_NARRATION);
            this.whichIcon = iconIndex;
        }

        @Override
        public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            boolean hover = mouseX >= this.getX() &&
                    mouseY >= this.getY() &&
                    mouseX < this.getX() + this.width &&
                    mouseY < this.getY() + this.height;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, BOOK_GUI_TEXTURES);

            int x = xPixel[whichIcon];
            int y = yPixel[whichIcon];
            int w = xSize[whichIcon];
            int h = ySize[whichIcon];

            if (hover)
            {
                x += 25;
            }

            this.blit(matrixStack, this.getX(), this.getY(), x, y, w, h);
        }
    }
}
