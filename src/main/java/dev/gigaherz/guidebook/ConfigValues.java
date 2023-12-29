package dev.gigaherz.guidebook;

import com.google.common.collect.Lists;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ConfigValues
{
    public static final ServerConfig SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static
    {
        final Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static
    {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static int bookGUIScale = -1;
    public static boolean flexibleScale = false;
    public static boolean flipScrollDirection = false;
    public static boolean useNaturalArrows = false;
    public static String[] giveOnFirstJoin = new String[0];

    public static class ServerConfig
    {
        public final ModConfigSpec.ConfigValue<List<? extends String>> giveOnFirstJoin;

        ServerConfig(ModConfigSpec.Builder builder)
        {
            builder.push("general");
            giveOnFirstJoin = builder
                    .comment("List of books to give to the player when they join. Applied retroactively to existing players if a new book is added to the list.")
                    .translation("text.guidebook.config.give_on_first_join")
                    .defineList("give_on_first_join", Lists.newArrayList(), o -> o instanceof String);
            builder.pop();
        }
    }

    public static class ClientConfig
    {
        public final ModConfigSpec.IntValue bookGUIScale;
        public final ModConfigSpec.BooleanValue flexibleScale;
        public final ModConfigSpec.BooleanValue flipScrollDirection;
        public final ModConfigSpec.BooleanValue useNaturalArrows;

        ClientConfig(ModConfigSpec.Builder builder)
        {
            builder.comment("Options for customizing the display of tools on the player")
                    .push("display");
            bookGUIScale = builder
                    .comment("Use -1 for same as GUI scale, 0 for auto, 1+ for small/medium/large.")
                    .translation("text.guidebook.config.book_gui_scale")
                    .defineInRange("book_gui_scale", -1, -1, Integer.MAX_VALUE);
            flexibleScale = builder
                    .comment("Keep at false to use integral scaling, which makes the font pixels evently scaled. If set to true, the books will fill the screen space, even if the font becomes wonky.")
                    .translation("text.guidebook.config.flexible_scale")
                    .define("flexible_scale", false);
            flipScrollDirection = builder
                    .comment("If TRUE, flips the scroll direction to the opposite of what the system reports.")
                    .define("flip_scroll_direction", false);
            useNaturalArrows = builder
                    .comment("If TRUE, flips the buttons on the GUI to point how the page flips, and not what direction the book advances.")
                    .define("use_natural_arrows", false);
            builder.pop();
        }
    }

    public static void refreshClient()
    {
        bookGUIScale = CLIENT.bookGUIScale.get();
        flexibleScale = CLIENT.flexibleScale.get();
        flipScrollDirection = CLIENT.flipScrollDirection.get();
        useNaturalArrows = CLIENT.useNaturalArrows.get();
    }

    public static void refreshServer()
    {
        giveOnFirstJoin = SERVER.giveOnFirstJoin.get().toArray(new String[0]);
    }
}
