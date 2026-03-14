package me.tunombre.server;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

            int nivelCombate = plugin.combateNiveles.getOrDefault(p.getUniqueId(), 1);
            String claseJugador = plugin.claseJugador.getOrDefault(p.getUniqueId(), "Aventurero");

            // ==========================================
            // 1. ESCANEAR ARMADURA PUESTA
            // ==========================================
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item == null || !item.hasItemMeta()) continue;

                if (item.getItemMeta().hasLore()) {
                    boolean cumpleRequisitos = true;

                    for (String line : item.getItemMeta().getLore()) {
                        String clean = ChatColor.stripColor(line);

                        // 🛑 RESTRICCIÓN DE CLASE (¡AHORA SÍ TE LA QUITA!)
                        if (clean.startsWith("Clase: ")) {
                            String claseRequerida = clean.replace("Clase: ", "").trim();
                            if (!claseRequerida.equalsIgnoreCase(claseJugador) && !claseRequerida.equalsIgnoreCase("Cualquiera")) {
                                p.getInventory().addItem(item);
                                if(i == 0) p.getInventory().setBoots(null);
                                if(i == 1) p.getInventory().setLeggings(null);
                                if(i == 2) p.getInventory().setChestplate(null);
                                if(i == 3) p.getInventory().setHelmet(null);

                                p.sendMessage("§c§l⚠ CLASE INCORRECTA §7| Esta armadura es exclusiva para la clase §e" + claseRequerida);
                                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                                cumpleRequisitos = false;
                            }
                        }

                        // ⚔️ REQUISITO DE COMBATE
                        if (clean.contains("Requisito de Combate: Nivel ")) {
                            try {
                                int req = Integer.parseInt(clean.split("Nivel ")[1].trim());
                                if (nivelCombate < req) {
                                    quitarArmadura(p, item, i, "Combate", req);
                                    cumpleRequisitos = false;
                                }
                            } catch (Exception ignored) {}
                        }

                        // ⛏️ REQUISITO DE MINERÍA
                        if (clean.contains("Requisito de Minería: Nivel ")) {
                            try {
                                int req = Integer.parseInt(clean.split("Nivel ")[1].trim());
                                int nivelMineria = Math.max(1, AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.MINING));

                                if (nivelMineria < req) {
                                    quitarArmadura(p, item, i, "Minería", req);
                                    cumpleRequisitos = false;
                                }
                            } catch (Exception ignored) {}
                        }

                        // 🌾 REQUISITO DE AGRICULTURA
                        if (clean.contains("Requisito de Agricultura: Nivel ")) {
                            try {
                                int req = Integer.parseInt(clean.split("Nivel ")[1].trim());
                                int nivelAgricultura = Math.max(1, AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FARMING));

                                if (nivelAgricultura < req) {
                                    quitarArmadura(p, item, i, "Agricultura", req);
                                    cumpleRequisitos = false;
                                }
                            } catch (Exception ignored) {}
                        }

                        // 🎣 REQUISITO DE PESCA
                        if (clean.contains("Requisito de Pesca: Nivel ")) {
                            try {
                                int req = Integer.parseInt(clean.split("Nivel ")[1].trim());
                                int nivelPesca = Math.max(1, AuraSkillsApi.get().getUser(p.getUniqueId()).getSkillLevel(Skills.FISHING));

                                if (nivelPesca < req) {
                                    quitarArmadura(p, item, i, "Pesca", req);
                                    cumpleRequisitos = false;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    // 🔋 SI CUMPLE TODO, EXTRAEMOS LAS ESTADÍSTICAS
                    if (cumpleRequisitos) {
                        if (item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE)) {
                            extraVida += item.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveVidaExtra, PersistentDataType.DOUBLE);
                        }
                        if (item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveVelocidadMineria, PersistentDataType.DOUBLE)) {
                            velMineria += item.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveVelocidadMineria, PersistentDataType.DOUBLE);
                        }
                        if (item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveVelocidadMovimiento, PersistentDataType.DOUBLE)) {
                            velMovimiento += item.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveVelocidadMovimiento, PersistentDataType.DOUBLE);
                        }
                    }
                }
            }

            // ==========================================
            // 2. APLICAR VIDA FINAL
            // ==========================================
            double total = 20.0 + extraVida;
            if (p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() != total) {
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(total);
            }

            // ==========================================
            // 3. APLICAR EFECTOS PASIVOS (ESCALABLES)
            // ==========================================
            if (velMineria > 0) {
                // Cada 20 puntos de Velocidad = +1 Nivel de Prisa. ¡Escalado infinito!
                int nivelHaste = (int) (velMineria / 20);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 40, nivelHaste));
            }
            if (velMovimiento > 0) {
                // Cada 20 puntos de Movimiento = +1 Nivel de Velocidad.
                int nivelSpeed = (int) (velMovimiento / 20);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 40, nivelSpeed));
            }
        }
    }

    private void quitarArmadura(Player p, ItemStack item, int slotIndex, String habilidad, int nivelRequerido) {
        p.getInventory().addItem(item);
        if(slotIndex == 0) p.getInventory().setBoots(null);
        if(slotIndex == 1) p.getInventory().setLeggings(null);
        if(slotIndex == 2) p.getInventory().setChestplate(null);
        if(slotIndex == 3) p.getInventory().setHelmet(null);

        p.sendMessage("§c§l⚠ NIVEL BAJO §7| Necesitas nivel §e" + nivelRequerido + " §7en §b" + habilidad);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
    }
}