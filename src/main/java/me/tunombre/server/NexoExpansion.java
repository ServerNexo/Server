package me.tunombre.server;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class NexoExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public NexoExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexo";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TuNombre";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();

        // ==========================================
        // 1. VARIABLES GLOBALES (NEXO)
        // ==========================================
        if (params.equalsIgnoreCase("nivel")) {
            return String.valueOf(plugin.nexoNiveles.getOrDefault(uuid, 1));
        }
        if (params.equalsIgnoreCase("xp")) {
            return String.valueOf(plugin.nexoXp.getOrDefault(uuid, 0));
        }
        if (params.equalsIgnoreCase("xprequerida")) {
            int nivelActual = plugin.nexoNiveles.getOrDefault(uuid, 1);
            return String.valueOf(nivelActual * 100);
        }

        // ==========================================
        // 2. VARIABLES DE PROFESIÓN (NUEVO)
        // ==========================================
        if (params.equalsIgnoreCase("mineria_nivel")) {
            return String.valueOf(plugin.mineriaNiveles.getOrDefault(uuid, 1));
        }
        if (params.equalsIgnoreCase("combate_nivel")) {
            return String.valueOf(plugin.combateNiveles.getOrDefault(uuid, 1));
        }
        if (params.equalsIgnoreCase("agricultura_nivel")) {
            return String.valueOf(plugin.agriculturaNiveles.getOrDefault(uuid, 1));
        }

        return null;
    }
}