package ru.sooslick.outlawExtension;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.sooslick.outlaw.Engine;

import java.io.IOException;
import java.io.InputStreamReader;

public class ClassDExtension extends JavaPlugin {

    private static ClassDExtension instance;
    private static int jobId;
    private static int retries = 5;

    public static ClassDExtension getInstance() {
        return instance;
    }

    private static final Runnable job = () -> {
        try {
            Engine e = Engine.getInstance();
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(new InputStreamReader(e.getResource("plugin.yml")));
            String v = yaml.getString("version", "unknown");
            instance.getLogger().info("§1Hooked the ClassD plugin of version " + v);
            Bukkit.getScheduler().cancelTask(jobId);
        } catch (NoClassDefFoundError e) {
            instance.getLogger().warning("§4Cannot hook ClassD plugin: not loaded");
            retryOrDisable();
        } catch (NullPointerException | IOException | InvalidConfigurationException e) {
            instance.getLogger().warning("§4Cannot hook ClassD plugin: it seems to be corrupted");
            retryOrDisable();
        }
    };

    private static void retryOrDisable() {
        if (--retries <= 0) {
            Bukkit.getPluginManager().disablePlugin(instance);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        jobId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, job, 2, 200);
        getLogger().info("§1ClassD Extension enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("§4ClassD Extension disabled.");
    }
}
