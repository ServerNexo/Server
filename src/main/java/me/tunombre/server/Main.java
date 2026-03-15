package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager;

    // 🛡️ RAM: Variables concurrentes seguras
    public ConcurrentHashMap<UUID, Integer> nexoNiveles = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> nexoXp = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> combateNiveles = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> combateXp = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> mineriaNiveles = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> mineriaXp = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> agriculturaNiveles = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> agriculturaXp = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> energiaMineria = new ConcurrentHashMap<>();

    // 💧 MANÁ ELIMINADO DE LA RAM: Ahora lo controla AuraSkills directamente.
    public ConcurrentHashMap<UUID, String> claseJugador = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
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
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new ReforjaListener(this), this);
        getServer().getPluginManager().registerEvents(new YunqueListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // 1. Energía (Sistema propio de recolección y artefactos)
                int nivelNexo = nexoNiveles.getOrDefault(id, 1);
                int maxEnergia = 100 + ((nivelNexo - 1) * 20);
                int energiaActual = energiaMineria.getOrDefault(id, maxEnergia);

                if (energiaActual < maxEnergia) {
                    energiaMineria.put(id, Math.min(energiaActual + 5, maxEnergia));
                    energiaActual = Math.min(energiaActual + 5, maxEnergia);
                }

                // 2. LEER MANÁ DIRECTO DE AURASKILLS
                int manaActual = 0;
                int maxMana = 0;
                try {
                    dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(id);
                    if (user != null) {
                        manaActual = (int) user.getMana();
                        maxMana = (int) user.getMaxMana();
                    }
                } catch (Exception ignored) {}

                // 3. Obtenemos la vida real del jugador para mostrarla
                int hpActual = (int) Math.ceil(p.getHealth());
                int hpMax = (int) p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                // 4. Dibujar el HUD completo en pantalla
                String hud = "§c❤ " + hpActual + "/" + hpMax + "  §b💧 " + manaActual + "/" + maxMana + "  §e⚡ " + energiaActual + "/" + maxEnergia;
                p.sendActionBar(hud);
            }
        }, 20L, 20L);

        new ArmorTask(this).runTaskTimer(this, 10L, 10L);

        getLogger().info("¡Nexo Core V3: Integración de Maná de AuraSkills Completada!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.desconectar();
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FileManager getFileManager() { return fileManager; }

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
        player.sendMessage("§c⚔ +" + cantidad + " XP §8(§7" + xp + "/" + (nivel * 100) + "§8)");
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