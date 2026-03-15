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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
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

        if (!jugador.getWorld().getName().equalsIgnoreCase(MUNDO_RPG)) return;

        Material tipoOriginal = bloque.getType();
        BlockData dataOriginal = bloque.getBlockData();

        int xpGanada = 0;
        int costeEnergia = 0;
        ItemStack recompensa = null;
        boolean esCultivo = false, esMineral = false, esTronco = false;

        // --- DICCIONARIO DE RECOLECCIÓN ---
        if (tipoOriginal == Material.COAL_ORE || tipoOriginal == Material.DEEPSLATE_COAL_ORE) {
            xpGanada = 2; costeEnergia = 5; recompensa = new ItemStack(Material.COAL, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.IRON_ORE || tipoOriginal == Material.DEEPSLATE_IRON_ORE) {
            xpGanada = 5; costeEnergia = 10; recompensa = new ItemStack(Material.RAW_IRON, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.DIAMOND_ORE || tipoOriginal == Material.DEEPSLATE_DIAMOND_ORE) {
            xpGanada = 25; costeEnergia = 30; recompensa = new ItemStack(Material.DIAMOND, 1); esMineral = true;
        }
        else if (tipoOriginal == Material.OAK_LOG || tipoOriginal == Material.BIRCH_LOG || tipoOriginal == Material.SPRUCE_LOG) {
            xpGanada = 3; costeEnergia = 4; recompensa = new ItemStack(tipoOriginal, 1); esTronco = true;
        }
        else if (tipoOriginal == Material.WHEAT || tipoOriginal == Material.CARROTS || tipoOriginal == Material.POTATOES) {
            if (dataOriginal instanceof Ageable cultivo && cultivo.getAge() == cultivo.getMaximumAge()) {
                xpGanada = 1; costeEnergia = 1; esCultivo = true;
                recompensa = new ItemStack(tipoOriginal == Material.WHEAT ? Material.WHEAT : (tipoOriginal == Material.CARROTS ? Material.CARROT : Material.POTATO), 1);
            } else { event.setCancelled(true); return; }
        }

        if (xpGanada > 0) {
            event.setCancelled(true);
            long ahora = System.currentTimeMillis();
            if (cooldownRecoleccion.containsKey(uuid) && (ahora - cooldownRecoleccion.get(uuid)) < 300) return;

            int energiaActual = plugin.energiaMineria.getOrDefault(uuid, 100);
            if (energiaActual < costeEnergia) {
                jugador.sendActionBar("§c§l⚠ ¡AGOTADO! §7Descansa...");
                return;
            }

            // 🛠️ LÓGICA DE HERRAMIENTA (NexoCore)
            double fortunaExtra = 0.0;
            ItemStack itemMano = jugador.getInventory().getItemInMainHand();
            if (itemMano != null && itemMano.hasItemMeta()) {
                ItemMeta metaTool = itemMano.getItemMeta();
                if (metaTool.getPersistentDataContainer().has(ItemManager.llaveHerramientaId, PersistentDataType.STRING)) {
                    String toolId = metaTool.getPersistentDataContainer().get(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
                    ToolDTO toolData = plugin.getFileManager().getToolDTO(toolId);

                    if (toolData != null) {
                        fortunaExtra = toolData.multiplicadorFortuna();

                        // Actualizar Bloques Rotos
                        Integer rotosGuardados = metaTool.getPersistentDataContainer().get(ItemManager.llaveBloquesRotos, PersistentDataType.INTEGER);
                        int rotos = (rotosGuardados != null ? rotosGuardados : 0) + 1;
                        metaTool.getPersistentDataContainer().set(ItemManager.llaveBloquesRotos, PersistentDataType.INTEGER, rotos);

                        // Evolución Azada Matemática
                        if (toolData.esEvolutiva()) {
                            if (rotos == 10000) metaTool.setDisplayName("§6§lAzada Matemática Avanzada");
                            if (rotos == 100000) metaTool.setDisplayName("§d§lAzada Matemática Divina");
                        }

                        // Actualizar Lore dinámico
                        List<String> lore = metaTool.getLore();
                        if (lore != null) {
                            for (int i = 0; i < lore.size(); i++) {
                                if (lore.get(i).contains("Bloques Rotos:")) {
                                    lore.set(i, "§7Bloques Rotos: §e" + String.format("%,d", rotos));
                                    break;
                                }
                            }
                            metaTool.setLore(lore);
                        }
                        itemMano.setItemMeta(metaTool);
                    }
                }
            }

            // 🛡️ LÓGICA DE ARMADURA (Suma fortuna)
            double suerteTotal = fortunaExtra;
            for (ItemStack armor : jugador.getInventory().getArmorContents()) {
                if (armor == null || !armor.hasItemMeta()) continue;
                String id = armor.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveArmaduraId, PersistentDataType.STRING);
                ArmorDTO armorDTO = plugin.getFileManager().getArmorDTO(id);
                if (armorDTO != null) {
                    if (esMineral) suerteTotal += armorDTO.suerteMinera();
                    if (esCultivo) suerteTotal += armorDTO.suerteAgricola();
                    if (esTronco) suerteTotal += armorDTO.suerteTala();
                }
            }

            // 🎲 CÁLCULO DE DROPS
            int cantidad = (random.nextDouble() * 100 <= suerteTotal) ? 2 : 1;
            if (cantidad > 1) {
                jugador.sendActionBar("§b✨ ¡DROP DUPLICADO! §b(+" + suerteTotal + "%)");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2f);
            }

            if (recompensa != null) {
                recompensa.setAmount(cantidad);
                jugador.getInventory().addItem(recompensa);
            }

            plugin.energiaMineria.put(uuid, Math.max(0, energiaActual - costeEnergia));
            cooldownRecoleccion.put(uuid, ahora);
            plugin.darNexoXp(jugador, xpGanada);

            // Regeneración
            if (esCultivo) {
                Ageable cultivo = (Ageable) dataOriginal; cultivo.setAge(0);
                bloque.setBlockData(cultivo);
            } else {
                bloque.setType(Material.BEDROCK);
                Bukkit.getScheduler().runTaskLater(plugin, () -> bloque.setType(tipoOriginal), 200L);
            }
        } else if (!jugador.hasPermission("nexo.admin")) event.setCancelled(true);
    }
}