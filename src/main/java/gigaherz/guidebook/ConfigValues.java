package gigaherz.guidebook;

import com.google.common.collect.Lists;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Config(modid=GuidebookMod.MODID)
public class ConfigValues
{
    @Config.Comment("Use -1 for same as GUI scale, 0 for auto, 1+ for small/medium/large")
    @Config.RangeInt(min=-1, max=10)
    public static int bookGUIScale = -1;

    @Config.Comment("Keep at false to use integral scaling, which makes the font pixels evently scaled. If set to true, the books will fill the screen space, even if the font becomes wonky.")
    public static boolean flexibleScale = false;

    @Config.Comment("List of books to give to the player when they join. Applied retroactively to existing players if a new book is added to the list.")
    public static String[] giveOnFirstJoin = new String[0];

    @Mod.EventBusSubscriber(modid = GuidebookMod.MODID)
    private static class EventHandler
    {
        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(GuidebookMod.MODID))
            {
                ConfigManager.sync(GuidebookMod.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
