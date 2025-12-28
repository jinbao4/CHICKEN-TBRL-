package your.mom.com.chicken;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class Gaps implements Listener {

    private final JavaPlugin plugin;

    public Gaps(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private int getMaxEgaps() {
        return plugin.getConfig().getInt("egaps.max-per-player", 5);
    }

    private boolean isEgap(ItemStack item) {
        return item != null && item.getType() == Material.ENCHANTED_GOLDEN_APPLE;
    }

    private void enforceEgapLimit(Player p) {
        int remaining = getMaxEgaps();

        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!isEgap(it)) continue;

            if (remaining <= 0) {
                contents[i] = null;
                continue;
            }

            if (it.getAmount() > remaining) {
                it.setAmount(remaining);
                remaining = 0;
            } else {
                remaining -= it.getAmount();
            }
        }

        p.getInventory().setContents(contents);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!isEgap(e.getItem().getItemStack())) return;

        Bukkit.getScheduler().runTask(plugin, () -> enforceEgapLimit(p));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        enforceEgapLimit(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        if (isEgap(cursor) && current != null && current.getType() == Material.BUNDLE) {
            e.setCancelled(true);
            return;
        }

        if (cursor != null && cursor.getType() == Material.BUNDLE && isEgap(current)) {
            e.setCancelled(true);
            return;
        }

        if (cursor != null && cursor.getType() == Material.BUNDLE && bundleHasEgap(cursor)) {
            e.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> enforceEgapLimit(p));
    }

    private boolean bundleHasEgap(ItemStack bundle) {
        if (!(bundle.getItemMeta() instanceof BundleMeta meta)) return false;
        for (ItemStack i : meta.getItems()) {
            if (isEgap(i)) return true;
        }
        return false;
    }
}
