package your.mom.com.chicken;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Chicken extends JavaPlugin implements Listener {

    private double minFallDistance;
    private double maxFallDistance;
    private double minFinalDamage;
    private boolean critMultiplierEnabled;
    private boolean densityAffectsPlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Gaps(this), this);

        getLogger().info("""
\u001B[38;5;214m   ██████╗██╗  ██╗██╗ ██████╗██╗  ██╗███████╗███╗   ██╗
\u001B[38;5;220m  ██╔════╝██║  ██║██║██╔════╝██║ ██╔╝██╔════╝████╗  ██║
\u001B[38;5;226m  ██║     ███████║██║██║     █████╔╝ █████╗  ██╔██╗ ██║
\u001B[38;5;190m  ██║     ██╔══██║██║██║     ██╔═██╗ ██╔══╝  ██║╚██╗██║
\u001B[38;5;154m  ╚██████╗██║  ██║██║╚██████╗██║  ██╗███████╗██║ ╚████║
\u001B[38;5;118m   ╚═════╝╚═╝  ╚═╝╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝
\u001B[0m           \u001B[37mby jinbao4 & serapnim0 \u001B[90m(alpha 1.0.0)\u001B[0m
""");}

        private void loadConfigValues() {
        minFallDistance = getConfig().getDouble("fall-distance.minimum", 1.5);
        maxFallDistance = getConfig().getDouble("fall-distance.maximum", 15.0);
        minFinalDamage = getConfig().getDouble("damage.minimum-final", 1.0);

        critMultiplierEnabled = getConfig().getBoolean(
                "critical-hits.scale-bonus", true
        );

        densityAffectsPlayers = getConfig().getBoolean(
                "density.affects-players", false
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMaceHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.MACE) return;

        float fallDistance = attacker.getFallDistance();
        if (fallDistance <= minFallDistance) return;

        boolean targetIsPlayer = event.getEntityType() == EntityType.PLAYER;
        boolean isCritical = event.isCritical();

        int densityLevel = weapon.getEnchantmentLevel(Enchantment.DENSITY);

        double vanillaSmash = calculateSmashBonus(fallDistance);
        double vanillaDensity = calculateDensityBonus(fallDistance, densityLevel);

        float cappedDistance = (float) Math.min(fallDistance, maxFallDistance);
        double cappedSmash = calculateSmashBonus(cappedDistance);

        double cappedDensity = 0.0;
        if (!targetIsPlayer || densityAffectsPlayers) {
            cappedDensity = calculateDensityBonus(cappedDistance, densityLevel);
        }

        double excessDamage = (vanillaSmash + vanillaDensity)
                - (cappedSmash + cappedDensity);

        if (critMultiplierEnabled && isCritical) {
            excessDamage *= 1.5;
        }

        if (excessDamage <= 0) return;

        double newDamage = Math.max(
                minFinalDamage,
                event.getDamage() - excessDamage
        );

        event.setDamage(newDamage);
    }

    private double calculateSmashBonus(float distance) {
        double damage = 0.0;

        double phase1Max = getConfig().getDouble("smash.phase1.max");
        double phase2Max = getConfig().getDouble("smash.phase2.max");

        damage += Math.min(distance, phase1Max)
                * getConfig().getDouble("smash.phase1.damage-per-block");

        if (distance > phase1Max) {
            damage += Math.min(distance - phase1Max, phase2Max)
                    * getConfig().getDouble("smash.phase2.damage-per-block");
        }

        if (distance > phase1Max + phase2Max) {
            damage += (distance - phase1Max - phase2Max)
                    * getConfig().getDouble("smash.phase3.damage-per-block");
        }

        return damage;
    }

    private double calculateDensityBonus(float distance, int level) {
        if (level <= 0) return 0.0;
        return getConfig().getDouble("density.multiplier-per-level") * level * distance;
    }
}
