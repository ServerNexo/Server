package me.tunombre.server.colecciones;

import me.tunombre.server.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ColeccionesConfig {

    private final Main plugin;
    private FileConfiguration config;
    private File configFile;

    public ColeccionesConfig(Main plugin) {
        this.plugin = plugin;
        crearConfig();
    }

    public void crearConfig() {
        configFile = new File(plugin.getDataFolder(), "colecciones.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("colecciones.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void guardarConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("¡No se pudo guardar el archivo colecciones.yml!");
        }
    }

    public void recargarConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // ==========================================================
    // 🔍 MÉTODOS DE LECTURA (¡Esto quita las líneas rojas!)
    // ==========================================================

    // Revisa si un bloque está en la lista de colecciones
    public boolean esColeccion(String id) {
        return config.contains("colecciones." + id);
    }

    // Obtiene toda la información de una colección (niveles, recompensas)
    public ConfigurationSection getDatosColeccion(String id) {
        return config.getConfigurationSection("colecciones." + id);
    }

    // Revisa si un mob está en la lista de Slayers
    public boolean esSlayer(String id) {
        return config.contains("slayers." + id);
    }

    // Obtiene toda la información de un Slayer
    public ConfigurationSection getDatosSlayer(String id) {
        return config.getConfigurationSection("slayers." + id);
    }
}