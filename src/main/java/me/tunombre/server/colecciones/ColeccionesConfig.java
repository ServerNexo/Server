package me.tunombre.server.colecciones;

import me.tunombre.server.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

// ❌ Fíjate que NO dice "extends JavaPlugin"
public class ColeccionesConfig {

    private final Main plugin;
    private FileConfiguration config;
    private File configFile;

    // ✅ Pedimos prestado el Main (Tu motor) a través del constructor
    public ColeccionesConfig(Main plugin) {
        this.plugin = plugin;
        crearConfig();
    }

    public void crearConfig() {
        // ❌ Le quitamos la palabra "File" al inicio
        configFile = new File(plugin.getDataFolder(), "colecciones.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("colecciones.yml", false);
        }

        // ❌ Le quitamos la palabra "FileConfiguration" al inicio
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
}