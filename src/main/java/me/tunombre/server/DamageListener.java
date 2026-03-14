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
        // Solo nos importa si un jugador golpea a una entidad viva (mobs o jugadores)
        if (event.getDamager() instanceof Player jugador && event.getEntity() instanceof LivingEntity victima) {

            ItemStack arma = jugador.getInventory().getItemInMainHand();
            if (arma == null || !arma.hasItemMeta()) return;

            var pdc = arma.getItemMeta().getPersistentDataContainer();

            // ==========================================
            // ⚔️ SISTEMA DE ARMAS RPG NEXOCORE
            // ==========================================
            if (pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String idArma = pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                WeaponDTO dto = plugin.getFileManager().getWeaponDTO(idArma);

                if (dto != null) {
                    String claseJugador = plugin.claseJugador.getOrDefault(jugador.getUniqueId(), "Cualquiera");
                    int nivelCombate = 1;
                    try {
                        nivelCombate = Math.max(1, AuraSkillsApi.get().getUser(jugador.getUniqueId()).getSkillLevel(Skills.FIGHTING));
                    } catch (Exception ignored) {}

                    // 1. RESTRICCIONES (Clase y Nivel de AuraSkills)
                    if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase(claseJugador)) {
                        jugador.sendMessage("§c§l⚠ §cTu clase (" + claseJugador + ") no puede usar esta arma.");
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        event.setDamage(1.0); // Daño de castigo mínimo
                        return;
                    }

                    if (nivelCombate < dto.nivelRequerido()) {
                        event.setCancelled(true);
                        jugador.sendMessage("§c§l⚠ ARMA PESADA §8| §7Necesitas Nivel de Combate §e" + dto.nivelRequerido());
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }

                    // 2. DAÑO BASE
                    // Paper 1.21.11 ya sumó el daño del WeaponDTO al golpe usando el AttributeModifier
                    // Así que el daño "crudo" ya viene perfecto en el evento.
                    double dañoFinal = event.getDamage();

                    // 3. MULTIPLICADOR DE PRESTIGIO (Ej: +15% de daño por nivel)
                    int prestigio = pdc.getOrDefault(ItemManager.llaveWeaponPrestige, PersistentDataType.INTEGER, 0);
                    if (prestigio > 0 && dto.permitePrestigio()) {
                        dañoFinal += (dañoFinal * (prestigio * dto.multiPrestigio()));
                    }

                    // 4. DAÑO Y EFECTOS ELEMENTALES
                    String elementoLimpio = ChatColor.stripColor(dto.elemento()).toUpperCase();
                    String nombreMob = victima.getCustomName() != null ? ChatColor.stripColor(victima.getCustomName()).toUpperCase() : "";
                    double multElemental = 1.0;

                    if (elementoLimpio.contains("FUEGO") || elementoLimpio.contains("MAGMA") || elementoLimpio.contains("SOLAR")) {
                        victima.setFireTicks(60); // Quema 3 segundos
                        if (nombreMob.contains("[HIELO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("HIELO") || elementoLimpio.contains("AGUA")) {
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, false));
                        if (nombreMob.contains("[FUEGO]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("RAYO") || elementoLimpio.contains("TORMENTA")) {
                        if (Math.random() <= 0.15) victima.getWorld().strikeLightningEffect(victima.getLocation());
                        if (nombreMob.contains("[AGUA]")) multElemental = 2.0;
                    }
                    else if (elementoLimpio.contains("VENENO")) {
                        victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1, false, false, false));
                    }

                    dañoFinal *= multElemental;

                    if (multElemental > 1.0) {
                        jugador.sendMessage("§a§l¡GOLPE CRÍTICO ELEMENTAL!");
                    }

                    // Aplicar el daño masivo RPG final
                    event.setDamage(dañoFinal);
                }
            }
        }
    }
}