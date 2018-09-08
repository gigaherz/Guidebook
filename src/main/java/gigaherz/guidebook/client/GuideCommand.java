package gigaherz.guidebook.client;

import gigaherz.guidebook.guidebook.client.BookRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.IClientCommand;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuideCommand
    extends CommandBase
    implements IClientCommand
{
    private final List<String> subCommands = Arrays.asList(
        "reload"
    );

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message)
    {
        return false;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if(args.length == 1)
        {
            return subCommands.stream().filter(c -> c.startsWith(args[0])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getName()
    {
        return "gbook";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return getName() + " [" + String.join(" | ", subCommands) + "]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if(args.length < 1 || !subCommands.contains(args[0]))
        {
            sender.sendMessage(new TextComponentString(getUsage(sender)));
            return;
        }

        String[] skipFirst = Arrays.stream(args).skip(1).toArray(String[]::new);

        switch(args[0])
        {
            case "reload":
                executeReload(server, sender, skipFirst);
                break;
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    private void executeReload(MinecraftServer server, ICommandSender sender, String[] args)
    {
        BookRegistry.parseAllBooks(Minecraft.getMinecraft().getResourceManager());
        sender.sendMessage(new TextComponentTranslation("cmd.gbook.guide.done"));
    }
}
