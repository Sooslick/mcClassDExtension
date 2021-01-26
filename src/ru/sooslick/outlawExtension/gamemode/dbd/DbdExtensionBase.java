package ru.sooslick.outlawExtension.gamemode.dbd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.sooslick.outlaw.Engine;
import ru.sooslick.outlaw.gamemode.GameModeBase;
import ru.sooslick.outlaw.gamemode.GameModeConfig;
import ru.sooslick.outlaw.util.CommonUtil;
import ru.sooslick.outlaw.util.WorldUtil;
import ru.sooslick.outlawExtension.ClassDExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DbdExtensionBase implements GameModeBase {
    private static final DbdExtensionConfig dbdConfig = new DbdExtensionConfig();
    private static final int COOLDOWN = 10;

    static DbdExtensionBase instance;

    private final DbdExtensionListener events;

    Map<Block, Shulker> targetsMap;
    private int cooldown;
    private int glowTimer;

    Score score;

    public DbdExtensionBase() {
        instance = this;
        targetsMap = Collections.emptyMap();
        events = new DbdExtensionListener();
        Bukkit.getPluginManager().registerEvents(events, ClassDExtension.getInstance());
    }

    @Override
    public void onIdle() {
        targetsMap.forEach((k, v) -> v.remove());
    }

    @Override
    public void onPreStart() {
        targetsMap.forEach((k, v) -> k.setType(Material.AIR));
        WorldBorder wb = Bukkit.getWorlds().get(0).getWorldBorder();
        wb.setCenter(0, 0);
        wb.setSize(dbdConfig.playArea);
    }

    @Override
    public void onGame() {
        targetsMap = new HashMap<>();
        Random random = CommonUtil.random;
        int bound = (int) (dbdConfig.playArea / 2.1);
        for (int i = 0; i < dbdConfig.totalBlocks; i++) {
            Location current = WorldUtil.getRandomLocation(bound);
            double gauss = random.nextGaussian()*42 + 63;
            if (gauss > 5 && gauss < current.getY())
                current.setY(gauss);
            Block b = current.getBlock();
            b.setType(Material.OBSIDIAN);
            Shulker e = (Shulker) current.getWorld().spawnEntity(current, EntityType.SHULKER);
            e.setAI(false);
            e.setInvulnerable(true);
            e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, COOLDOWN *20+10, 1));
            glowTimer = 10;
            targetsMap.put(b, e);
        }

        Engine engine = Engine.getInstance();
        Player outlaw = engine.getOutlaw().getPlayer();

        //configure sb
        Scoreboard sb = engine.getScoreboardHolder().getScoreboard();
        Team t = sb.registerNewTeam("blocks");
        t.setColor(ChatColor.AQUA);
        targetsMap.values().forEach(e -> t.addEntry(e.getUniqueId().toString()));
        score = sb.registerNewObjective("Defense", "dummy", "Defense").getScore(outlaw.getName());
        score.setScore(dbdConfig.requiredBlocks);
        score.getObjective().setDisplaySlot(DisplaySlot.PLAYER_LIST);

        outlaw.getInventory().addItem(new ItemStack(Material.COMPASS));
        cooldown = COOLDOWN;
    }

    @Override
    public void tick() {
        glowImpl();
        compassImpl();
    }

    @Override
    public void unload() {
        targetsMap.forEach((k, v) -> {
            v.remove();
            k.setType(Material.AIR);
        });
        HandlerList.unregisterAll(events);
    }

    @Override
    public GameModeConfig getConfig() {
        return dbdConfig;
    }

    @Override
    public String getObjective() {
        return String.format("BREAK %s / %s SPECIAL BLOCKS", dbdConfig.requiredBlocks, dbdConfig.totalBlocks);
    }

    @Override
    public String getName() {
        return "Checkpoint Defense";
    }

    @Override
    public String getDescription() {
        return "§6Obsidian Defense\n§eIn this game mode the Victim has to destroy a certain amount of special blocks that randomly spawn on the map.";
    }

    private void glowImpl() {
        if (--glowTimer <= 0) {
            glowTimer = COOLDOWN;
            targetsMap.values().forEach(e -> e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, COOLDOWN *20+10, 1)));
        }
    };

    private void compassImpl() {
        if (--cooldown <= 0) {
            cooldown = COOLDOWN;
            Player o = Engine.getInstance().getOutlaw().getPlayer();
            Location oLoc = o.getLocation();
            Location nearestBlock = null;
            double mind = dbdConfig.playArea*1.5;
            for (Block b : targetsMap.keySet()) {
                Location l = b.getLocation();
                double d = WorldUtil.distance2d(l, oLoc);
                if (d < mind) {
                    mind = d;
                    nearestBlock = l;
                }
            }
            if (nearestBlock != null)
                o.setCompassTarget(nearestBlock);
        }
    };
}
