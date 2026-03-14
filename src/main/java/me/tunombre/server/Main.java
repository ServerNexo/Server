package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager; // El cerebro de los YML

    public HashMap<UUID, Integer> nexoNiveles = new HashMap<>();
    public HashMap<UUID, Integer> nexoXp = new HashMap<>();
    public HashMap<UUID, Integer> combateNiveles = new HashMap<>();
    public HashMap<UUID, Integer> combateXp = new HashMap<>();
    public HashMap<UUID, Integer> mineriaNiveles = new HashMap<>();
    public HashMap<UUID, Integer> mineriaXp = new HashMap<>();
    public HashMap<UUID, Integer> agriculturaNiveles = new HashMap<>();
    public HashMap<UUID, Integer> agriculturaXp = new HashMap<>();
    public HashMap<UUID, Integer> energiaMineria = new HashMap<>();
    // --- RAM: MECÁNICAS DE JUEGO ---
    public HashMap<UUID, Integer> manaJugador = new HashMap<>(); // ¡NUEVO MOTOR DE MANÁ!
    public HashMap<UUID, String> claseJugador = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this); // <-- Iniciamos el FileManager
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();
        ItemManager.init(this);

        getCommand("test").setExecutor(new ComandoTest(this));
        getCommand("nexo").setExecutor(new ComandoNexo(this));
        getCommand("desguace").setExecutor(new ComandoDesguace(this));

        getServer().getPluginManager().registerEvents(new DesguaceListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new HerreriaListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this); // Registrado 1 sola vez
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);

        getServer().getPluginManager().registerEvents(new ItemRequirementListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // ==========================================
        // RELOJ UNIFICADO: REGENERACIÓN Y HUD (Cada 1 segundo / 20 ticks)
        // ==========================================
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // 1. Cálculos de Energía (Movimiento/Artefactos)
                int nivelNexo = nexoNiveles.getOrDefault(id, 1);
                int maxEnergia = 100 + ((nivelNexo - 1) * 20);
                int energiaActual = energiaMineria.getOrDefault(id, maxEnergia);

                // Regenera 5 de energía por segundo
                if (energiaActual < maxEnergia) {
                    energiaMineria.put(id, Math.min(energiaActual + 5, maxEnergia));
                    energiaActual = Math.min(energiaActual + 5, maxEnergia);
                }

                // 2. Cálculos de Maná (Combate/Hechizos)
                int nivelCombate = combateNiveles.getOrDefault(id, 1);
                int maxMana = 100 + (nivelCombate * 10);
                int manaActual = manaJugador.getOrDefault(id, maxMana);

                // Regenera 15 de maná por segundo
                if (manaActual < maxMana) {
                    manaJugador.put(id, Math.min(manaActual + 15, maxMana));
                    manaActual = Math.min(manaActual + 15, maxMana);
                }

                // 3. Dibujar el HUD en la Action Bar constantemente
                String hud = "§b💧 Maná: " + manaActual + "/" + maxMana + "   §e⚡ Energía: " + energiaActual + "/" + maxEnergia;
                p.sendActionBar(hud);
            }
        }, 20L, 20L); // El "20L" significa que corre 1 vez por segundo

        // Reloj de Armaduras (Cada 10 ticks)
        new ArmorTask(this).runTaskTimer(this, 10L, 10L);

        getLogger().info("¡Nexo Core V3: Sistema de Clases y Elementos Activado!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.desconectar();
    }

    // ==========================================
    // PUENTES DE COMUNICACIÓN (GETTERS)
    // ==========================================
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FileManager getFileManager() { // <-- ¡AQUÍ ESTÁ EL GETTER QUE FALTABA!
        return fileManager;
    }

    public void darCombateXp(Player player, int cantidad) {
        UUID uuid = player.getUniqueId();
        int nivel = combateNiveles.getOrDefault(uuid, 1);
        int xp = combateXp.getOrDefault(uuid, 0) + cantidad;
        while (xp >= (nivel * 100)) {
            xp -= (nivel * 100);
            nivel++;
            player.sendTitle("§c§l¡COMBATE NIVEL " + nivel + "!", "§7Tus instintos mejoran...", 10, 70, 20);
        }
        combateNiveles.put(uuid, nivel);
        combateXp.put(uuid, xp);
        player.sendActionBar("§c⚔ +" + cantidad + " XP §8(§7" + xp + "/" + (nivel * 100) + "§8)");
    }

    public void darNexoXp(Player player, int cantidad) {
        UUID uuid = player.getUniqueId();
        int nivel = nexoNiveles.getOrDefault(uuid, 1);
        int xp = nexoXp.getOrDefault(uuid, 0) + cantidad;
        while (xp >= (nivel * 100)) {
            xp -= (nivel * 100);
            nivel++;
            player.sendTitle("§e§l¡NEXO NIVEL " + nivel + "!", "§fHas ascendido", 10, 70, 20);
        }
        nexoNiveles.put(uuid, nivel);
        nexoXp.put(uuid, xp);
    }
}