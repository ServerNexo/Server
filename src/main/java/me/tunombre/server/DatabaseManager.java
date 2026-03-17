package me.tunombre.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    // 🛡️ MÉTODO BLINDADO CONTRA CRASHEOS DE CONEXIÓN
    public void conectar() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(plugin.getConfig().getString("database.url"));
            config.setUsername(plugin.getConfig().getString("database.username"));
            config.setPassword(plugin.getConfig().getString("database.password"));

            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000); // Máximo 10 segundos esperando para no congelar el servidor

            dataSource = new HikariDataSource(config);
            crearTabla(); // Aquí llamamos a la creación de tablas

            plugin.getLogger().info("✅ ¡Conexión a Supabase establecida correctamente!");

        } catch (Exception e) {
            plugin.getLogger().severe("============================================");
            plugin.getLogger().severe("❌ ERROR DE BASE DE DATOS SUPABASE ❌");
            plugin.getLogger().severe("No se pudo conectar a la base de datos: " + e.getMessage());
            plugin.getLogger().severe("Revisa que tu contraseña sea correcta o que Supabase no esté pausado.");
            plugin.getLogger().severe("El plugin encenderá, pero los datos no se guardarán en la nube.");
            plugin.getLogger().severe("============================================");
        }
    }

    public void desconectar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("El DataSource no está inicializado.");
        return dataSource.getConnection();
    }

    // Método agregado para que NexoCollections pueda usar HikariCP
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    // ==========================================
    // 🗄️ CREACIÓN DE MÚLTIPLES TABLAS (Optimizadas Java 21)
    // ==========================================
    private void crearTabla() {
        if (dataSource == null) return; // Si falló la conexión, no intentamos crear tablas

        // 🟢 Clean Code: Text Blocks para evitar el "+" infinito en SQL
        String sqlJugadores = """
                CREATE TABLE IF NOT EXISTS jugadores (
                    uuid VARCHAR(36) PRIMARY KEY, nombre VARCHAR(16) NOT NULL,
                    nexo_nivel INT DEFAULT 1, nexo_xp INT DEFAULT 0,
                    combate_nivel INT DEFAULT 1, combate_xp INT DEFAULT 0,
                    mineria_nivel INT DEFAULT 1, mineria_xp INT DEFAULT 0,
                    agricultura_nivel INT DEFAULT 1, agricultura_xp INT DEFAULT 0
                );""";

        String sqlMochilas = """
                CREATE TABLE IF NOT EXISTS mochilas (
                    uuid VARCHAR(36), mochila_id INT, contenido TEXT,
                    PRIMARY KEY (uuid, mochila_id)
                );""";

        String sqlGuardarropa = """
                CREATE TABLE IF NOT EXISTS guardarropa (
                    uuid VARCHAR(36), preset_id INT, contenido TEXT,
                    PRIMARY KEY (uuid, preset_id)
                );""";

        String sqlStorage = """
                CREATE TABLE IF NOT EXISTS nexo_storage (
                    uuid VARCHAR(36), tipo VARCHAR(32), contenido TEXT,
                    PRIMARY KEY (uuid, tipo)
                );""";

        String sqlColecciones = """
                CREATE TABLE IF NOT EXISTS nexo_collections (
                    uuid VARCHAR(36) PRIMARY KEY,
                    collections_data JSONB NOT NULL DEFAULT '{}'::jsonb
                );""";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                // Ejecutamos las creaciones al arrancar el servidor
                stmt.execute(sqlJugadores);
                stmt.execute(sqlMochilas);
                stmt.execute(sqlGuardarropa);
                stmt.execute(sqlStorage);
                stmt.execute(sqlColecciones); // Ejecutamos la tabla de colecciones
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 👤 GESTIÓN DE JUGADOR (Thread-Safe)
    // ==========================================
    public void cargarJugador(Player player) {
        if (dataSource == null) return;

        String selectSQL = "SELECT * FROM jugadores WHERE uuid = ?";
        String insertSQL = "INSERT INTO jugadores (uuid, nombre) VALUES (?, ?)"; // Eliminados valores por defecto (la base de datos se encarga de eso)
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement psSelect = conn.prepareStatement(selectSQL)) {
                    psSelect.setString(1, uuid.toString());
                    ResultSet rs = psSelect.executeQuery();

                    if (rs.next()) {
                        // Leemos datos en asíncrono
                        int nN = rs.getInt("nexo_nivel"), nX = rs.getInt("nexo_xp");
                        int cN = rs.getInt("combate_nivel"), cX = rs.getInt("combate_xp");
                        int mN = rs.getInt("mineria_nivel"), mX = rs.getInt("mineria_xp");
                        int aN = rs.getInt("agricultura_nivel"), aX = rs.getInt("agricultura_xp");

                        // 🔴 FIX: Volvemos al Main Thread para modificar los mapas concurrentes sin crashear el servidor
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.nexoNiveles.put(uuid, nN); plugin.nexoXp.put(uuid, nX);
                            plugin.combateNiveles.put(uuid, cN); plugin.combateXp.put(uuid, cX);
                            plugin.mineriaNiveles.put(uuid, mN); plugin.mineriaXp.put(uuid, mX);
                            plugin.agriculturaNiveles.put(uuid, aN); plugin.agriculturaXp.put(uuid, aX);
                        });
                    } else {
                        try (PreparedStatement psInsert = conn.prepareStatement(insertSQL)) {
                            psInsert.setString(1, uuid.toString());
                            psInsert.setString(2, name);
                            psInsert.executeUpdate();

                            // 🔴 FIX: Volvemos al Main Thread
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.nexoNiveles.put(uuid, 1); plugin.nexoXp.put(uuid, 0);
                                plugin.combateNiveles.put(uuid, 1); plugin.combateXp.put(uuid, 0);
                                plugin.mineriaNiveles.put(uuid, 1); plugin.mineriaXp.put(uuid, 0);
                                plugin.agriculturaNiveles.put(uuid, 1); plugin.agriculturaXp.put(uuid, 0);

                                plugin.getLogger().info("¡Nuevo jugador RPG registrado: " + name);
                            });
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al cargar jugador " + name + ": " + e.getMessage());
            }
        });
    }

    public void guardarJugador(Player player) {
        if (dataSource == null) return;

        UUID uuid = player.getUniqueId();
        if (!plugin.nexoNiveles.containsKey(uuid)) return;

        // 🔴 FIX: Leemos los datos en el Main Thread ANTES de mandarlos al hilo asíncrono
        int nNivel = plugin.nexoNiveles.get(uuid); int nXp = plugin.nexoXp.get(uuid);
        int cNivel = plugin.combateNiveles.get(uuid); int cXp = plugin.combateXp.get(uuid);
        int mNivel = plugin.mineriaNiveles.get(uuid); int mXp = plugin.mineriaXp.get(uuid);
        int aNivel = plugin.agriculturaNiveles.get(uuid); int aXp = plugin.agriculturaXp.get(uuid);
        String nombre = player.getName();

        String updateSQL = """
                UPDATE jugadores SET nexo_nivel = ?, nexo_xp = ?, nombre = ?, 
                combate_nivel = ?, combate_xp = ?, mineria_nivel = ?, mineria_xp = ?, 
                agricultura_nivel = ?, agricultura_xp = ? WHERE uuid = ?
                """;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSQL)) {

                ps.setInt(1, nNivel); ps.setInt(2, nXp); ps.setString(3, nombre);
                ps.setInt(4, cNivel); ps.setInt(5, cXp); ps.setInt(6, mNivel);
                ps.setInt(7, mXp); ps.setInt(8, aNivel); ps.setInt(9, aXp);
                ps.setString(10, uuid.toString());

                ps.executeUpdate(); // Guardamos en la base de datos primero

                // 🔴 FIX ANTI DATA LOSS: Solo borramos los datos de la RAM si se guardó con éxito en Supabase
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.nexoNiveles.remove(uuid); plugin.nexoXp.remove(uuid);
                    plugin.combateNiveles.remove(uuid); plugin.combateXp.remove(uuid);
                    plugin.mineriaNiveles.remove(uuid); plugin.mineriaXp.remove(uuid);
                    plugin.agriculturaNiveles.remove(uuid); plugin.agriculturaXp.remove(uuid);
                    plugin.energiaMineria.remove(uuid);
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("Fallo grave al guardar a " + nombre + ". Sus datos se mantuvieron en memoria: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🔴 NUEVO MÉTODO CRÍTICO PARA EL APAGADO (onDisable)
    // ==========================================
    public void guardarJugadorSync(Player player) {
        if (dataSource == null) return;
        UUID uuid = player.getUniqueId();
        if (!plugin.nexoNiveles.containsKey(uuid)) return;

        String updateSQL = """
                UPDATE jugadores SET nexo_nivel = ?, nexo_xp = ?, nombre = ?, 
                combate_nivel = ?, combate_xp = ?, mineria_nivel = ?, mineria_xp = ?, 
                agricultura_nivel = ?, agricultura_xp = ? WHERE uuid = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSQL)) {

            ps.setInt(1, plugin.nexoNiveles.get(uuid));
            ps.setInt(2, plugin.nexoXp.get(uuid));
            ps.setString(3, player.getName());
            ps.setInt(4, plugin.combateNiveles.get(uuid));
            ps.setInt(5, plugin.combateXp.get(uuid));
            ps.setInt(6, plugin.mineriaNiveles.get(uuid));
            ps.setInt(7, plugin.mineriaXp.get(uuid));
            ps.setInt(8, plugin.agriculturaNiveles.get(uuid));
            ps.setInt(9, plugin.agriculturaXp.get(uuid));
            ps.setString(10, uuid.toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en guardado síncrono de " + player.getName() + ": " + e.getMessage());
        }
    }
}