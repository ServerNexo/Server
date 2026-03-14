package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class BlockBreakListener implements Listener {

    private final Main plugin;
    private final HashMap<UUID, Long> cooldownRecoleccion = new HashMap<>();
    private final String MUNDO_RPG = "Mina";
    private final Random random = new Random();

    public BlockBreakListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPonerBloque(BlockPlaceEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) {
            if (!event.getPlayer().hasPermission("nexo.admin")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void alRomperBloque(BlockBreakEvent event) {
        Player jugador = event.getPlayer();
        Block bloque = event.getBlock();
        UUID uuid = jugador.getUniqueId();

        // 1. Si no está en el mundo RPG, Minecraft normal
        if (!jugador.getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) {
            return;
        }

        Material tipoOriginal = bloque.getType();
        BlockData dataOriginal = bloque.getBlockData();

        int xpGanada = 0;
        int costeEnergia = 0;
        ItemStack recompensa = null;
        boolean esCultivo = false;
        boolean esMineral = false;
        boolean esTronco = false;

        // ==========================================
        // 2. DICCIONARIO DE RECOLECCIÓN
        // ==========================================
        if (tipoOriginal == Material.COAL_ORE || tipoOriginal == Material.DEEPSLATE_COAL_ORE) {
            xpGanada = 2; costeEnergia = 5; recompensa = new ItemStack(Material.COAL, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.IRON_ORE || tipoOriginal == Material.DEEPSLATE_IRON_ORE) {
            xpGanada = 5; costeEnergia = 10; recompensa = new ItemStack(Material.RAW_IRON, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.DIAMOND_ORE || tipoOriginal == Material.DEEPSLATE_DIAMOND_ORE) {
            xpGanada = 25; costeEnergia = 30; recompensa = new ItemStack(Material.DIAMOND, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.OAK_LOG || tipoOriginal == Material.BIRCH_LOG || tipoOriginal == Material.SPRUCE_LOG || tipoOriginal == Material.DARK_OAK_LOG) {
            xpGanada = 3; costeEnergia = 4; recompensa = new ItemStack(tipoOriginal, 1); esTronco = true;
        }
        else if (tipoOriginal == Material.WHEAT || tipoOriginal == Material.CARROTS || tipoOriginal == Material.POTATOES) {
            if (dataOriginal instanceof Ageable) {
                Ageable cultivo = (Ageable) dataOriginal;
                if (cultivo.getAge() == cultivo.getMaximumAge()) {
                    xpGanada = 1; costeEnergia = 1; esCultivo = true;
                    if (tipoOriginal == Material.WHEAT) recompensa = new ItemStack(Material.WHEAT, 1);
                    else if (tipoOriginal == Material.CARROTS) recompensa = new ItemStack(Material.CARROT, 1);
                    else if (tipoOriginal == Material.POTATOES) recompensa = new ItemStack(Material.POTATO, 1);
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // ==========================================
        // 3. LÓGICA CORE (Energía y Armaduras)
        // ==========================================
        if (xpGanada > 0) {
            event.setCancelled(true);

            long ahora = System.currentTimeMillis();
            if (cooldownRecoleccion.containsKey(uuid) && (ahora - cooldownRecoleccion.get(uuid)) < 300) {
                return;
            }

            int energiaActual = plugin.energiaMineria.getOrDefault(uuid, 100);
            if (energiaActual < costeEnergia) {
                jugador.sendActionBar("§c§l⚠ ¡ESTÁS AGOTADO! §7Descansa un poco...");
                return;
            }

            // ESCANEAR ARMADURA PARA SUERTE
            int cantidadDrops = 1;
            double suerteMineraTotal = 0.0;
            double suerteAgricolaTotal = 0.0;
            double suerteTalaTotal = 0.0;

            for (ItemStack itemArmadura : jugador.getInventory().getArmorContents()) {
                if (itemArmadura == null || !itemArmadura.hasItemMeta()) continue;
                var meta = itemArmadura.getItemMeta().getPersistentDataContainer();

                if (meta.has(ItemManager.llaveSuerteMinera, PersistentDataType.DOUBLE)) {
                    suerteMineraTotal += meta.get(ItemManager.llaveSuerteMinera, PersistentDataType.DOUBLE);
                }
                if (meta.has(ItemManager.llaveSuerteAgricola, PersistentDataType.DOUBLE)) {
                    suerteAgricolaTotal += meta.get(ItemManager.llaveSuerteAgricola, PersistentDataType.DOUBLE);
                }
                if (meta.has(ItemManager.llaveSuerteTala, PersistentDataType.DOUBLE)) {
                    suerteTalaTotal += meta.get(ItemManager.llaveSuerteTala, PersistentDataType.DOUBLE);
                }
            }

            // 🎲 CÁLCULO DE PROBABILIDADES
            if (esMineral && suerteMineraTotal > 0 && random.nextDouble() * 100 <= suerteMineraTotal) {
                cantidadDrops = 2;
                jugador.sendActionBar("§b✨ §l¡MINERAL DUPLICADO! §b✨");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }

            if (esCultivo && suerteAgricolaTotal > 0 && random.nextDouble() * 100 <= suerteAgricolaTotal) {
                cantidadDrops = 2;
                jugador.sendActionBar("§a🌾 §l¡COSECHA ABUNDANTE! §a🌾");
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_COMPOSTER_READY, 0.5f, 2.0f);
            }

            if (esTronco && suerteTalaTotal > 0 && random.nextDouble() * 100 <= suerteTalaTotal) {
                cantidadDrops = 2;
                jugador.sendActionBar("§2🪓 §l¡MADERA EXTRA! §2🪓");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }

            // Dar Recompensas
            if (recompensa != null) {
                recompensa.setAmount(cantidadDrops);
                jugador.getInventory().addItem(recompensa);
            }

            // Consumir Energía y dar XP
            plugin.energiaMineria.put(uuid, Math.max(0, energiaActual - costeEnergia));
            cooldownRecoleccion.put(uuid, ahora);
            plugin.darNexoXp(jugador, xpGanada);

            // Regeneración de bloque (Hypixel Style)
            if (esCultivo) {
                Ageable cultivo = (Ageable) dataOriginal;
                cultivo.setAge(0);
                bloque.setBlockData(cultivo);
            } else {
                bloque.setType(Material.BEDROCK);
                Bukkit.getScheduler().runTaskLater(plugin, () -> bloque.setType(tipoOriginal), 200L);
            }

        } else {
            if (!jugador.hasPermission("nexo.admin")) event.setCancelled(true);
        }
    }
}