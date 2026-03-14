package me.tunombre.server;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    // Llaves Universales y Materiales
    public static NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound;

    // Llaves Antiguas (Profesiones/Stats)
    public static NamespacedKey llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento, llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina;

    // 🚀 NUEVAS LLAVES DTO OPTIMIZADAS
    public static NamespacedKey llaveArmaduraId;
    public static NamespacedKey llaveWeaponId;
    public static NamespacedKey llaveWeaponPrestige;

    // Llaves Antiguas de Armas (Las mantenemos por compatibilidad con viejos ítems si los hay)
    public static NamespacedKey llaveArmaClase, llaveArmaReqCombate, llaveArmaDanioBase, llaveArmaMitica;

    public static Main pluginMemoria;

    public static void init(Main plugin) {
        pluginMemoria = plugin;
        llaveNivelMejora = new NamespacedKey(plugin, "nexo_upgrade");
        llaveMaterialMejora = new NamespacedKey(plugin, "nexo_material_polvo");
        llaveVidaExtra = new NamespacedKey(plugin, "nexo_vida_extra");
        llaveElemento = new NamespacedKey(plugin, "nexo_elemento");
        llaveSoulbound = new NamespacedKey(plugin, "nexo_soulbound");

        llaveSuerteMinera = new NamespacedKey(plugin, "nexo_suerte_minera");
        llaveVelocidadMineria = new NamespacedKey(plugin, "nexo_velocidad_mineria");
        llaveSuerteAgricola = new NamespacedKey(plugin, "nexo_suerte_agricola");
        llaveVelocidadMovimiento = new NamespacedKey(plugin, "nexo_velocidad_movimiento");
        llaveSuerteTala = new NamespacedKey(plugin, "nexo_suerte_tala");
        llaveFuerzaHacha = new NamespacedKey(plugin, "nexo_fuerza_hacha");
        llaveVelocidadPesca = new NamespacedKey(plugin, "nexo_velocidad_pesca");
        llaveCriaturaMarina = new NamespacedKey(plugin, "nexo_criatura_marina");

        // DTOs
        llaveArmaduraId = new NamespacedKey(plugin, "nexo_armadura_id");
        llaveWeaponId = new NamespacedKey(plugin, "nexo_weapon_id");
        llaveWeaponPrestige = new NamespacedKey(plugin, "nexo_weapon_prestige");

        // Compatibilidad armas viejas
        llaveArmaClase = new NamespacedKey(plugin, "nexo_arma_clase");
        llaveArmaReqCombate = new NamespacedKey(plugin, "nexo_arma_req_combate");
        llaveArmaDanioBase = new NamespacedKey(plugin, "nexo_arma_danio_base");
        llaveArmaMitica = new NamespacedKey(plugin, "nexo_arma_mitica");
    }

    // ==========================================
    // ⚔️ FÁBRICA DATA-DRIVEN (ARMAS RPG 1.21)
    // ==========================================
    public static ItemStack generarArmaRPG(String id_yml) {
        WeaponDTO dto = pluginMemoria.getFileManager().getWeaponDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró el arma " + id_yml + " en caché!");
            return new ItemStack(Material.WOODEN_SWORD);
        }

        // Leer el material del YML
        String matString = pluginMemoria.getFileManager().getArmas().getString("armas_rpg." + id_yml + ".material", "IRON_SWORD");
        Material mat = Material.matchMaterial(matString);
        if (mat == null) mat = Material.IRON_SWORD;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        // Nombre y Color
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre()) + " §8[§e+0§8]");

        // Lore RPG
        lore.add("§7Clase: " + dto.claseRequerida());
        lore.add("§7Elemento: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.elemento()));
        lore.add("");
        lore.add("§7Daño Base: §c" + dto.danioBase() + " ⚔");
        lore.add("§7Velocidad: §e" + dto.velocidadAtaque() + " ⚡");
        lore.add("");

        if (!dto.habilidadId().equalsIgnoreCase("ninguna")) {
            lore.add("§6✦ Habilidad: §f" + dto.habilidadId().toUpperCase() + " §e§l(CLIC DERECHO)");
            lore.add("");
        }

        lore.add("§fRequisito de Combate: Nivel " + dto.nivelRequerido());
        meta.setLore(lore);
        meta.setUnbreakable(true);

        // 🚀 INYECCIÓN DE DATOS (PDC)
        meta.getPersistentDataContainer().set(llaveWeaponId, PersistentDataType.STRING, dto.id());
        meta.getPersistentDataContainer().set(llaveWeaponPrestige, PersistentDataType.INTEGER, 0); // Empieza en prestigio 0

        // ==========================================
        // ⚙️ MANIPULACIÓN DE ATRIBUTOS NATIVOS 1.21.11
        // ==========================================

        // 1. Daño
        NamespacedKey dmgKey = new NamespacedKey(pluginMemoria, "nexo_dmg_" + dto.id());
        org.bukkit.attribute.AttributeModifier dmgMod = new org.bukkit.attribute.AttributeModifier(
                dmgKey, dto.danioBase(), org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, dmgMod);

        // 2. Velocidad de Ataque (La base de Minecraft es 4.0. Si queremos 1.2, restamos 2.8)
        NamespacedKey spdKey = new NamespacedKey(pluginMemoria, "nexo_spd_" + dto.id());
        double speedOffset = dto.velocidadAtaque() - 4.0;
        org.bukkit.attribute.AttributeModifier spdMod = new org.bukkit.attribute.AttributeModifier(
                spdKey, speedOffset, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED, spdMod);

        // Ocultar atributos vanilla feos (El clásico "+7 Daño de ataque" verde de Minecraft)
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ⚙️ FÁBRICA DATA-DRIVEN (ARMADURAS PROFESIONES)
    // ==========================================
    public static ItemStack generarArmaduraProfesion(String id_yml, String tipoPieza) {
        ArmorDTO dto = pluginMemoria.getFileManager().getArmorDTO(id_yml);
        if (dto == null) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la armadura " + id_yml + " en caché!");
            return new ItemStack(Material.STONE);
        }

        String matString = pluginMemoria.getFileManager().getArmaduras().getString("armaduras_profesion." + id_yml + ".material", "LEATHER_CHESTPLATE");
        String prefijoMat = matString.contains("_") ? matString.split("_")[0] : matString;
        Material mat;
        try {
            mat = Material.valueOf(prefijoMat + "_" + tipoPieza.toUpperCase());
        } catch (Exception e) {
            mat = Material.LEATHER_CHESTPLATE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        String etiquetaPieza = "";
        switch (tipoPieza.toUpperCase()) {
            case "HELMET": etiquetaPieza = " §8(Casco)"; break;
            case "CHESTPLATE": etiquetaPieza = " §8(Peto)"; break;
            case "LEGGINGS": etiquetaPieza = " §8(Pantalones)"; break;
            case "BOOTS": etiquetaPieza = " §8(Botas)"; break;
        }
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', dto.nombre()) + etiquetaPieza);

        lore.add("§7Clase: " + dto.claseRequerida());
        lore.add("");

        if (dto.vidaExtra() > 0) lore.add("§7Vida Extra: §c+" + dto.vidaExtra() + " ❤");
        if (dto.velocidadMovimiento() > 0) lore.add("§7Velocidad: §f+" + dto.velocidadMovimiento() + " 🍃");
        if (dto.suerteMinera() > 0) lore.add("§7Fortuna Minera: §b+" + dto.suerteMinera() + "% ✨");
        if (dto.velocidadMineria() > 0) lore.add("§7Prisa Minera: §e+" + dto.velocidadMineria() + " ⚡");
        if (dto.suerteAgricola() > 0) lore.add("§7Fortuna Agrícola: §a+" + dto.suerteAgricola() + "% 🌾");
        if (dto.suerteTala() > 0) lore.add("§7Doble Caída (Tala): §2+" + dto.suerteTala() + "% 🪓");
        if (dto.criaturaMarina() > 0) lore.add("§7Prob. Criatura Marina: §3+" + dto.criaturaMarina() + "% 🦑");
        if (dto.velocidadPesca() > 0) lore.add("§7Velocidad Pesca: §9+" + dto.velocidadPesca() + "% 🎣");

        List<String> loreCustom = pluginMemoria.getFileManager().getArmaduras().getStringList("armaduras_profesion." + id_yml + ".lore_custom");
        if (loreCustom != null && !loreCustom.isEmpty()) {
            lore.add("");
            for (String linea : loreCustom) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', linea));
            }
        }

        lore.add("");
        lore.add("§fRequisito de " + dto.skillRequerida() + ": Nivel " + dto.nivelRequerido());

        meta.setLore(lore);
        meta.setUnbreakable(true);

        // 🚀 INYECCIÓN DEL ID MAESTRO
        meta.getPersistentDataContainer().set(llaveArmaduraId, PersistentDataType.STRING, dto.id());

        // Compatibilidad con sistemas viejos
        if (dto.vidaExtra() > 0) meta.getPersistentDataContainer().set(llaveVidaExtra, PersistentDataType.DOUBLE, dto.vidaExtra());

        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ⚙️ ARTEFACTOS Y MATERIALES
    // ==========================================
    public static ItemStack crearPolvoEstelar() {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e✨ Polvo Estelar");
        meta.getPersistentDataContainer().set(llaveMaterialMejora, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack crearHojaVacio() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d🌌 Hoja del Vacío");
        List<String> lore = new ArrayList<>();
        lore.add("§7Artefacto de Utilidad");
        lore.add("");
        lore.add("§eHabilidad: Transmisión Instantánea §e§l(CLIC DERECHO)");
        lore.add("§8Costo: §e40 Energía ⚡");
        lore.add("§c🔒 Ligado al Alma");
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(llaveSoulbound, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}