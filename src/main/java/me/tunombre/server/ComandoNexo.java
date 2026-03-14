package me.tunombre.server;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoNexo implements CommandExecutor {

    private final Main plugin;

    public ComandoNexo(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("nexo.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 3) {
            Player objetivo = Bukkit.getPlayer(args[1]);
            if (objetivo == null) return true;

            try {
                int cantidad = Integer.parseInt(args[2]);

                // 1. Comando de Nexo XP Global
                if (args[0].equalsIgnoreCase("darxp")) {
                    plugin.darNexoXp(objetivo, cantidad);
                    if (sender instanceof Player) sender.sendMessage("§aHas dado " + cantidad + " Nexo XP a " + objetivo.getName());
                }
                // 2. Comando de Combate XP
                else if (args[0].equalsIgnoreCase("darcombatexp")) {
                    plugin.darCombateXp(objetivo, cantidad);
                    if (sender instanceof Player) sender.sendMessage("§cHas dado " + cantidad + " Combate XP a " + objetivo.getName());
                }

            } catch (NumberFormatException e) {
                if (sender instanceof Player) sender.sendMessage("§cLa cantidad debe ser un número válido.");
            }
            return true;
        }

        if (sender instanceof Player) sender.sendMessage("§cUso correcto: /nexo <darxp|darcombatexp> <jugador> <cantidad>");
        return true;
    }
}