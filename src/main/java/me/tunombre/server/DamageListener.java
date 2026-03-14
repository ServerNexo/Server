package me.tunombre.server;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
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

            var pdc = arma.getItemMeta().getPersistentDataContainer();
            double dañoReal = event.getDamage();

            // Leemos la clase dinámica que generó el ArmorTask
            String claseJugador = plugin.claseJugador.getOrDefault(jugador.getUniqueId(), "Cualquiera");

            // Leemos el nivel oficial de AuraSkills
            int nivelCombate = 1;
            try {
                nivelCombate = Math.max(1, AuraSkillsApi.get().getUser(jugador.getUniqueId()).getSkillLevel(Skills.FIGHTING));
            } catch (Exception ignored) {}

            // ==========================================
            // 1. REQUISITOS DE CLASE Y NIVEL
            // ==========================================
            if (pdc.has(ItemManager.llaveArmaClase, PersistentDataType.STRING)) {
                String claseArma = pdc.get(ItemManager.llaveArmaClase, PersistentDataType.STRING);
                if (!claseArma.equalsIgnoreCase(claseJugador) && !claseArma.equalsIgnoreCase("Cualquiera")) {
                    jugador.sendMessage("§c§l⚠ §cTu armadura no es compatible con el estilo de combate de esta arma.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    event.setDamage(1.0);
                    return;
                }
            }

            if (pdc.has(ItemManager.llaveArmaReqCombate, PersistentDataType.INTEGER)) {
                int req = pdc.get(ItemManager.llaveArmaReqCombate, PersistentDataType.INTEGER);
                if (nivelCombate < req) {
                    event.setCancelled(true);
                    jugador.sendMessage("§c§l⚠ ARMA PESADA §8| §7Necesitas Nivel de Combate §e" + req + " §7para usarla.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }
            }

            // ==========================================
            // 2. CÁLCULO DE DAÑO RPG
            // ==========================================
            if (pdc.has(ItemManager.llaveArmaDanioBase, PersistentDataType.DOUBLE)) {
                dañoReal = pdc.get(ItemManager.llaveArmaDanioBase, PersistentDataType.DOUBLE);
            }

            // Bono Herrería
            if (pdc.has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                int nivelHerreria = pdc.get(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER);
                dañoReal += (dañoReal * (nivelHerreria * 0.10));
            }

            // Escalado por Nivel de Combate (+2% por nivel de AuraSkills)
            dañoReal += (dañoReal * (nivelCombate * 0.02));

            // Bono Arma Mítica
            if (pdc.has(ItemManager.llaveArmaMitica, PersistentDataType.BYTE)) {
                dañoReal *= 1.5;
            }

            // ==========================================
            // 3. LA RUEDA ELEMENTAL (Debilidades)
            // ==========================================
            if (pdc.has(ItemManager.llaveElemento, PersistentDataType.STRING)) {
                String elementoArma = pdc.get(ItemManager.llaveElemento, PersistentDataType.STRING);
                String nombreMob = victima.getCustomName() != null ? ChatColor.stripColor(victima.getCustomName()).toUpperCase() : "";

                double multiplicador = 1.0;

                switch (elementoArma) {
                    case "FUEGO":
                        victima.setFireTicks(60);
                        if (nombreMob.contains("[HIELO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[RAYO]")) multiplicador = 0.5;
                        break;
                    case "HIELO":
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                        if (nombreMob.contains("[VENENO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[FUEGO]")) multiplicador = 0.5;
                        break;
                    case "RAYO":
                        if (Math.random() <= 0.20) victima.getWorld().strikeLightningEffect(victima.getLocation());
                        if (nombreMob.contains("[FUEGO]")) multiplicador = 2.0;
                        if (nombreMob.contains("[VENENO]")) multiplicador = 0.5;
                        break;
                    case "VENENO":
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1, false, false, false));
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

            event.setDamage(dañoReal);
        }
    }
}