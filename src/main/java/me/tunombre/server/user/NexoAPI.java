package me.tunombre.server.user;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NexoAPI {

    private static NexoAPI instance;
    private final UserManager userManager;

    public NexoAPI(UserManager userManager) {
        this.userManager = userManager;
        instance = this;
    }

    public static NexoAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("¡NexoAPI aún no ha sido inicializada en el Core!");
        }
        return instance;
    }

    // ==========================================
    // 🟢 LECTURA SEGURA (Read-Through)
    // ==========================================

    /**
     * Obtiene al jugador de la RAM de forma instantánea.
     * Útil para eventos rápidos de Bukkit como golpear o romper un bloque.
     */
    public NexoUser getUserLocal(UUID uuid) {
        return userManager.getUserOrNull(uuid);
    }

    /**
     * Busca al jugador. Si no está en la RAM (ej. desconectado),
     * puedes programar que lo busque en la BD en el futuro.
     * Ideal para los Addons.
     */
    public CompletableFuture<Optional<NexoUser>> getUserAsync(UUID uuid) {
        // Por ahora devuelve lo de la RAM, después conectaremos la DB aquí para "Read-Through"
        return CompletableFuture.completedFuture(userManager.getUser(uuid));
    }

    // ==========================================
    // 🟢 MODIFICACIÓN SEGURA (Addons)
    // ==========================================

    /**
     * Método asíncrono que los minijuegos o Addons llamarán para dar XP.
     * Ejemplo: NexoAPI.getInstance().addCombateXpAsync(uuid, 50);
     */
    public CompletableFuture<Void> addCombateXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> {
                user.addCombateXp(xp);
                // Aquí podrías meter la lógica de subir de nivel que tenías en el Main
            });
        });
    }

    public CompletableFuture<Void> addNexoXpAsync(UUID uuid, int xp) {
        return getUserAsync(uuid).thenAccept(optUser -> {
            optUser.ifPresent(user -> user.addNexoXp(xp));
        });
    }

    // Puedes ir agregando más métodos para Minería, Agricultura, etc.
}