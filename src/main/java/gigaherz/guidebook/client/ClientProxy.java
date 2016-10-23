package gigaherz.guidebook.client;

import gigaherz.common.client.ModelHandle;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.common.IModProxy;
import gigaherz.guidebook.guidebook.GuiGuidebook;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static gigaherz.common.client.ModelHelpers.registerItemModel;

@Mod.EventBusSubscriber
public class ClientProxy implements IModProxy
{
    @Override
    public void displayBook(String book)
    {
        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        Minecraft.getMinecraft().displayGuiScreen(new GuiGuidebook(book));
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        OBJLoader.INSTANCE.addDomain(GuidebookMod.MODID);
        ModelHandle.init();

        registerItemModel(GuidebookMod.guidebook);
    }
}
