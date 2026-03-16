package me.tunombre.server.colecciones;

import me.tunombre.server.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ColeccionesListener implements Listener {
    private final Main plugin;

    public ColeccionesListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        CollectionManager.loadPlayerFromDatabase(event.getPlayer().getUniqueId(), plugin.getDatabaseManager().getDataSource());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Se borra de la RAM (El guardado lo hace FlushTask)
        CollectionManager.removeProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata("player_placed", new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("player_placed")) return;

        String blockId = event.getBlock().getType().name().toLowerCase();

        if (plugin.getColeccionesConfig().esColeccion(blockId)) {
            CollectionProfile profile = CollectionManager.getProfile(event.getPlayer().getUniqueId());
            if (profile != null) profile.addProgress(blockId, 1, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobId = event.getEntity().getType().name().toLowerCase();

        if (plugin.getColeccionesConfig().esSlayer(mobId)) {
            CollectionProfile profile = CollectionManager.getProfile(killer.getUniqueId());
            if (profile != null) {
                int xpPorKill = plugin.getColeccionesConfig().getDatosSlayer(mobId).getInt("xp_por_kill", 1);
                profile.addProgress("slayer_" + mobId, xpPorKill, true);
            }
        }
    }
}