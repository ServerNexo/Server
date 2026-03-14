package me.tunombre.server;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ComandoTest implements CommandExecutor {

    private final Main plugin;

    public ComandoTest(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("nexo.admin")) {
            p.sendMessage("§cNo tienes permiso para usar comandos de desarrollo.");
            return true;
        }

        // ==========================================
        // 🔮 MODO INVOCACIÓN DE SETS COMPLETOS (Ej: /test agri_t7)
        // ==========================================
        if (args.length >= 1) {
            String idArmadura = args[0].toLowerCase();

            // 1. Comprobamos si el ID existe pidiendo un Peto de prueba
            ItemStack prueba = ItemManager.generarArmaduraProfesion(idArmadura, "CHESTPLATE");
            if (prueba.getType() == Material.STONE && !prueba.getItemMeta().hasDisplayName()) {
                p.sendMessage("§c[!] Error: No se encontró '" + idArmadura + "' en armaduras.yml");
                return true;
            }

            // 2. ¡Imprimimos el Set Completo!
            String[] piezas = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
            for (String pieza : piezas) {
                ItemStack itemSet = ItemManager.generarArmaduraProfesion(idArmadura, pieza);
                p.getInventory().addItem(itemSet);
            }

            p.sendMessage("§a✨ ¡Has invocado el set completo de: §e" + idArmadura + "§a!");
            return true;
        }

        // ==========================================
        // ⚙️ MODO NORMAL (Sin argumentos)
        // ==========================================
        p.sendMessage("§6§l=== PANEL DE DESARROLLADOR NEXO ===");
        p.sendMessage("§e/test <ID_YML> §7- Invoca una armadura del archivo (Ej: /test agri_t7)");
        p.sendMessage("§e/test §7- Abre el menú de Herrería y da Polvos");

        // 1. Te damos 10 Polvos Estelares
        ItemStack polvos = ItemManager.crearPolvoEstelar();
        polvos.setAmount(10);
        p.getInventory().addItem(polvos);

        // 2. Te abrimos el menú de la Herrería
        HerreriaListener herreria = new HerreriaListener(plugin);
        herreria.abrirMenu(p);

        return true;
    }
}