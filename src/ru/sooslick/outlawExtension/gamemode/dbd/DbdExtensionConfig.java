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
    public int playArea, totalBlocks, requiredBlocks;
    boolean firstRead = true;

    @Override
    public void readConfig() {
        FileConfiguration cfg = ClassDExtension.getInstance().getConfig();

        int oldPlayArea = playArea;
        int oldTotalBlocks = totalBlocks;
        int oldRequiredBlocks = requiredBlocks;

        //read
        playArea = cfg.getInt("playArea", 1000);
        totalBlocks = cfg.getInt("totalBlocks", 7);
        requiredBlocks = cfg.getInt("requiredBlocks", 5);

        //validate
        int spawnArea = Cfg.spawnDistance + Cfg.spawnRadius;
        if (playArea < spawnArea) playArea = spawnArea + 15;
        if (totalBlocks <= 0) totalBlocks = 1;
        if (requiredBlocks > totalBlocks) requiredBlocks = totalBlocks;

        //changes detection
        if (!firstRead) {
            if (oldPlayArea != playArea) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "playArea", playArea));
            if (oldTotalBlocks != totalBlocks) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "totalBlocks", totalBlocks));
            if (oldRequiredBlocks != requiredBlocks) Bukkit.broadcastMessage(String.format(Messages.CONFIG_MODIFIED, "requiredBlocks", requiredBlocks));
        }
        firstRead = false;
    }

    @Override
    public String getValueOf(String field) {
        switch (field.toLowerCase()) {
            case "playarea": return String.valueOf(playArea);
            case "totalblocks": return String.valueOf(totalBlocks);
            case "requiredblocks": return String.valueOf(requiredBlocks);
            default: return null;
        }
    }

    @Override
    public List<String> availableParameters() {
        return Arrays.asList("playArea", "totalBlocks", "requiredBlocks");
    }
}
