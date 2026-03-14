package me.tunombre.server;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class InteractListener implements Listener {

    private final Main plugin;
    // ⏳ Memoria RAM para Cooldowns: "UUID-Habilidad" -> Tiempo en Milisegundos
    private final HashMap<String, Long> cooldowns = new HashMap<>();

    public InteractListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alUsarHabilidad(PlayerInteractEvent event) {
        Player jugador = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = jugador.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR || !item.hasItemMeta()) return;

            var pdc = item.getItemMeta().getPersistentDataContainer();
            UUID uuid = jugador.getUniqueId();

            // ==========================================
            // 🔮 SISTEMA DE HABILIDADES ACTIVAS
            // ==========================================
            if (pdc.has(ItemManager.llaveWeaponId, PersistentDataType.STRING)) {
                String idArma = pdc.get(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                WeaponDTO dto = plugin.getFileManager().getWeaponDTO(idArma);

                if (dto != null && !dto.habilidadId().equalsIgnoreCase("ninguna")) {
                    event.setCancelled(true); // Para no interactuar con bloques (ej. puertas)

                    // 1. Validar que tiene nivel para usarla
                    int nivelCombate = 1;
                    try {
                        nivelCombate = Math.max(1, AuraSkillsApi.get().getUser(uuid).getSkillLevel(Skills.FIGHTING));
                    } catch (Exception ignored) {}
                    if (nivelCombate < dto.nivelRequerido()) return;

                    // 2. SISTEMA DE COOLDOWN (5 Segundos por defecto)
                    String cdKey = uuid.toString() + "-" + dto.habilidadId();
                    long tiempoActual = System.currentTimeMillis();
                    int tiempoCooldown = 5000;

                    if (cooldowns.containsKey(cdKey)) {
                        long tiempoRestante = (cooldowns.get(cdKey) + tiempoCooldown) - tiempoActual;
                        if (tiempoRestante > 0) {
                            jugador.sendActionBar("§c§l⏳ Enfriamiento: §e" + (tiempoRestante / 1000) + "s");
                            return;
                        }
                    }

                    // 3. CONSUMO DE MANÁ (AuraSkills)
                    int costoMana = 30; // Podemos ajustarlo por habilidad luego
                    SkillsUser user = AuraSkillsApi.get().getUser(uuid);
                    if (user != null) {
                        if (user.getMana() < costoMana) {
                            jugador.sendMessage("§b§l⚠ NO TIENES SUFICIENTE MANÁ §7(" + costoMana + ")");
                            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                            return;
                        }
                        user.setMana(user.getMana() - costoMana);
                    }

                    // 4. EJECUTAR PODER
                    cooldowns.put(cdKey, tiempoActual);
                    ejecutarHabilidad(jugador, dto.habilidadId());
                }
            }
        }
    }

    // ==========================================
    // ⚡ DICCIONARIO DE PODERES
    // ==========================================
    private void ejecutarHabilidad(Player p, String habilidad) {
        switch (habilidad.toLowerCase()) {

            case "quake":
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 1);
                // Daña en área y levanta a los enemigos
                for (org.bukkit.entity.Entity e : p.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof LivingEntity victima && e != p) {
                        victima.damage(10.0, p);
                        victima.setVelocity(new Vector(0, 0.6, 0));
                    }
                }
                p.sendMessage("§6§l¡TERREMOTO!");
                break;

            case "ola":
                p.playSound(p.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
                Vector direccion = p.getLocation().getDirection().multiply(1.5);
                for (org.bukkit.entity.Entity e : p.getNearbyEntities(6, 6, 6)) {
                    if (e instanceof LivingEntity victima && e != p) {
                        // Solo empuja si están al frente del jugador
                        Vector dirAlEnemigo = victima.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        if (p.getLocation().getDirection().dot(dirAlEnemigo) > 0.5) {
                            victima.damage(5.0, p);
                            victima.setVelocity(direccion);
                        }
                    }
                }
                p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(0,1,0), 100, 0.5, 0.5, 0.5, 0.1);
                p.sendMessage("§9§l¡OLA CHOCANTE!");
                break;

            case "rafaga":
                p.playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
                // Lanza 3 flechas con un delay usando el reloj del servidor
                for (int i = 0; i < 3; i++) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Arrow flecha = p.launchProjectile(Arrow.class);
                        flecha.setVelocity(p.getLocation().getDirection().multiply(2.0));
                    }, i * 4L); // Retraso de 4 ticks entre cada flecha
                }
                p.sendMessage("§f§l¡RÁFAGA DE VIENTO!");
                break;

            case "traslacion":
                Location destino = p.getLocation().clone().add(p.getLocation().getDirection().multiply(6));
                p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 50);
                p.teleport(destino);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                p.sendMessage("§8§l¡TRASLACIÓN!");
                break;

            default:
                // Mensaje genérico para habilidades aún no programadas
                p.sendMessage("§e✨ Habilidad: §f" + habilidad.toUpperCase());
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                break;
        }
    }
}