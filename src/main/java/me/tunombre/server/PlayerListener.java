package me.tunombre.server;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();

        plugin.getDatabaseManager().cargarJugador(jugador);

        if (!jugador.hasPlayedBefore()) {
            jugador.getInventory().addItem(ItemManager.generarArmaRPG("baculo_manantial_t1"));
            jugador.sendMessage("§6§lNEXO §8| §7¡Bienvenido! Te hemos entregado tu primera arma.");
        }
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        Player jugador = event.getPlayer();
        plugin.getDatabaseManager().guardarJugador(jugador);
    }

    // ==========================================
    // 🔒 SISTEMA ANTI-DROP (SOULBOUND)
    // ==========================================
    @EventHandler
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // Revisamos si el ítem tiene la llave invisible "Soulbound"
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true); // Cancelamos que lo tire
            event.getPlayer().sendMessage("§c§l🔒 No puedes tirar un objeto ligado a tu alma.");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}