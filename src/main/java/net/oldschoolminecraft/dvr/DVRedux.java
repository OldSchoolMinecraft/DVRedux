package net.oldschoolminecraft.dvr;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class DVRedux extends JavaPlugin
{
    private static final Logger log = Logger.getLogger("Minecraft");

    public VoteConfig config;
    private final Object lock = new Object();
    public Vote currentVote = null;
    private long lastVoteEndTime = 0L;
    private static long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes
    private static int VOTE_DURATION_TICKS = 20 * 60; // 60 seconds (adjust as desired)
    private static int YES_THRESHOLD_PERCENT_DAY = 40;
    private static int YES_THRESHOLD_PERCENT_RAIN = 60;
    public boolean shouldWeatherBeOn;
    private int voteTaskID;

    public void onEnable()
    {
        instance = this;

        config = new VoteConfig(new File(getDataFolder(), "config.yml"));

        COOLDOWN_MS = ((Integer) config.getConfigOption("cooldownSeconds")) * 1000L;
        VOTE_DURATION_TICKS = ((Integer) config.getConfigOption("voteDurationSeconds")) * 20;
        YES_THRESHOLD_PERCENT_DAY = (Integer) config.getConfigOption("yesVotePercentageRequired");
        YES_THRESHOLD_PERCENT_RAIN = (Integer) config.getConfigOption("yesRainVotePercentageRequired");
        
        getServer().getPluginManager().registerEvents(new WeatherHandler(), this);

        getCommand("dvadmin").setExecutor(new DVAdminCommand(this));

        System.out.println("DVRedux enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;
        String name = cmd.getName().toLowerCase();

        synchronized (lock)
        {
            if (name.equalsIgnoreCase("vote"))
            {
                if (args.length == 0)
                {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /vote <day|rain|yes|no>"));
                    return true;
                }

                String sub = args[0].toLowerCase();

                if (sub.equalsIgnoreCase("day") || sub.equalsIgnoreCase("rain"))
                {
                    if (currentVote != null)
                    {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cA vote is already in progress!"));
                        return true;
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastVoteEndTime < COOLDOWN_MS)
                    {
                        long remaining = (COOLDOWN_MS - (now - lastVoteEndTime)) / 1000;
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou must wait " + remaining + " seconds before starting a new vote."));
                        return true;
                    }

                    currentVote = new Vote(sub.equalsIgnoreCase("day") ? VoteType.DAY : VoteType.RAIN);
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', currentVote.getType() == VoteType.DAY ? config.getString("messages.started") : config.getString("messages.startedRain")));
                    currentVote.castVote(player.getName(), true); // Auto-cast vote for the initiator
                    voteTaskID = Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::endVote, VOTE_DURATION_TICKS);
                    return true;
                }

                if (sub.equalsIgnoreCase("yes") || sub.equalsIgnoreCase("no"))
                {
                    if (currentVote == null)
                    {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo vote is currently in progress."));
                        return true;
                    }

                    boolean yes = sub.startsWith("y");
                    boolean no = sub.startsWith("n");

                    if (!(yes || no))
                    {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /vote <day|rain|yes|no>"));
                        return true;
                    }

                    boolean added = currentVote.castVote(player.getName(), yes);

                    if (!added) player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYour vote has already been counted."));
                    else player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYour vote has been counted!"));

                    return true;
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /vote <day|rain|yes|no>"));
                return true;
            }
        }
        return false;
    }

    public void endVote()
    {
        synchronized (lock)
        {
            if (currentVote == null) return;

            int total = currentVote.getTotalVotes();
            int yes = currentVote.getYesVotes();
            int percent = total == 0 ? 0 : (yes * 100) / total;

            int threshold = 100;

            if (currentVote.getType() == VoteType.DAY) threshold = YES_THRESHOLD_PERCENT_DAY;
            else if (currentVote.getType() == VoteType.RAIN) threshold = YES_THRESHOLD_PERCENT_RAIN;

            if (percent >= threshold)
            {
                World world = Bukkit.getServer().getWorlds().get(0);
                if (currentVote.getType() == VoteType.DAY)
                {
                    world.setTime(0);
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.succeeded")));
                } else if (currentVote.getType() == VoteType.RAIN) {
                    shouldWeatherBeOn = true;
                    world.setStorm(true);
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.succeededRain")));
                }
            } else {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.failed")));
            }

            lastVoteEndTime = System.currentTimeMillis();
            currentVote = null;
            if (voteTaskID != 0) Bukkit.getScheduler().cancelTask(voteTaskID);
        }
    }

    public void onDisable()
    {
        System.out.println("DVRedux disabled");
    }

    private static DVRedux instance;
    public static DVRedux getInstance()
    {
        return instance;
    }
}
