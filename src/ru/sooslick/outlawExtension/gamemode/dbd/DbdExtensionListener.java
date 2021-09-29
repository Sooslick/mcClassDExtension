package ru.sooslick.outlawExtension.gamemode.dbd;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import ru.sooslick.outlaw.Engine;
import ru.sooslick.outlaw.GameState;

public class DbdExtensionListener implements Listener {
    private static final Engine engine;
    private static final DbdExtensionBase base;

    static {
        engine = Engine.getInstance();
        base = DbdExtensionBase.instance;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled() || engine.getGameState() != GameState.GAME)
            return;
        for (Block b : base.targetsMap.keySet()) {
            if (b.equals(e.getBlock())) {
                int newScore = base.score.getScore() - 1;
                base.score.setScore(newScore);
                if (newScore <= 0)
                    engine.triggerEndgame(true);
                else
                    Bukkit.broadcastMessage("§cVictim just destroyed one of the targets! §6" + newScore + " blocks left");
                //find entity by weak reference
                LivingEntity rmEnt = base.targetsMap.get(b);
                LivingEntity detectedEntity = (LivingEntity) b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1).stream()
                        .filter(ent -> ent.getUniqueId().equals(rmEnt.getUniqueId()))
                        .findFirst()
                        .orElse(rmEnt);
                detectedEntity.remove();
                base.targetsMap.remove(b);
                base.forceCompassUpdate();
                break;
            }
        }
    }
}
