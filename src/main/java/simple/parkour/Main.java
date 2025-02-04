package simple.parkour;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static String color = "§b";

    @Override
    public void onEnable() {
        // Commands
        getCommand("in-world").setExecutor(new InWorldSystem());

        // Listeners
        PluginManager x = Bukkit.getPluginManager();
        x.registerEvents(new InWorldSystem(), this);

        // Start-Done Message
        getLogger().info("Enabled " + getPluginMeta().getName() + " v" + getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        // Stop-Done Message
        getLogger().info("Disabled " + getPluginMeta().getName() + " v" + getPluginMeta().getVersion());
    }
}
