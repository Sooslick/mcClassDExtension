package ru.sooslick.outlawExtension.gamemode.dbd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.sooslick.outlaw.Engine;
import ru.sooslick.outlaw.Messages;
import ru.sooslick.outlaw.gamemode.GameModeBase;
import ru.sooslick.outlaw.gamemode.GameModeConfig;
import ru.sooslick.outlaw.util.CommonUtil;
import ru.sooslick.outlaw.util.LoggerUtil;
import ru.sooslick.outlaw.util.WorldUtil;
import ru.sooslick.outlawExtension.ClassDExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Bukkit.broadcastMessage(Messages.READY_FOR_GAME);
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
        // check nether
        int overworldCount = dbdConfig.totalOverworldBlocks;
        int netherCount = dbdConfig.totalNetherBlocks;
        World nether = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NETHER).findFirst().orElse(null);
        if (nether == null) {
            ClassDExtension.getInstance().getLogger().warning("Cannot find Nether, all special blocks will be spawned in the Overworld");
            overworldCount += netherCount;
            netherCount = 0;
        }
        // overworld blocks
        for (int i = 0; i < overworldCount; i++) {
            Location current = WorldUtil.getRandomLocation(bound);
            double gauss = random.nextGaussian() * 42 + 63;
            if (gauss > 5 && gauss < current.getY())
                current.setY(gauss);
            placeBlock(current);
        }
        // nether blocks
        for (int i = 0; i < netherCount; i++) {
            int dbound = bound * 2;
            int x = CommonUtil.random.nextInt(dbound) - bound;
            int z = CommonUtil.random.nextInt(dbound) - bound;
            int y = CommonUtil.random.nextInt(116) + 8;
            placeBlock(new Location(nether, x, y, z));
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
        forceCompassUpdate();
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
        return String.format("BREAK %s / %s SPECIAL BLOCKS", dbdConfig.requiredBlocks, dbdConfig.totalOverworldBlocks + dbdConfig.totalNetherBlocks);
    }

    @Override
    public String getName() {
        return "Checkpoint Defense";
    }

    @Override
    public String getDescription() {
        return "§6Obsidian Defense\n§eIn this game mode the Victim has to destroy a certain amount of special blocks that randomly spawn on the map.";
    }

    public void forceCompassUpdate() {
        cooldown = 0;
        compassImpl();
    }

    private void glowImpl() {
        if (--glowTimer <= 0) {
            glowTimer = COOLDOWN;
            // since entities in map have weak references, find shulkers
            List<Entity> foundEntities = new LinkedList<>();
            targetsMap.keySet().stream().map(b -> b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1)).forEach(foundEntities::addAll);
            List<UUID> uuids = targetsMap.values().stream().map(Entity::getUniqueId).collect(Collectors.toList());
            foundEntities.stream()
                    .filter(e -> uuids.contains(e.getUniqueId()))
                    .map(e -> (LivingEntity) e)
                    .forEach(e -> e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, COOLDOWN * 20 + 10, 1)));
        }
    }

    private void compassImpl() {
        if (--cooldown <= 0) {
            cooldown = COOLDOWN;
            Player o = Engine.getInstance().getOutlaw().getPlayer();
            World oWorld = o.getWorld();
            World otherWorld = null;
            Location oLoc = o.getLocation();
            Location nearestBlock = null;
            double mind = dbdConfig.playArea * 1.5;
            // detect nearest block
            for (Block b : targetsMap.keySet()) {
                Location l = b.getLocation();
                double d = WorldUtil.distance2d(l, oLoc);
                if (d < mind) {
                    mind = d;
                    nearestBlock = l;
                } else if (d > 99999) {
                    otherWorld = l.getWorld();
                }
            }
            // find compass
            Inventory inv = o.getInventory();
            ItemStack is = null;
            for (ItemStack current : inv.getContents()) {
                //ItemStack current CAN be null
                if (current != null && current.getType() == Material.COMPASS) {
                    is = current;
                    break;
                }
            }
            CompassMeta meta = null;
            if (is != null && is.getItemMeta() instanceof CompassMeta)
                meta = (CompassMeta) is.getItemMeta();
            // update compass
            if (nearestBlock != null) {
                o.setCompassTarget(nearestBlock);
                if (oWorld.getEnvironment() == World.Environment.NETHER && meta != null) {
                    meta.setLodestoneTracked(false);
                    meta.setLodestone(nearestBlock);
                }
            }
            // nearest tracked block in other world!
            else if (otherWorld != null) {
                if (meta != null) {
                    meta.setLodestoneTracked(false);
                    if (oWorld.getEnvironment() == World.Environment.NORMAL) {
                        //tracked position on nether
                        Location trackedLocation = new Location(otherWorld, 0, 0, 0);
                        meta.setLodestone(trackedLocation);
                    } else {
                        // tracked position in overworld
                        meta.setLodestone(null);
                    }
                }
            }
            if (is != null)
                is.setItemMeta(meta);
        }
    }

    private void placeBlock(Location selected) {
        Block b = selected.getBlock();
        b.setType(Material.OBSIDIAN);
        Shulker e = (Shulker) selected.getWorld().spawnEntity(selected, EntityType.SHULKER);
        e.setAI(false);
        e.setInvulnerable(true);
        e.setCustomName("Special block");
        e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, COOLDOWN * 20 + 10, 1));
        glowTimer = 10;
        targetsMap.put(b, e);
        LoggerUtil.debug("Spawned special block at " + b.getLocation());
    }
}
