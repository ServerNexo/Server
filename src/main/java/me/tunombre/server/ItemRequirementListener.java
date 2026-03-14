package me.tunombre.server;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.UUID;

public class ItemRequirementListener implements Listener {

    private final Main plugin;

    public ItemRequirementListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alAtacar(EntityDamageByEntityEvent event) {
        // Solo nos importa si el que ataca es un jugador
        if (!(event.getDamager() instanceof Player)) return;

        Player jugador = (Player) event.getDamager();
        UUID uuid = jugador.getUniqueId();
        ItemStack arma = jugador.getInventory().getItemInMainHand();

        // Si no tiene un item en la mano, o el item no tiene "Lore" (descripción), lo dejamos pasar
        if (!arma.hasItemMeta() || !arma.getItemMeta().hasLore()) return;

        List<String> lore = arma.getItemMeta().getLore();
        boolean esMitica = false;

        // ==========================================
        // 1. VERIFICAR REQUISITOS (Candados)
        // ==========================================
        for (String linea : lore) {
            String lineaLimpia = ChatColor.stripColor(linea);

            // A) REQUISITO DE COMBATE (Para espadas, hachas, arcos)
            if (lineaLimpia.startsWith("Nivel de Combate Requerido: ")) {
                int nivelRequerido = extraerNivel(lineaLimpia, "Nivel de Combate Requerido: ");
                int nivelActual = plugin.combateNiveles.getOrDefault(uuid, 1);

                if (nivelActual < nivelRequerido) {
                    cancelarAtaque(event, jugador, "Combate", nivelRequerido);
                    return; // Detiene todo el código aquí mismo
                }
            }

            // B) REQUISITO GLOBAL DEL NEXO (Para ítems utilitarios o sets especiales)
            if (lineaLimpia.startsWith("Nivel del Nexo Requerido: ")) {
                int nivelRequerido = extraerNivel(lineaLimpia, "Nivel del Nexo Requerido: ");
                int nivelActual = plugin.nexoNiveles.getOrDefault(uuid, 1);

                if (nivelActual < nivelRequerido) {
                    cancelarAtaque(event, jugador, "Nexo", nivelRequerido);
                    return; // Detiene todo el código aquí mismo
                }
            }

            // C) DETECCIÓN DE RAREZA (Para el escalado de daño)
            if (lineaLimpia.contains("Arma Mítica")) {
                esMitica = true;
            }
        }

        // ==========================================
        // 2. DAÑO ESCALABLE POR NIVEL DE COMBATE
        // ==========================================
        // Si el código llegó hasta aquí, significa que el jugador SÍ es digno de usar el arma.

        int nivelCombate = plugin.combateNiveles.getOrDefault(uuid, 1);
        double danioBase = event.getDamage();

        // El escalado: Por cada Nivel de Combate que tengas, tu arma hace un 2% más de daño
        double multiplicador = nivelCombate * 0.02;
        double danioFinal = danioBase + (danioBase * multiplicador);

        // Si el lore decía "Arma Mítica", recibe un 50% de daño extra multiplicativo
        if (esMitica) {
            danioFinal = danioFinal * 1.5;
        }

        // ¡Sobreescribimos el daño de Minecraft por nuestro daño RPG!
        event.setDamage(danioFinal);

        // (Opcional) Si quieres que el jugador vea en su pantalla el daño que hace, quita las "//" de la siguiente línea:
        // jugador.sendActionBar("§c⚔ Daño: " + String.format("%.1f", danioFinal) + " §8(Bono: +" + (int)(multiplicador*100) + "%)");
    }

    // --- MÉTODOS DE AYUDA (Para no repetir código) ---

    private int extraerNivel(String linea, String prefijo) {
        try {
            return Integer.parseInt(linea.replace(prefijo, "").trim());
        } catch (NumberFormatException e) {
            return 0; // Si hay un error leyendo el lore, no pide nivel
        }
    }

    private void cancelarAtaque(EntityDamageByEntityEvent event, Player p, String tipo, int req) {
        event.setCancelled(true);
        p.sendMessage("§c§l⚠ ARMA PESADA §8| §7Necesitas Nivel de §e" + tipo + " " + req + " §7para usarla.");
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
}