package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
// import java.util.concurrent.ConcurrentHashMap; (Ya no necesitamos esto aquí)

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager;

    // 🌟 NUEVO: El Cerebro del Servidor
    private me.tunombre.server.user.UserManager userManager;
    private me.tunombre.server.user.NexoAPI nexoAPI;

    private me.tunombre.server.colecciones.ColeccionesConfig coleccionesConfig;

    // Motores Públicos
    public me.tunombre.server.minigames.CombatComboManager combatComboManager;
    public me.tunombre.server.pvp.PvPManager pvpManager;

    // 🛡️ RAM: Variables concurrentes seguras (ELIMINADAS)

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();

        // 🌟 INICIALIZAR EL CEREBRO
        this.userManager = new me.tunombre.server.user.UserManager();
        this.nexoAPI = new me.tunombre.server.user.NexoAPI(this.userManager);

        ItemManager.init(this);

        this.coleccionesConfig = new me.tunombre.server.colecciones.ColeccionesConfig(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.colecciones.ColeccionesListener(this), this);
        new me.tunombre.server.colecciones.FlushTask(databaseManager.getDataSource()).runTaskTimerAsynchronously(this, 12000L, 12000L);

        if (getCommand("test") != null) getCommand("test").setExecutor(new ComandoTest(this));
        if (getCommand("nexocore") != null) getCommand("nexocore").setExecutor(new ComandoNexo(this));
        if (getCommand("desguace") != null) getCommand("desguace").setExecutor(new ComandoDesguace(this));

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

        me.tunombre.server.artefactos.ArtefactoManager artefactoManager = new me.tunombre.server.artefactos.ArtefactoManager(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.artefactos.ArtefactoListener(this, artefactoManager), this);

        me.tunombre.server.mochilas.MochilaManager mochilaManager = new me.tunombre.server.mochilas.MochilaManager(this);
        if (getCommand("pv") != null) getCommand("pv").setExecutor(new me.tunombre.server.mochilas.ComandoPV(mochilaManager));
        getServer().getPluginManager().registerEvents(new me.tunombre.server.mochilas.MochilaListener(mochilaManager), this);

        me.tunombre.server.guardarropa.GuardarropaManager guardarropaManager = new me.tunombre.server.guardarropa.GuardarropaManager(this);
        me.tunombre.server.guardarropa.GuardarropaListener guardarropaListener = new me.tunombre.server.guardarropa.GuardarropaListener(guardarropaManager);
        if (getCommand("wardrobe") != null) getCommand("wardrobe").setExecutor(new me.tunombre.server.guardarropa.ComandoWardrobe(guardarropaListener));
        getServer().getPluginManager().registerEvents(guardarropaListener, this);

        me.tunombre.server.pasivas.PasivasManager pasivasManager = new me.tunombre.server.pasivas.PasivasManager(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pasivas.PasivasListener(this, pasivasManager), this);

        this.combatComboManager = new me.tunombre.server.minigames.CombatComboManager(this);
        getServer().getPluginManager().registerEvents(this.combatComboManager, this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.MiningMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.FishingHookManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.WoodcuttingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.FarmingMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.AlchemyMinigameManager(this), this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.minigames.EnchantingMinigameManager(this), this);

        me.tunombre.server.accesorios.AccesoriosManager accesoriosManager = new me.tunombre.server.accesorios.AccesoriosManager(this);
        if (getCommand("accesorios") != null) getCommand("accesorios").setExecutor(new me.tunombre.server.accesorios.ComandoAccesorios(accesoriosManager));
        getServer().getPluginManager().registerEvents(new me.tunombre.server.accesorios.AccesoriosListener(this, accesoriosManager), this);

        this.pvpManager = new me.tunombre.server.pvp.PvPManager(this);
        if (getCommand("pvp") != null) getCommand("pvp").setExecutor(new me.tunombre.server.pvp.ComandoPvP(this.pvpManager));
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pvp.PvPListener(this.pvpManager), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // Tarea del HUD
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isOnline()) continue; // 🟢 FIX: Evitar errores si el jugador se desconecta en este milisegundo

                UUID id = p.getUniqueId();

                // 🟢 ARQUITECTURA LIMPIA: Obtenemos el usuario y sus datos de la API
                me.tunombre.server.user.NexoUser user = me.tunombre.server.user.NexoAPI.getInstance().getUserLocal(id);

                int maxEnergia = 100;
                int energiaActual = 100;

                if (user != null) {
                    int nivelNexo = user.getNexoNivel();
                    maxEnergia = 100 + ((nivelNexo - 1) * 20) + user.getEnergiaExtraAccesorios();
                    energiaActual = user.getEnergiaMineria();

                    if (energiaActual < maxEnergia) {
                        int nuevaEnergia = Math.min(energiaActual + 5, maxEnergia);
                        user.setEnergiaMineria(nuevaEnergia);
                        energiaActual = nuevaEnergia;
                    }
                }

                int manaActual = 0;
                int maxMana = 0;
                try {
                    dev.aurelium.auraskills.api.user.SkillsUser userAura = dev.aurelium.auraskills.api.AuraSkillsApi.get().getUser(id);
                    if (userAura != null) {
                        manaActual = (int) userAura.getMana();
                        maxMana = (int) userAura.getMaxMana();
                    }
                } catch (Exception ignored) {}

                int hpActual = (int) Math.ceil(p.getHealth());
                int hpMax = (int) p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                String hud = "§c❤ " + hpActual + "/" + hpMax + "  §b💧 " + manaActual + "/" + maxMana + "  §e⚡ " + energiaActual + "/" + maxEnergia;

                if (combatComboManager != null && combatComboManager.enFrenesi.containsKey(id)) {
                    hud = "§4§l[FRENESÍ ACTIVO] " + hud;
                }

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
        if (databaseManager != null && databaseManager.getDataSource() != null) {
            new me.tunombre.server.colecciones.FlushTask(databaseManager.getDataSource()).run();
            getLogger().info("Colecciones y Slayers guardados en Supabase correctamente.");
        }

        // 🔴 FIX CRÍTICO: Guardado SÍNCRONO al apagar para no perder datos
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (databaseManager != null) {
                databaseManager.guardarJugadorSync(p);
            }
        }

        if (databaseManager != null) databaseManager.desconectar();

        BlockBreakListener.restaurarBloquesRotos();
        getLogger().info("Bloques del Nexo restaurados exitosamente y datos guardados de forma segura.");
    }

    public me.tunombre.server.colecciones.ColeccionesConfig getColeccionesConfig() { return coleccionesConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FileManager getFileManager() { return fileManager; }

    // Getter para tu nuevo gestor por si lo necesitas en otras clases del Core
    public me.tunombre.server.user.UserManager getUserManager() { return userManager; }

}