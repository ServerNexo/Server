package me.tunombre.server;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    // Llaves de Combate
    public static NamespacedKey llaveNivelMejora, llaveMaterialMejora, llaveVidaExtra, llaveElemento, llaveSoulbound;

    // ¡NUEVAS! Llaves de Profesiones
    public static NamespacedKey llaveSuerteMinera, llaveVelocidadMineria;
    public static NamespacedKey llaveSuerteAgricola, llaveVelocidadMovimiento;
    public static NamespacedKey llaveSuerteTala, llaveFuerzaHacha;
    public static NamespacedKey llaveVelocidadPesca, llaveCriaturaMarina;
    public static Main pluginMemoria;

    public static void init(Main plugin) {
        // (Tus llaves anteriores siguen aquí...)
        pluginMemoria = plugin; // Guardamos el plugin para poder leer los YML
        llaveNivelMejora = new NamespacedKey(plugin, "nexo_upgrade");
        llaveMaterialMejora = new NamespacedKey(plugin, "nexo_material_polvo"); // <--- ¡ESTA ES LA FUGITIVA!
        llaveVidaExtra = new NamespacedKey(plugin, "nexo_vida_extra");
        llaveElemento = new NamespacedKey(plugin, "nexo_elemento");
        llaveSoulbound = new NamespacedKey(plugin, "nexo_soulbound");

        // Inicializamos las nuevas llaves de recolección
        llaveSuerteMinera = new NamespacedKey(plugin, "nexo_suerte_minera");
        llaveVelocidadMineria = new NamespacedKey(plugin, "nexo_velocidad_mineria");
        llaveSuerteAgricola = new NamespacedKey(plugin, "nexo_suerte_agricola");
        llaveVelocidadMovimiento = new NamespacedKey(plugin, "nexo_velocidad_movimiento");
        llaveSuerteTala = new NamespacedKey(plugin, "nexo_suerte_tala");
        llaveFuerzaHacha = new NamespacedKey(plugin, "nexo_fuerza_hacha");
        llaveVelocidadPesca = new NamespacedKey(plugin, "nexo_velocidad_pesca");
        llaveCriaturaMarina = new NamespacedKey(plugin, "nexo_criatura_marina");
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
    // FÁBRICA DE ARMAS
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

        // 🛡️ INDESTRUCTIBILIDAD INYECTADA
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(llaveNivelMejora, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(llaveElemento, PersistentDataType.STRING, elemento.toUpperCase());
        item.setItemMeta(meta);

        return item;
    }

    // ==========================================
    // FÁBRICA DE ARMADURAS
    // ==========================================
    public static ItemStack generarArmadura(String clase, String elemento, int tier) {
        Material mat = Material.LEATHER_CHESTPLATE;
        double vidaExtra = 5.0 * tier;
        String nombre = "Armadura";

        switch (clase.toUpperCase()) {
            case "MAGO":    mat = Material.LEATHER_CHESTPLATE; nombre = "Túnica"; vidaExtra *= 0.5; break;
            case "TANQUE":  mat = Material.NETHERITE_CHESTPLATE; nombre = "Pechera"; vidaExtra *= 1.5; break;
            case "PALADIN": mat = Material.DIAMOND_CHESTPLATE; nombre = "Égida"; break;
            case "ARQUERO": mat = Material.CHAINMAIL_CHESTPLATE; nombre = "Jubón"; vidaExtra *= 0.8; break;
            default:        mat = Material.IRON_CHESTPLATE; nombre = "Coraza"; break;
        }

        String colorElemento = "§f";
        switch (elemento.toUpperCase()) {
            case "FUEGO":  colorElemento = "§c"; nombre += " de las Llamas"; break;
            case "HIELO":  colorElemento = "§b"; nombre += " de la Escarcha"; break;
            case "RAYO":   colorElemento = "§e"; nombre += " de la Tormenta"; break;
            case "VENENO": colorElemento = "§2"; nombre += " de la Viuda"; break;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        lore.add("§7Clase: " + clase);
        lore.add("§7Resistencia a: " + colorElemento + elemento);
        lore.add("§7Vida Extra: §c+" + vidaExtra + " ❤");
        lore.add("");
        lore.add("§fRequisito de Combate: Nivel " + getNivelRequerido(tier));
        lore.add("");
        lore.add(getRarezaYColor(tier));

        meta.setLore(lore);
        meta.setDisplayName(colorElemento + nombre + " §8[§e+0§8]");

        // 🛡️ INDESTRUCTIBILIDAD INYECTADA
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
        String ruta = "armaduras_profesion." + id_yml;
        if (pluginMemoria == null || pluginMemoria.getFileManager() == null || !pluginMemoria.getFileManager().getArmaduras().contains(ruta)) {
            org.bukkit.Bukkit.getLogger().warning("¡No se encontró la armadura " + id_yml + " en armaduras.yml!");
            return new ItemStack(Material.STONE);
        }

        // 1. Datos Básicos
        String nombre = org.bukkit.ChatColor.translateAlternateColorCodes('&', pluginMemoria.getFileManager().getArmaduras().getString(ruta + ".nombre"));
        String clase = pluginMemoria.getFileManager().getArmaduras().getString(ruta + ".clase", "Aventurero");
        String reqSkill = pluginMemoria.getFileManager().getArmaduras().getString(ruta + ".requisito_skill", "Ninguna");
        int reqNivel = pluginMemoria.getFileManager().getArmaduras().getInt(ruta + ".nivel_requerido", 1);

        // 2. Sistema de Recorte de Material (Ej: IRON_CHESTPLATE -> IRON_HELMET)
        String matString = pluginMemoria.getFileManager().getArmaduras().getString(ruta + ".material", "LEATHER_CHESTPLATE");
        String prefijoMat = matString.contains("_") ? matString.split("_")[0] : matString;
        Material mat;
        try {
            mat = Material.valueOf(prefijoMat + "_" + tipoPieza.toUpperCase());
        } catch (Exception e) {
            mat = Material.LEATHER_CHESTPLATE;
        }

        // 3. Extracción de TODAS las Estadísticas (Las 4 profesiones)
        double vidaExtra = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".vida_extra", 0.0);
        double velMovimiento = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".velocidad_movimiento", 0.0);
        double suerteMinera = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".suerte_minera", 0.0);
        double velMineria = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".velocidad_mineria", 0.0);
        double suerteAgricola = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".suerte_agricola", 0.0);
        double suerteTala = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".suerte_tala", 0.0);
        double velPesca = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".velocidad_pesca", 0.0);
        double criaturaMarina = pluginMemoria.getFileManager().getArmaduras().getDouble(ruta + ".criatura_marina", 0.0);
        List<String> loreCustom = pluginMemoria.getFileManager().getArmaduras().getStringList(ruta + ".lore_custom");

        // 4. Construcción del Ítem
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        // Etiqueta visual para el nombre según la pieza
        String etiquetaPieza = "";
        switch (tipoPieza.toUpperCase()) {
            case "HELMET": etiquetaPieza = " §8(Casco)"; break;
            case "CHESTPLATE": etiquetaPieza = " §8(Peto)"; break;
            case "LEGGINGS": etiquetaPieza = " §8(Pantalones)"; break;
            case "BOOTS": etiquetaPieza = " §8(Botas)"; break;
        }
        meta.setDisplayName(nombre + etiquetaPieza);

        lore.add("§7Clase: " + clase);
        lore.add("");

        // 5. Pintamos las estadísticas en el Lore (Solo si son mayores a 0)
        if (vidaExtra > 0) lore.add("§7Vida Extra: §c+" + vidaExtra + " ❤");
        if (velMovimiento > 0) lore.add("§7Velocidad: §f+" + velMovimiento + " 🍃");
        if (suerteMinera > 0) lore.add("§7Fortuna Minera: §b+" + suerteMinera + "% ✨");
        if (velMineria > 0) lore.add("§7Prisa Minera: §e+" + velMineria + " ⚡");
        if (suerteAgricola > 0) lore.add("§7Fortuna Agrícola: §a+" + suerteAgricola + "% 🌾");
        if (suerteTala > 0) lore.add("§7Doble Caída (Tala): §2+" + suerteTala + "% 🪓");
        if (criaturaMarina > 0) lore.add("§7Prob. Criatura Marina: §3+" + criaturaMarina + "% 🦑");
        if (velPesca > 0) lore.add("§7Velocidad Pesca: §9+" + velPesca + "% 🎣");

        // Pintamos el Lore Custom si existe (Las pasivas)
        if (loreCustom != null && !loreCustom.isEmpty()) {
            lore.add("");
            for (String linea : loreCustom) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', linea));
            }
        }

        lore.add("");
        lore.add("§fRequisito de " + reqSkill + ": Nivel " + reqNivel);

        meta.setLore(lore);
        meta.setUnbreakable(true); // ¡Indestructible!

        // 6. Inyectamos los datos invisibles (NBT) para que el servidor los detecte y aplique efectos
        if (vidaExtra > 0) meta.getPersistentDataContainer().set(llaveVidaExtra, PersistentDataType.DOUBLE, vidaExtra);
        if (velMovimiento > 0) meta.getPersistentDataContainer().set(llaveVelocidadMovimiento, PersistentDataType.DOUBLE, velMovimiento);
        if (suerteMinera > 0) meta.getPersistentDataContainer().set(llaveSuerteMinera, PersistentDataType.DOUBLE, suerteMinera);
        if (velMineria > 0) meta.getPersistentDataContainer().set(llaveVelocidadMineria, PersistentDataType.DOUBLE, velMineria);
        if (suerteAgricola > 0) meta.getPersistentDataContainer().set(llaveSuerteAgricola, PersistentDataType.DOUBLE, suerteAgricola);
        if (suerteTala > 0) meta.getPersistentDataContainer().set(llaveSuerteTala, PersistentDataType.DOUBLE, suerteTala);
        if (criaturaMarina > 0) meta.getPersistentDataContainer().set(llaveCriaturaMarina, PersistentDataType.DOUBLE, criaturaMarina);
        if (velPesca > 0) meta.getPersistentDataContainer().set(llaveVelocidadPesca, PersistentDataType.DOUBLE, velPesca);

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

    // ==========================================
    // ARTEFACTOS DE UTILIDAD
    // ==========================================
    public static ItemStack crearHojaVacio() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d🌌 Hoja del Vacío");

        List<String> lore = new ArrayList<>();
        lore.add("§7Artefacto de Utilidad");
        lore.add("");
        lore.add("§eHabilidad: Transmisión Instantánea §e§l(CLIC DERECHO)");
        lore.add("§7Te teletransporta 8 bloques hacia");
        lore.add("§7adelante atravesando el espacio.");
        lore.add("§8Costo: §e40 Energía ⚡");
        lore.add("");
        lore.add("§c🔒 Ligado al Alma"); // Etiqueta visual
        lore.add("§d§lRELIQUIA ÉPICA");

        meta.setLore(lore);

        // 🛡️ INDESTRUCTIBILIDAD INYECTADA
        meta.setUnbreakable(true);

        // 🔒 SOULBOUND INYECTADO (Nadie puede tirar esto al piso)
        meta.getPersistentDataContainer().set(llaveSoulbound, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

}