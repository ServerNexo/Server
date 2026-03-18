package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager;

    private me.tunombre.server.user.UserManager userManager;
    private me.tunombre.server.user.NexoAPI nexoAPI;

    private me.tunombre.server.colecciones.ColeccionesConfig coleccionesConfig;
    public me.tunombre.server.pvp.PvPManager pvpManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.conectar();

        this.userManager = new me.tunombre.server.user.UserManager();
        this.nexoAPI = new me.tunombre.server.user.NexoAPI(this.userManager);

        // Sistemas que se quedaron en el Core (Progresión y Bases)
        this.coleccionesConfig = new me.tunombre.server.colecciones.ColeccionesConfig(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.colecciones.ColeccionesListener(this), this);
        new me.tunombre.server.colecciones.FlushTask(databaseManager.getDataSource()).runTaskTimerAsynchronously(this, 12000L, 12000L);

        if (getCommand("test") != null) getCommand("test").setExecutor(new ComandoTest(this));
        if (getCommand("nexocore") != null) getCommand("nexocore").setExecutor(new ComandoNexo(this));

        // Listeners generales que aún están en el Core
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);

        // Pasivas y PvP (Los moveremos luego si decides aislar la progresión)
        me.tunombre.server.pasivas.PasivasManager pasivasManager = new me.tunombre.server.pasivas.PasivasManager(this);
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pasivas.PasivasListener(this, pasivasManager), this);

        this.pvpManager = new me.tunombre.server.pvp.PvPManager(this);
        if (getCommand("pvp") != null) getCommand("pvp").setExecutor(new me.tunombre.server.pvp.ComandoPvP(this.pvpManager));
        getServer().getPluginManager().registerEvents(new me.tunombre.server.pvp.PvPListener(this.pvpManager), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(this).register();
        }

        // Tarea del HUD
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isOnline()) continue;

                UUID id = p.getUniqueId();
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

                if (pvpManager != null && pvpManager.estaEnCombate(p)) {
                    hud = "§c⚔ §l¡EN COMBATE! §r" + hud;
                }

                p.sendActionBar(hud);
            }
        }, 20L, 20L);

        getLogger().info("¡Nexo Core V8.2: Core Purificado!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null && databaseManager.getDataSource() != null) {
            new me.tunombre.server.colecciones.FlushTask(databaseManager.getDataSource()).run();
            getLogger().info("Colecciones y Slayers guardados en Supabase correctamente.");
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (databaseManager != null) {
                databaseManager.guardarJugadorSync(p);
            }
        }

        if (databaseManager != null) databaseManager.desconectar();
        BlockBreakListener.restaurarBloquesRotos();
    }

    public me.tunombre.server.colecciones.ColeccionesConfig getColeccionesConfig() { return coleccionesConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FileManager getFileManager() { return fileManager; }
    public me.tunombre.server.user.UserManager getUserManager() { return userManager; }
}