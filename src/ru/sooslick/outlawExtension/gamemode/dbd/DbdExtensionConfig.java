package ru.sooslick.outlawExtension.gamemode.dbd;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import ru.sooslick.outlaw.Cfg;
import ru.sooslick.outlaw.Messages;
import ru.sooslick.outlaw.gamemode.GameModeConfig;
import ru.sooslick.outlawExtension.ClassDExtension;

import java.util.Arrays;
import java.util.List;

public class DbdExtensionConfig implements GameModeConfig {
    public int playArea, totalOverworldBlocks, totalNetherBlocks, requiredBlocks;
    boolean firstRead = true;

    @Override
    public void readConfig() {
        ClassDExtension plugin = ClassDExtension.getInstance();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        int oldPlayArea = playArea;
        int oldTotalOverworldBlocks = totalOverworldBlocks;
        int oldTotalNetherBlocks = totalNetherBlocks;
        int oldRequiredBlocks = requiredBlocks;

        //read
        playArea = cfg.getInt("playArea", 1000);
        totalOverworldBlocks = cfg.getInt("totalOverworldBlocks", 4);
        totalNetherBlocks = cfg.getInt("totalNetherBlocks", 3);
        requiredBlocks = cfg.getInt("requiredBlocks", 5);

        //validate
        int spawnArea = (Cfg.spawnDistance + Cfg.spawnRadius) * 2;
        if (playArea < spawnArea) playArea = spawnArea + 15;
        if (totalOverworldBlocks < 0) totalOverworldBlocks = 0;
        if (totalNetherBlocks < 0) totalNetherBlocks = 0;
        if (totalOverworldBlocks + totalNetherBlocks == 0) totalOverworldBlocks = 1;
        if (requiredBlocks > totalOverworldBlocks + totalNetherBlocks) requiredBlocks = totalOverworldBlocks + totalNetherBlocks;

        //changes detection
        if (!firstRead) {
            if (oldPlayArea != playArea) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "playArea", playArea));
            if (oldTotalOverworldBlocks != totalOverworldBlocks) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "totalOverworldBlocks", totalOverworldBlocks));
            if (oldTotalNetherBlocks != totalNetherBlocks) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "totalNetherBlocks", totalNetherBlocks));
            if (oldRequiredBlocks != requiredBlocks) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "requiredBlocks", requiredBlocks));
        }
        firstRead = false;
    }

    @Override
    public String getValueOf(String field) {
        switch (field.toLowerCase()) {
            case "playarea": return String.valueOf(playArea);
            case "totaloverworldblocks": return String.valueOf(totalOverworldBlocks);
            case "totalnetherblocks": return String.valueOf(totalNetherBlocks);
            case "requiredblocks": return String.valueOf(requiredBlocks);
            default: return null;
        }
    }

    @Override
    public List<String> availableParameters() {
        return Arrays.asList("playArea", "totalOverworldBlocks", "totalNetherBlocks", "requiredBlocks");
    }
}
