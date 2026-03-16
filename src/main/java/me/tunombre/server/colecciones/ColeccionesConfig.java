package me.tunombre.server.colecciones;

import me.tunombre.server.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColeccionesConfig {
    private final Main plugin;
    private File file;
    private FileConfiguration config;

    // Mapas para buscar rápidamente si un bloque o mob está configurado
    private final Map<String, ConfigurationSection> colecciones = new HashMap<>();
    private final Map<String, ConfigurationSection> slayers = new HashMap<>();

    public ColeccionesConfig(Main plugin) {
        this.plugin = plugin;
        cargarConfiguracion();
    }

    public void cargarConfiguracion() {
        file = new File(plugin.getDataFolder(), "colecciones.yml");
        if (!file.exists()) {
            plugin.saveResource("colecciones.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        colecciones.clear();
        slayers.clear();

        ConfigurationSection colSec = config.getConfigurationSection("colecciones");
        if (colSec != null) {
            for (String key : colSec.getKeys(false)) {
                colecciones.put(key.toLowerCase(), colSec.getConfigurationSection(key));
            }
        }

        ConfigurationSection slaySec = config.getConfigurationSection("slayers");
        if (slaySec != null) {
            for (String key : slaySec.getKeys(false)) {
                slayers.put(key.toLowerCase(), slaySec.getConfigurationSection(key));
            }
        }
    }

    public boolean esColeccion(String id) { return colecciones.containsKey(id.toLowerCase()); }
    public boolean esSlayer(String id) { return slayers.containsKey(id.toLowerCase()); }

    public ConfigurationSection getDatosColeccion(String id) { return colecciones.get(id.toLowerCase()); }
    public ConfigurationSection getDatosSlayer(String id) { return slayers.get(id.toLowerCase()); }
}