package com.bendemen.eywahead;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EywaHead extends JavaPlugin implements Listener {
    private final Map<UUID, DeathContext> deaths = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Plugin axGraves = Bukkit.getPluginManager().getPlugin("AxGraves");
        if (axGraves == null || !axGraves.isEnabled()) {
            getLogger().warning("AxGraves was not found/enabled. Eywa-Head will stay inactive.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        registerGraveSpawnEvent();
        getLogger().info("Enabled. Waiting for original HeadDrop player-head items.");
    }

    @Override public void onDisable() { deaths.clear(); }

    @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DeathContext context = new DeathContext(player.getUniqueId(), player.getLocation().clone(), System.currentTimeMillis());
        deaths.put(player.getUniqueId(), context);
        Bukkit.getScheduler().runTaskLater(this, () -> deaths.computeIfPresent(player.getUniqueId(), (id, current) -> current == context && current.head == null ? null : current), 100L);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Item item)) return;
        ItemStack stack = item.getItemStack();
        if (stack.getType() != Material.PLAYER_HEAD || !(stack.getItemMeta() instanceof SkullMeta skullMeta)) return;
        OfflinePlayer owner = skullMeta.getOwningPlayer();
        if (owner == null) return;
        DeathContext context = deaths.get(owner.getUniqueId());
        if (context == null || System.currentTimeMillis() - context.createdAt > 5000L) return;
        if (!sameArea(context.location, item.getLocation(), 8.0)) return;
        context.head = stack.clone();
        context.itemEntity = item;
        getLogger().fine("Captured original player-head item for " + owner.getName() + "; waiting for AxGraves grave.");
        scheduleInsert(context);
    }

    private void registerGraveSpawnEvent() {
        try {
            Class<?> raw = Class.forName("com.artillexstudios.axgraves.api.events.GraveSpawnEvent");
            if (!Event.class.isAssignableFrom(raw)) throw new IllegalStateException("AxGraves event is not a Bukkit Event");
            @SuppressWarnings("unchecked") Class<? extends Event> eventClass = (Class<? extends Event>) raw;
            EventExecutor executor = (listener, event) -> {
                try {
                    Player player = (Player) eventClass.getMethod("getPlayer").invoke(event);
                    Object grave = eventClass.getMethod("getGrave").invoke(event);
                    DeathContext context = deaths.get(player.getUniqueId());
                    if (context == null) return;
                    context.grave = grave;
                    scheduleInsert(context);
                } catch (ReflectiveOperationException exception) {
                    getLogger().severe("Could not read AxGraves GraveSpawnEvent: " + exception.getMessage());
                }
            };
            Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, executor, this, true);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            getLogger().severe("Could not hook AxGraves GraveSpawnEvent. Check your AxGraves version: " + exception.getMessage());
        }
    }

    private void scheduleInsert(DeathContext context) {
        if (context.head == null || context.grave == null || context.insertScheduled) return;
        context.insertScheduled = true;
        Bukkit.getScheduler().runTask(this, () -> {
            context.insertScheduled = false;
            if (context.head == null || context.grave == null) return;
            try {
                Inventory inventory = (Inventory) context.grave.getClass().getMethod("getGui").invoke(context.grave);

                // AxGraves may already have stored the HeadDrop head as part of the death drops.
                // Do not add a second copy; just remove the separate world entity.
                boolean alreadyStored = false;
                for (ItemStack existing : inventory.getContents()) {
                    if (existing != null && existing.isSimilar(context.head)
                            && existing.getAmount() >= context.head.getAmount()) {
                        alreadyStored = true;
                        break;
                    }
                }

                if (!alreadyStored && !inventory.addItem(context.head.clone()).isEmpty()) {
                    getLogger().warning("Grave inventory had no room for " + context.playerId + "'s HeadDrop head; leaving the original item untouched.");
                    return;
                }

                if (context.itemEntity != null && context.itemEntity.isValid()) context.itemEntity.remove();
                context.head = null;
                deaths.remove(context.playerId, context);
                getLogger().info((alreadyStored ? "HeadDrop head was already stored; removed duplicate world item for " : "Stored original HeadDrop head in AxGraves grave for ") + context.playerId + ".");
            } catch (ReflectiveOperationException | RuntimeException exception) {
                getLogger().severe("Could not insert HeadDrop head into AxGraves grave: " + exception.getMessage());
            }
        });
    }

    private static boolean sameArea(Location a, Location b, double radius) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld()) && a.distanceSquared(b) <= radius * radius;
    }

    private static final class DeathContext {
        private final UUID playerId;
        private final Location location;
        private final long createdAt;
        private volatile Object grave;
        private volatile ItemStack head;
        private volatile Item itemEntity;
        private volatile boolean insertScheduled;
        private DeathContext(UUID playerId, Location location, long createdAt) { this.playerId = playerId; this.location = location; this.createdAt = createdAt; }
    }
}
