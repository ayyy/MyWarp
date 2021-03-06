package me.taylorkelly.mywarp.listeners;

import me.taylorkelly.mywarp.MyWarp;
import me.taylorkelly.mywarp.timer.PlayerWarmup;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class MWEntityListener implements Listener {

    /**
     * Called whenever an entity takes damage
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)
                || !MyWarp.inst().getWarpSettings().timersAbortOnDamage) {
            return;
        }
        Player victim = (Player) event.getEntity();

        if (PlayerWarmup.isActive(victim.getName())
                && !MyWarp.inst().getPermissionsManager()
                        .hasPermission(victim, "mywarp.warmup.disobey.dmgabort")) {
            PlayerWarmup.endWarmup(victim.getName());
            victim.sendMessage(MyWarp.inst().getLanguageManager().getString("timer.warmup.cancelled.damage"));
        }
    }
}
