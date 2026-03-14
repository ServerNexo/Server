package me.tunombre.server;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InteractListener implements Listener {

    private final Main plugin;

    public InteractListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alUsarHabilidad(PlayerInteractEvent event) {
        Player jugador = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            ItemStack item = jugador.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR || !item.hasItemMeta()) return;
            if (item.getItemMeta().getDisplayName() == null) return;

            String nombre = item.getItemMeta().getDisplayName();
            UUID uuid = jugador.getUniqueId();

            // ==========================================
            // 1. ARTEFACTO: HOJA DEL VACÍO (Usa ENERGÍA ⚡)
            // ==========================================
            if (nombre.contains("Hoja del Vacío")) {
                event.setCancelled(true);
                int energiaActual = plugin.energiaMineria.getOrDefault(uuid, 100);
                int costoTeleport = 40;

                if (energiaActual >= costoTeleport) {
                    plugin.energiaMineria.put(uuid, energiaActual - costoTeleport);
                    Location origen = jugador.getLocation();
                    Location destino = origen.clone().add(origen.getDirection().multiply(8));
                    destino.setYaw(origen.getYaw()); destino.setPitch(origen.getPitch());
                    jugador.teleport(destino);
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    jugador.getWorld().spawnParticle(Particle.PORTAL, origen, 50);
                    jugador.getWorld().spawnParticle(Particle.PORTAL, destino, 50);
                } else {
                    jugador.sendMessage("§e§l⚠ NO TIENES SUFICIENTE ENERGÍA");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return; // Cortamos aquí para que no ejecute lo demás
            }

            // ==========================================
            // 2. HABILIDADES DE COMBATE (Usan MANÁ 💧)
            // ==========================================
            if (item.getItemMeta().hasLore()) {
                boolean esMago = false;
                for (String linea : item.getItemMeta().getLore()) {
                    if (ChatColor.stripColor(linea).contains("Clase: Mago")) esMago = true;
                }

                // HABILIDAD DE MAGO: EXPLOSIÓN DE FUEGO
                if (esMago && nombre.contains("Báculo")) {
                    event.setCancelled(true);

                    int nivelCombate = plugin.combateNiveles.getOrDefault(uuid, 1);
                    int maxMana = 100 + (nivelCombate * 10);
                    int manaActual = plugin.manaJugador.getOrDefault(uuid, maxMana);
                    int costoMana = 50; // Cuesta 50 de Maná lanzar un hechizo

                    if (manaActual >= costoMana) {
                        plugin.manaJugador.put(uuid, manaActual - costoMana);

                        // Lanzamos la bola de fuego
                        Fireball bolaFuego = jugador.launchProjectile(Fireball.class);
                        bolaFuego.setYield(2.0F); // Tamaño de la explosión (2.0 es seguro, no rompe mucho si desactivas el daño a bloques)
                        bolaFuego.setIsIncendiary(false); // Para no prender fuego a tu bosque por accidente

                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1f, 1f);

                    } else {
                        jugador.sendMessage("§b§l⚠ NO TIENES SUFICIENTE MANÁ");
                        jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                    }
                }
            }
        }
    }
}