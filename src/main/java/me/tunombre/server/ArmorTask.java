package me.tunombre.server;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ArmorTask extends BukkitRunnable {
    private final Main plugin;

    public ArmorTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack[] armor = p.getInventory().getArmorContents();
            double extraVida = 0;
            double velMineria = 0;
            double velMovimiento = 0;

            String claseDominante = "Cualquiera";

            // ==========================================
            // 1. DETECTAR CLASE DOMINANTE
            // ==========================================
            // Buscamos la primera pieza de armadura que tenga una clase específica
            for (ItemStack item : armor) {
                if (item == null || !item.hasItemMeta()) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    ArmorDTO dto = plugin.getFileManager().getArmorDTO(pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING));
                    if (dto != null && !dto.claseRequerida().equalsIgnoreCase("Cualquiera") && !dto.claseRequerida().equalsIgnoreCase("Ninguna")) {
                        claseDominante = dto.claseRequerida();
                        break; // ¡Encontramos la clase del jugador!
                    }
                }
            }

            // Guardamos la clase en la RAM para que las armas sepan qué eres
            plugin.claseJugador.put(p.getUniqueId(), claseDominante);

            // ==========================================
            // 2. APLICAR STATS Y RESTRICCIONES
            // ==========================================
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item == null || !item.hasItemMeta()) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();

                if (pdc.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {
                    String id = pdc.get(ItemManager.llaveArmaduraId, PersistentDataType.STRING);
                    ArmorDTO dto = plugin.getFileManager().getArmorDTO(id);

                    if (dto != null) {
                        boolean cumpleRequisitos = true;

                        // A) REVISAR CHOQUE DE CLASES
                        if (!dto.claseRequerida().equalsIgnoreCase("Cualquiera") &&
                                !dto.claseRequerida().equalsIgnoreCase("Ninguna") &&
                                !dto.claseRequerida().equalsIgnoreCase(claseDominante)) {

                            quitarArmadura(p, item, i, "Choque de Clases (" + dto.claseRequerida() + ")");
                            cumpleRequisitos = false;
                        }

                        // B) REVISAR NIVEL DE AURASKILLS
                        if (cumpleRequisitos) {
                            int nivelJugador = 1;
                            String skill = dto.skillRequerida();

                            try {
                                if (skill.equalsIgnoreCase("Combate")) nivelJugador = Math.max(1, AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FIGHTING));
                                else if (skill.equalsIgnoreCase("Minería")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.MINING));
                                else if (skill.equalsIgnoreCase("Agricultura")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FARMING));
                                else if (skill.equalsIgnoreCase("Pesca")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING));
                                else if (skill.equalsIgnoreCase("Tala")) nivelJugador = Math.max(1, (int) AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FORAGING));
                            } catch (Exception ignored) {}

                            if (nivelJugador < dto.nivelRequerido()) {
                                quitarArmadura(p, item, i, skill + " Nv." + dto.nivelRequerido());
                                cumpleRequisitos = false;
                            }
                        }

                        // Si todo es legal, sumamos las stats
                        if (cumpleRequisitos) {
                            extraVida += dto.vidaExtra();
                            velMineria += dto.velocidadMineria();
                            velMovimiento += dto.velocidadMovimiento();
                        }
                    }
                }
                else if (pdc.has(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE)) {
                    extraVida += pdc.get(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE);
                }
            }

            // ==========================================
            // 3. APLICAR VIDA Y EFECTOS FINALES
            // ==========================================
            double total = 20.0 + extraVida;
            if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() != total) {
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(total);

                // ¡LA MAGIA ANTI-PANTALLA LLENA!
                p.setHealthScaled(true);
                p.setHealthScale(20.0); // Siempre dibuja solo 10 corazones (20.0 visual)
            }

            if (velMineria > 0) {
                int nivelHaste = (int) (velMineria / 20);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 40, nivelHaste, false, false, false));
            }
            if (velMovimiento > 0) {
                int nivelSpeed = (int) (velMovimiento / 20);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 40, nivelSpeed, false, false, false));
            }
        }
    }

    private void quitarArmadura(Player p, ItemStack item, int slotIndex, String razon) {
        p.getInventory().addItem(item);
        if(slotIndex == 0) p.getInventory().setBoots(null);
        if(slotIndex == 1) p.getInventory().setLeggings(null);
        if(slotIndex == 2) p.getInventory().setChestplate(null);
        if(slotIndex == 3) p.getInventory().setHelmet(null);

        p.sendMessage("§c§l⚠ NO ERES DIGNO §7| Requisito: §e" + razon);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
}