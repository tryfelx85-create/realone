package com.arena.spawn;

import com.arena.anticheat.CPSCheck;
import com.arena.anticheat.FastBreakCheck;
import com.arena.anticheat.HotbarSwapCheck;
import com.arena.anticheat.ReachCheck;
import com.arena.anticheat.ScaffoldCheck;
import com.arena.anticheat.SpeedCheck;
import com.arena.anticheat.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ArenaPlugin has been enabled!");

        getCommand("spectate").setExecutor(new SpectateCommand());
        getCommand("stopspectating").setExecutor(new StopSpectatingCommand());

        // FIX: StartButtonListener now needs the plugin instance to start the vote timer
        getServer().getPluginManager().registerEvents(new StartButtonListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPRestrictionListener(), this);
        getServer().getPluginManager().registerEvents(new VoteListener(), this);
        getServer().getPluginManager().registerEvents(new MatchEndListener(), this); // FIX: new — closes match-end gap

        // Anti-cheat checks
        ViolationManager.init(getLogger());
        getServer().getPluginManager().registerEvents(new ReachCheck(), this);
        getServer().getPluginManager().registerEvents(new CPSCheck(), this);
        getServer().getPluginManager().registerEvents(new FastBreakCheck(), this);
        getServer().getPluginManager().registerEvents(new ScaffoldCheck(), this);
        getServer().getPluginManager().registerEvents(new SpeedCheck(), this);
        getServer().getPluginManager().registerEvents(new HotbarSwapCheck(), this);

        startDecayTask();
    }

    /** Periodically decays violation scores so old flags don't accumulate forever. */
    private void startDecayTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ViolationManager.CheckType type : ViolationManager.CheckType.values()) {
                    ViolationManager.decay(player, type, 0.5);
                }
            }
        }, 20L * 10, 20L * 10); // every 10 seconds
    }

    @Override
    public void onDisable() {
        getLogger().info("ArenaPlugin has been disabled!");
    }
}
