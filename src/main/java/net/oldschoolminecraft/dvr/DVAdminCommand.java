package net.oldschoolminecraft.dvr;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DVAdminCommand implements CommandExecutor
{
    private DVRedux plugin;

    public DVAdminCommand(DVRedux plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender.hasPermission("dvr.admin") || sender.isOp()))
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have permission to use this command."));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub)
        {
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /dvadmin <end|status|reload>"));
                return true;
            case "end":
                plugin.endVote();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cVote forcibly ended."));
                return true;
            case "status":
                Vote currentVote = plugin.currentVote;
                if (currentVote == null)
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eNo vote is currently in progress."));
                else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eCurrent vote type: &b" + currentVote.getType()));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eYes votes: &a" + currentVote.getYesVotes()));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eNo votes: &c" + currentVote.getNoVotes()));
                }
                return true;
            case "reload":
                plugin.config.reload();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aConfiguration reloaded."));
                return true;
        }
    }
}
