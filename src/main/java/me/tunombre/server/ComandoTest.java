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
        // ⛏️ MODO INVOCACIÓN DE HERRAMIENTAS (Ej: /test tool pico_novato)
        // ==========================================
        if (args.length == 2 && args[0].equalsIgnoreCase("tool")) {
            String idHerramienta = args[1].toLowerCase();
            ItemStack herramienta = ItemManager.generarHerramientaProfesion(idHerramienta);

            // Si devuelve un pico de madera sin nombre, es que falló
            if (herramienta.getType() == Material.WOODEN_PICKAXE && !herramienta.getItemMeta().hasDisplayName()) {
                p.sendMessage("§c[!] Error: No se encontró '" + idHerramienta + "' en herramientas.yml");
                return true;
            }

            p.getInventory().addItem(herramienta);
            p.sendMessage("§a✨ ¡Has invocado la herramienta: §e" + idHerramienta + "§a!");
            return true;
        }

        // ==========================================
        // ⚔️ MODO INVOCACIÓN DE ARMAS (Ej: /test arma dagas_espinas_t1)
        // ==========================================
        if (args.length == 2 && args[0].equalsIgnoreCase("arma")) {
            String idArma = args[1].toLowerCase();
            ItemStack arma = ItemManager.generarArmaRPG(idArma);

            // Si nos devuelve una espada de madera sin nombre, es que falló
            if (arma.getType() == Material.WOODEN_SWORD && !arma.getItemMeta().hasDisplayName()) {
                p.sendMessage("§c[!] Error: No se encontró '" + idArma + "' en armas.yml");
                return true;
            }

            p.getInventory().addItem(arma);
            p.sendMessage("§a✨ ¡Has invocado el arma: §e" + idArma + "§a!");
            return true;
        }

        // ==========================================
        // 🛡️ MODO INVOCACIÓN DE ARMADURAS (Ej: /test agri_t7)
        // ==========================================
        if (args.length == 1) {
            String idArmadura = args[0].toLowerCase();

            // Comprobamos si el ID existe pidiendo un Peto de prueba
            ItemStack prueba = ItemManager.generarArmaduraProfesion(idArmadura, "CHESTPLATE");
            if (prueba.getType() == Material.STONE && !prueba.getItemMeta().hasDisplayName()) {
                p.sendMessage("§c[!] Error: No se encontró '" + idArmadura + "' en armaduras.yml");
                return true;
            }

            // ¡Imprimimos el Set Completo!
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
        p.sendMessage("§e/test tool <ID> §7- Invoca una herramienta (Ej: /test tool pico_novato)");
        p.sendMessage("§e/test arma <ID> §7- Invoca un arma (Ej: /test arma dagas_espinas_t1)");
        p.sendMessage("§e/test <ID> §7- Invoca una armadura (Ej: /test agri_t7)");
        p.sendMessage("§e/test §7- Abre el menú de Herrería y da Polvos");

        // Te damos 10 Polvos Estelares
        ItemStack polvos = ItemManager.crearPolvoEstelar();
        polvos.setAmount(10);
        p.getInventory().addItem(polvos);

        // Te abrimos el menú de la Herrería
        HerreriaListener herreria = new HerreriaListener(plugin);
        herreria.abrirMenu(p);

        return true;
    }
}