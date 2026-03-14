package me.tunombre.server;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    public static NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound;

    // Antiguas Profesiones (Las conservamos por si acaso)
    public static NamespacedKey llaveSuerteMinera, llaveVelocidadMineria, llaveSuerteAgricola, llaveVelocidadMovimiento, llaveSuerteTala, llaveFuerzaHacha, llaveVelocidadPesca, llaveCriaturaMarina;

    // 🚀 NUEVAS LLAVES OPTIMIZADAS
    public static NamespacedKey llaveArmaduraId; // Conecta con el ArmorDTO
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

        // Nuevas
        llaveArmaduraId = new NamespacedKey(plugin, "nexo_armadura_id");
        llaveArmaClase = new NamespacedKey(plugin, "nexo_arma_clase");
        llaveArmaReqCombate = new NamespacedKey(plugin, "nexo_arma_req_combate");
        llaveArmaDanioBase = new NamespacedKey(plugin, "nexo_arma_danio_base");
        llaveArmaMitica = new NamespacedKey(plugin, "nexo_arma_mitica");
    }

    private static int getNivelRequerido(int tier) {
        switch (tier) {
            case 1: return 1; case 2: return 10; case 3: return 25;
            case 4: return 40; case 5: return 50; default: return 1;
        }
    }

    private static String getRarezaYColor(int tier) {
        switch (tier) {
            case 1: return "§7COMÚN"; case 2: return "§bRARO"; case 3: return "§eÉPICO";
            case 4: return "§4MÍTICO"; case 5: return "§6LEGENDARIO"; default: return "§7COMÚN";
        }
    }

    // ==========================================
    // FÁBRICA DE ARMAS (AHORA USA PDC)
    // ==========================================
    public static ItemStack generarArma(String clase, String elemento, int tier) {
        Material mat = Material.WOODEN_SWORD;
        int dañoBase = 10 * tier * tier;
        String nombre = "Arma";

        switch (clase.toUpperCase()) {
            case "MAGO":    mat = Material.BLAZE_ROD; nombre = "Báculo"; break;
            case "TANQUE":  mat = Material.MACE; nombre = "Maza Pesada"; break;
            case "PALADIN": mat = Material.TRIDENT; nombre = "Lanza Sagrada"; break;
            case "ARQUERO": mat = Material.BOW; nombre = "Arco Largo"; break;
            default:        mat = Material.IRON_SWORD; nombre = "Espada"; break;
        }

        String colorElemento = "§f"; String iconoElemento = "";
        switch (elemento.toUpperCase()) {
            case "FUEGO":  colorElemento = "§c"; iconoElemento = "🔥"; nombre += " Ígneo"; break;
            case "HIELO":  colorElemento = "§b"; iconoElemento = "❄"; nombre += " Glacial"; break;
            case "RAYO":   colorElemento = "§e"; iconoElemento = "⚡"; nombre += " del Trueno"; break;
            case "VENENO": colorElemento = "§2"; iconoElemento = "☠"; nombre += " Tóxico"; break;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        lore.add("§7Clase: " + clase);
        lore.add("§7Daño Base: §c" + dañoBase);
        lore.add("§7Elemento: " + colorElemento + elemento + " " + iconoElemento);
        lore.add("");
        lore.add("§fRequisito de Combate: Nivel " + getNivelRequerido(tier));
        lore.add("");
        lore.add(getRarezaYColor(tier));

        meta.setLore(lore);
        meta.setDisplayName(colorElemento + nombre + " §8[§e+0§8]");
        meta.setUnbreakable(true);

        // 🚀 INYECCIÓN DE DATOS SEGUROS
        meta.getPersistentDataContainer().set(llaveNivelMejora, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(llaveElemento, PersistentDataType.STRING, elemento.toUpperCase());
        meta.getPersistentDataContainer().set(llaveArmaClase, PersistentDataType.STRING, clase);
        meta.getPersistentDataContainer().set(llaveArmaDanioBase, PersistentDataType.DOUBLE, (double) dañoBase);
        meta.getPersistentDataContainer().set(llaveArmaReqCombate, PersistentDataType.INTEGER, getNivelRequerido(tier));

        if (tier >= 4) {
            meta.getPersistentDataContainer().set(llaveArmaMitica, PersistentDataType.BYTE, (byte) 1);
        }

        item.setItemMeta(meta);
        return item;
    }

    // (generarArmadura normal queda intacto por ahora)
    public static ItemStack generarArmadura(String clase, String elemento, int tier) {
        Material mat = Material.LEATHER_CHESTPLATE;
        double vidaExtra = 5.0 * tier;
        String nombre = "Armadura";

        // ... (Tu código actual de generarArmadura)
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre + " §8[§e+0§8]");
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(llaveVidaExtra, PersistentDataType.DOUBLE, vidaExtra);
        meta.getPersistentDataContainer().set(llaveNivelMejora, PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ⚙️ FÁBRICA DATA-DRIVEN (Profesiones)
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

        // Mantenemos las antiguas por si hay plugins de terceros, pero nosotros usaremos el ID
        if (dto.vidaExtra() > 0) meta.getPersistentDataContainer().set(llaveVidaExtra, PersistentDataType.DOUBLE, dto.vidaExtra());

        item.setItemMeta(meta);
        return item;
    }

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