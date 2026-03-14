package me.tunombre.server;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DamageListener implements Listener {

    private final Main plugin;

    public DamageListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPegar(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player jugador && event.getEntity() instanceof LivingEntity victima) {

            var arma = jugador.getInventory().getItemInMainHand();
            if (arma == null || !arma.hasItemMeta()) return;

            double dañoReal = event.getDamage();
            String claseJugador = "Mago"; // AQUI DEBES USAR: plugin.claseJugador.getOrDefault(jugador.getUniqueId(), "Ninguna");

            // 1. LEER EL DAÑO BASE Y RESTRICCIÓN DE CLASE
            if (arma.getItemMeta().hasLore()) {
                for (String linea : arma.getItemMeta().getLore()) {
                    String limpia = ChatColor.stripColor(linea);

                    // 🛑 RESTRICCIÓN DE CLASE EN ARMAS
                    if (limpia.startsWith("Clase: ")) {
                        String claseArma = limpia.replace("Clase: ", "").trim();
                        if (!claseArma.equalsIgnoreCase(claseJugador) && !claseArma.equalsIgnoreCase("Cualquiera")) {
                            jugador.sendMessage("§c§l⚠ §cEsta arma es muy pesada/compleja para tu clase.");
                            jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                            event.setDamage(1.0); // Hace daño de puño si no es su clase
                            return; // Cortamos la ejecución aquí
                        }
                    }

                    if (limpia.startsWith("Daño Base: ")) {
                        try {
                            dañoReal = Double.parseDouble(limpia.replace("Daño Base: ", "").trim());
                        } catch (Exception ignored) {}
                    }
                }
            }

            var container = arma.getItemMeta().getPersistentDataContainer();

            // 2. APLICAR BONO DE LA HERRERÍA
            if (container.has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                int nivelHerreria = container.get(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER);
                dañoReal += (dañoReal * (nivelHerreria * 0.10));
            }

            // 🎒 3. ESCANEAR INVENTARIO BUSCANDO TALISMANES DE DAÑO
            // Aquí sumamos daño extra si el jugador lleva "Artefactos" en su inventario
            // (Para esto tendrás que crear una 'llaveDanioExtra' en ItemManager después)
            for (ItemStack itemBolsa : jugador.getInventory().getContents()) {
                if (itemBolsa != null && itemBolsa.hasItemMeta()) {
                    /* Descomenta esto cuando añadas la llaveDanioExtra en ItemManager
                    if (itemBolsa.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveDanioExtra, PersistentDataType.DOUBLE)) {
                        dañoReal += itemBolsa.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveDanioExtra, PersistentDataType.DOUBLE);
                    }
                    */
                }
            }

            // 4. LA RUEDA ELEMENTAL (Debilidades)
            if (container.has(ItemManager.llaveElemento, PersistentDataType.STRING)) {
                String elementoArma = container.get(ItemManager.llaveElemento, PersistentDataType.STRING);
                String nombreMob = victima.getCustomName() != null ? ChatColor.stripColor(victima.getCustomName()).toUpperCase() : "";

                double multiplicador = 1.0;

                switch (elementoArma) {
                    case "FUEGO":
                        victima.setFireTicks(60);
                        if (nombreMob.contains("[HIELO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[RAYO]")) multiplicador = 0.5;
                        break;
                    case "HIELO":
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        if (nombreMob.contains("[VENENO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[FUEGO]")) multiplicador = 0.5;
                        break;
                    case "RAYO":
                        if (Math.random() <= 0.20) victima.getWorld().strikeLightningEffect(victima.getLocation());
                        if (nombreMob.contains("[FUEGO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[VENENO]")) multiplicador = 0.5;
                        break;
                    case "VENENO":
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                        if (nombreMob.contains("[RAYO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[HIELO]")) multiplicador = 0.5;
                        break;
                }

                dañoReal = dañoReal * multiplicador;

                if (multiplicador > 1.0) {
                    jugador.sendMessage("§a§l¡GOLPE CRÍTICO ELEMENTAL!");
                } else if (multiplicador < 1.0) {
                    jugador.sendMessage("§cEl enemigo resiste tu elemento...");
                }
            }

            // 5. SOBREESCRIBIR EL DAÑO
            event.setDamage(dañoReal);
        }
    }
}