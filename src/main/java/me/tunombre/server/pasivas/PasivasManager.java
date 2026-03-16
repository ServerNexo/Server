package me.tunombre.server.pasivas;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.tunombre.server.Main;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PasivasManager {

    private final Main plugin;

    // Control de tiempos y estados
    public final Map<UUID, Long> cdUltimaBatalla = new ConcurrentHashMap<>();
    public final Map<UUID, Long> ultimoTroncoRoto = new ConcurrentHashMap<>();
    public final Map<UUID, Long> invulnerablesUltimaBatalla = new ConcurrentHashMap<>();

    public PasivasManager(Main plugin) {
        this.plugin = plugin;
        iniciarTareasPeriodicas();
    }

    // ==========================================
    // 🧠 LECTURA DE NIVELES (AuraSkills)
    // ==========================================
    public int getNivel(Player p, dev.aurelium.auraskills.api.skill.Skill skill) {
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(p.getUniqueId());
            if (user != null) return user.getSkillLevel(skill);
        } catch (Exception ignored) {}
        return 0;
    }

    // ==========================================
    // ⚡ DESCUENTO GLOBAL DE ENERGÍA (Encantamiento Lvl 50)
    // ==========================================
    public int calcularCostoEnergia(Player p, int costoBase) {
        if (getNivel(p, Skills.ENCHANTING) >= 50) {
            return (int) (costoBase * 0.90); // -10% de costo
        }
        return costoBase;
    }

    // ==========================================
    // ⚙️ TAREAS PERIÓDICAS (Aura Deméter y Visión)
    // ==========================================
    private void iniciarTareasPeriodicas() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // 1. Visión Profunda (Minería Lvl 10)
                if (p.getLocation().getY() < 0 && getNivel(p, Skills.MINING) >= 10) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
                }

                // 2. Limpieza de Invulnerabilidad (Última Batalla)
                if (invulnerablesUltimaBatalla.containsKey(id)) {
                    if (System.currentTimeMillis() > invulnerablesUltimaBatalla.get(id)) {
                        invulnerablesUltimaBatalla.remove(id);
                        p.sendMessage("§cTu inmunidad de Última Batalla se ha desvanecido.");
                    }
                }
            }
        }, 20L, 20L); // Cada segundo

        // 3. Aura de Deméter (Agricultura Lvl 50) - Cada 3 segundos
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (getNivel(p, Skills.FARMING) >= 50) {
                    int energia = plugin.energiaMineria.getOrDefault(p.getUniqueId(), 0);
                    int costo = calcularCostoEnergia(p, 5); // Aplica descuento si tiene Enchanting 50

                    if (energia >= costo) {
                        // Buscar cultivo cercano
                        Block centro = p.getLocation().getBlock();
                        boolean aplico = false;
                        buscarCultivo:
                        for (int x = -5; x <= 5; x++) {
                            for (int z = -5; z <= 5; z++) {
                                Block b = centro.getRelative(x, 0, z);
                                if (b.getBlockData() instanceof Ageable cultivo && cultivo.getAge() < cultivo.getMaximumAge()) {
                                    b.applyBoneMeal(BlockFace.UP);
                                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation(), 5);
                                    aplico = true;
                                    break buscarCultivo; // Solo uno por tick
                                }
                            }
                        }
                        if (aplico) {
                            plugin.energiaMineria.put(p.getUniqueId(), energia - costo);
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2f);
                        }
                    }
                }
            }
        }, 60L, 60L); // Cada 3 segundos
    }
}