package me.tunombre.server.minigames;

import me.tunombre.server.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class FishingHookManager implements Listener {

    private final Main plugin;

    public FishingHookManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void alPescar(PlayerFishEvent event) {
        // Solo nos interesa cuando el jugador tira exitosamente de la caña y saca algo
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {

            if (event.getCaught() instanceof Item itemCapturado) {
                ItemStack pescado = itemCapturado.getItemStack();
                if (pescado == null || !pescado.hasItemMeta()) return;

                // EvenMoreFish inyecta claves PDC en sus pescados (ej: emf-fish-name o emf-fish-rarity)
                var pdc = pescado.getItemMeta().getPersistentDataContainer();

                // Iteramos por las keys de EMF de forma segura (sin depender de su API directa)
                boolean esPezCustom = false;
                for (NamespacedKey key : pdc.getKeys()) {
                    if (key.getNamespace().equalsIgnoreCase("evenmorefish") || key.getKey().contains("emf")) {
                        esPezCustom = true;
                        break;
                    }
                }

                if (esPezCustom) {
                    Player p = event.getPlayer();

                    // Conectamos con tu ecosistema de Energía
                    int energiaAct = plugin.energiaMineria.getOrDefault(p.getUniqueId(), 100);
                    int maxEnergia = 100 + ((plugin.nexoNiveles.getOrDefault(p.getUniqueId(), 1) - 1) * 20);

                    plugin.energiaMineria.put(p.getUniqueId(), Math.min(energiaAct + 5, maxEnergia));
                    p.sendMessage("§b🎣 ¡Pesca perfecta! §e+5⚡ Energía");
                }
            }
        }
    }

    /*
     * 💡 Hook Opcional de MythicMobs (Bosses de Pesca).
     * Si instalas MythicMobs, puedes descomentar esto importando su API:
     * * @EventHandler
     * public void alMatarBoss(io.lumine.mythic.bukkit.events.MythicMobDeathEvent event) {
     * if (event.getMob().getType().getInternalName().contains("FISHING_BOSS")) {
     * if (event.getKiller() instanceof Player p) {
     * // Dar 50 Energía a los participantes
     * }
     * }
     * }
     */
}