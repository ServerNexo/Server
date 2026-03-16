package me.tunombre.server.mochilas;

import me.tunombre.server.Base64Util;
import me.tunombre.server.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MochilaManager {
    private final Main plugin;

    public MochilaManager(Main plugin) {
        this.plugin = plugin;
    }

    public void abrirMochila(Player p, int id) {
        // Hacemos la consulta Asíncrona para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String base64Data = null;
            String sql = "SELECT contenido FROM mochilas WHERE uuid = ? AND mochila_id = ?";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    base64Data = rs.getString("contenido");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalData = base64Data;
            // Volvemos al hilo principal para abrir el inventario
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 54, "§8🎒 Mochila Virtual #" + id);

                if (finalData != null && !finalData.isEmpty()) {
                    ItemStack[] items = Base64Util.itemStackArrayFromBase64(finalData);
                    inv.setContents(items);
                }

                p.openInventory(inv);
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            });
        });
    }

    public void guardarMochila(Player p, int id, Inventory inv) {
        // Serializamos usando nuestra utilidad
        String base64Data = Base64Util.itemStackArrayToBase64(inv.getContents());

        // Guardamos en la BD de forma asíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Sintaxis PostgreSQL: Inserta, y si ya existe, lo actualiza
            String sql = "INSERT INTO mochilas (uuid, mochila_id, contenido) VALUES (?, ?, ?) " +
                    "ON CONFLICT (uuid, mochila_id) DO UPDATE SET contenido = EXCLUDED.contenido;";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUniqueId().toString());
                ps.setInt(2, id);
                ps.setString(3, base64Data);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}