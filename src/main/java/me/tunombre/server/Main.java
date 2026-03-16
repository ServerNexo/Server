package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager;

    // Motores Públicos
    public me.tunombre.server.minigames.CombatComboManager combatComboManager;
    public me.tunombre.server.pvp.PvPManager pvpManager;

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

    // ⚡ NUEVO: Energía extra otorgada por los Accesorios
    public ConcurrentHashMap<UUID, Integer> energiaExtraAccesorios = new ConcurrentHashMap<>();

    // 💧 MANÁ ELIMINADO DE LA RAM: Ahora lo controla AuraSkills directamente.
    public ConcurrentHashMap<UUID, String> claseJugador = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();
        ItemManager.init(this);

        // Comandos base
        getCommand("test").setExecutor(new ComandoTest(this));
        getCommand("nexo").setExecutor(new ComandoNexo(this));
        getCommand("desguace").setExecutor(new ComandoDesguace(this));

        // Listeners base
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

        // ==========================================
        // 🪄 SISTEMA DE ARTEFACTOS
        // ==========================================
        me.tunombre.server.artefactos.ArtefactoManager artefactoManager = new me.tunombre.server.artefactos.ArtefactoManager(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.artefactos.ArtefactoListener(this, artefactoManager), this);

        // ==========================================
        // 🎒 SISTEMA DE MOCHILAS
        // ==========================================
        me.tunombre.server.mochilas.MochilaManager mochilaManager = new me.tunombre.server.mochilas.MochilaManager(this);
        if (getCommand("pv") != null) {
            getCommand("pv").setExecutor(new me.tunombre.server.mochilas.ComandoPV(mochilaManager));
        }
        getServer().getPluginManager().registerEvents(new me.tunombre.server.mochilas.MochilaListener(mochilaManager), this);

        // ==========================================
        // 👕 SISTEMA DE GUARDARROPA
        // ==========================================
        me.tunombre.server.guardarropa.GuardarropaManager guardarropaManager = new me.tunombre.server.guardarropa.GuardarropaManager(this);
        me.tunombre.server.guardarropa.GuardarropaListener guardarropaListener = new me.tunombre.server.guardarropa.GuardarropaListener(guardarropaManager);
        if (getCommand("wardrobe") != null) {
            getCommand("wardrobe").setExecutor(new me.tunombre.server.guardarropa.ComandoWardrobe(guardarropaListener));
        }
        getServer().getPluginManager().registerEvents(guardarropaListener, this);

        // ==========================================
        // 🌟 MOTOR DE HABILIDADES PASIVAS (Core 7)
        // ==========================================
        me.tunombre.server.pasivas.PasivasManager pasivasManager = new me.tunombre.server.pasivas.PasivasManager(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pasivas.PasivasListener(this, pasivasManager), this);

        // ==========================================
        // 🎮 MOTOR DE MINIJUEGOS
        // ==========================================
        this.combatComboManager = new me.tunombre.server.minigames.CombatComboManager(this);
        getServer().getPluginManager().registerEvents(this.combatComboManager, this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.MiningMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.FishingHookManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.WoodcuttingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.FarmingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.AlchemyMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.EnchantingMinigameManager(this), this);

        // ==========================================
        // 💍 SISTEMA DE ACCESORIOS Y NEXO-POWER
        // ==========================================
        me.tunombre.server.accesorios.AccesoriosManager accesoriosManager = new me.tunombre.server.accesorios.AccesoriosManager(this);
        if (getCommand("accesorios") != null) {
            getCommand("accesorios").setExecutor(new me.tunombre.server.accesorios.ComandoAccesorios(accesoriosManager));
        }
        getServer().getPluginManager().registerEvents(new me.tunombre.server.accesorios.AccesoriosListener(this, accesoriosManager), this);

        // ==========================================
        // ⚔️ MOTOR DE PVP Y HONOR (BOUNTY SYSTEM)
        // ==========================================
        this.pvpManager = new me.tunombre.server.pvp.PvPManager(this);
        if (getCommand("pvp") != null) {
            getCommand("pvp").setExecutor(new me.tunombre.server.pvp.ComandoPvP(this.pvpManager));
        }
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pvp.PvPListener(this.pvpManager), this);

        // Integración PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // Tarea del HUD (Energía, Maná y Vida)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                // 1. Energía (Ahora suma la energía extra de los accesorios)
                int nivelNexo = nexoNiveles.getOrDefault(id, 1);
                int maxEnergia = 100 + ((nivelNexo - 1) * 20) + energiaExtraAccesorios.getOrDefault(id, 0);
                int energiaActual = energiaMineria.getOrDefault(id, maxEnergia);

                if (energiaActual < maxEnergia) {
                    energiaMineria.put(id, Math.min(energiaActual + 5, maxEnergia));
                    energiaActual = Math.min(energiaActual + 5, maxEnergia);
                }

                // 2. LEER MANÁ DE AURASKILLS
                int manaActual = 0;
                int maxMana = 0;
                try {
                    dev.aurelium.auraskills.api.user.SkillsUser user = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(id);
                    if (user != null) {
                        manaActual = (int) user.getMana();
                        maxMana = (int) user.getMaxMana();
                    }
                } catch (Exception ignored) {}

                // 3. Vida
                int hpActual = (int) Math.ceil(p.getHealth());
                int hpMax = (int) p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                // 4. Dibujar HUD
                String hud = "§c❤ " + hpActual + "/" + hpMax + "  §b💧 " + manaActual + "/" + maxMana + "  §e⚡ " + energiaActual + "/" + maxEnergia;

                // Indicador de Frenesí
                if (combatComboManager != null && combatComboManager.enFrenesi.containsKey(id)) {
                    hud = "§4§l[FRENESÍ ACTIVO] " + hud;
                }

                // Indicador de Combate PvP
                if (pvpManager != null && pvpManager.estaEnCombate(p)) {
                    hud = "§c⚔ §l¡EN COMBATE! §r" + hud;
                }

                p.sendActionBar(hud);
            }
        }, 20L, 20L);

        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);

        getLogger().info("¡Nexo Core V8.1: Core Optimizado y Parcheado!");
    }

    @Override
    public void onDisable() {
        // 🚨 FORZAR GUARDADO DE TODOS LOS JUGADORES ONLINE ANTES DE APAGAR 🚨
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (databaseManager != null) {
                databaseManager.guardarJugador(p);
            }
        }

        if (databaseManager != null) databaseManager.desconectar();

        BlockBreakListener.restaurarBloquesRotos();
        getLogger().info("Bloques del Nexo restaurados exitosamente y datos guardados.");
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