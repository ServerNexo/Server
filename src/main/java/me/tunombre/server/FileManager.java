package me.tunombre.server;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private final Main plugin;

    private FileConfiguration artefactosConfig, armadurasConfig, armasConfig, herramientasConfig, reforjasConfig, encantamientosConfig;
    private File artefactosFile, armadurasFile, armasFile, herramientasFile, reforjasFile, encantamientosFile;

    public FileManager(Main plugin) {
        this.plugin = plugin;
        cargarArchivos();
    }

    public void cargarArchivos() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // 1. Artefactos
        artefactosFile = new File(plugin.getDataFolder(), "artefactos.yml");
        if (!artefactosFile.exists()) plugin.saveResource("artefactos.yml", false);
        artefactosConfig = YamlConfiguration.loadConfiguration(artefactosFile);

        // 2. Armaduras
        armadurasFile = new File(plugin.getDataFolder(), "armaduras.yml");
        if (!armadurasFile.exists()) plugin.saveResource("armaduras.yml", false);
        armadurasConfig = YamlConfiguration.loadConfiguration(armadurasFile);

        // 3. Armas
        armasFile = new File(plugin.getDataFolder(), "armas.yml");
        if (!armasFile.exists()) plugin.saveResource("armas.yml", false);
        armasConfig = YamlConfiguration.loadConfiguration(armasFile);

        // 4. Herramientas
        herramientasFile = new File(plugin.getDataFolder(), "herramientas.yml");
        if (!herramientasFile.exists()) plugin.saveResource("herramientas.yml", false);
        herramientasConfig = YamlConfiguration.loadConfiguration(herramientasFile);

        // 5. Reforjas
        reforjasFile = new File(plugin.getDataFolder(), "reforjas.yml");
        if (!reforjasFile.exists()) plugin.saveResource("reforjas.yml", false);
        reforjasConfig = YamlConfiguration.loadConfiguration(reforjasFile);

        // 6. Encantamientos
        encantamientosFile = new File(plugin.getDataFolder(), "encantamientos.yml");
        if (!encantamientosFile.exists()) plugin.saveResource("encantamientos.yml", false);
        encantamientosConfig = YamlConfiguration.loadConfiguration(encantamientosFile);
    }

    // GETTERS PARA LEER LOS DATOS
    public FileConfiguration getArtefactos() { return artefactosConfig; }
    public FileConfiguration getArmaduras() { return armadurasConfig; }
    public FileConfiguration getArmas() { return armasConfig; }
    public FileConfiguration getHerramientas() { return herramientasConfig; }
    public FileConfiguration getReforjas() { return reforjasConfig; }
    public FileConfiguration getEncantamientos() { return encantamientosConfig; }
}