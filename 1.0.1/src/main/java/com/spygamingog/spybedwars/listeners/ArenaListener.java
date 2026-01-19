package com.spygamingog.spybedwars.listeners;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.api.arena.GameState;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.team.Team;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Optional;

public class ArenaListener implements Listener {
    private final java.util.Map<java.util.UUID, Boolean> canRespawnAtDeath = new java.util.HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (!arenaOpt.isPresent()) return;
        Arena arena = arenaOpt.get();

        if (arena.getGameState() != GameState.PLAYING) {
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                event.setCancelled(true);
            }
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        if (event.getAction().name().contains("RIGHT")) {
            if (item.getType() == Material.FIRE_CHARGE) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
                
                org.bukkit.entity.Fireball fireball = player.launchProjectile(org.bukkit.entity.Fireball.class);
                fireball.setYield(2.0f);
                fireball.setIsIncendiary(false);
                arena.addPersistentEntity(fireball);
            } else if (item.getType() == Material.SNOWBALL && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Ice Fish")) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                org.bukkit.entity.Snowball snowball = player.launchProjectile(org.bukkit.entity.Snowball.class);
                snowball.setMetadata("ice_fish", new org.bukkit.metadata.FixedMetadataValue(SpyBedWars.getInstance(), true));
                arena.addPersistentEntity(snowball);
                
                // Silverfish/Ice Fish specific: Spawn silverfish immediately on launch to follow snowball?
                // No, standard BedWars spawns on hit. But let's ensure it's aggressive.
            } else if (item.getType() == Material.IRON_GOLEM_SPAWN_EGG) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                org.bukkit.entity.IronGolem golem = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.IronGolem.class);
                golem.setCustomName(ChatColor.WHITE + player.getName() + "'s Golem");
                golem.setCustomNameVisible(true);
                
                // Increase golem damage and knockback
                org.bukkit.attribute.AttributeInstance damageAttr = golem.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
                if (damageAttr != null) {
                    damageAttr.setBaseValue(12.0); // Increased damage for better combat
                }
                org.bukkit.attribute.AttributeInstance kbAttr = golem.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_KNOCKBACK);
                if (kbAttr != null) {
                    kbAttr.setBaseValue(1.5); // Add significant knockback
                }
                
                // Mark golem with team and arena metadata
                Team team = arena.getTeam(player);
                if (team != null) {
                    golem.setMetadata("spybedwars_team", new org.bukkit.metadata.FixedMetadataValue(SpyBedWars.getInstance(), team.getName()));
                    golem.setMetadata("spybedwars_arena", new org.bukkit.metadata.FixedMetadataValue(SpyBedWars.getInstance(), arena.getArenaName()));
                    
                    // Add golem to arena's tracking for cleanup
                    arena.addPersistentEntity(golem);
                    
                    // Periodically search for targets
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            if (golem.isDead() || !golem.isValid()) {
                                this.cancel();
                                return;
                            }
                            
                            if (golem.getTarget() == null) {
                                for (org.bukkit.entity.Entity nearby : golem.getNearbyEntities(15, 15, 15)) {
                                    // Target enemy players
                                    if (nearby instanceof Player p && arena.getPlayers().contains(p.getUniqueId()) && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                                        Team targetTeam = arena.getTeam(p);
                                        if (targetTeam != null && !targetTeam.getName().equals(team.getName())) {
                                            golem.setTarget(p);
                                            break;
                                        }
                                    }
                                    // Target enemy Iron Golems
                                    else if (nearby instanceof org.bukkit.entity.IronGolem enemyGolem && enemyGolem.hasMetadata("spybedwars_team")) {
                                        String enemyTeamName = enemyGolem.getMetadata("spybedwars_team").get(0).asString();
                                        if (!enemyTeamName.equals(team.getName())) {
                                            golem.setTarget(enemyGolem);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }.runTaskTimer(SpyBedWars.getInstance(), 20L, 20L);
                }
            } else if (item.getType() == Material.EGG && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Bridge Egg")) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                org.bukkit.entity.Egg egg = player.launchProjectile(org.bukkit.entity.Egg.class);
                egg.setMetadata("bridge_egg", new org.bukkit.metadata.FixedMetadataValue(SpyBedWars.getInstance(), true));
                arena.addPersistentEntity(egg); // Track for cleanup
                
                final Team team = arena.getTeam(player);
                final Material blockMat = (team != null) ? team.getColor().getMaterial() : Material.WHITE_WOOL;

                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (egg.isDead() || !egg.isValid()) {
                            this.cancel();
                            return;
                        }
                        
                        Location loc = egg.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
                        if (loc.getBlock().getType() == Material.AIR) {
                            // Check if location is within arena boundaries or near spawns if necessary
                            loc.getBlock().setType(blockMat);
                            arena.addPlacedBlock(loc.getBlock().getLocation());
                        }
                        
                        // Also check 1 block behind to fill gaps
                        Location behind = egg.getLocation().subtract(egg.getVelocity().normalize()).getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
                        if (behind.getBlock().getType() == Material.AIR) {
                            behind.getBlock().setType(blockMat);
                            arena.addPlacedBlock(behind.getBlock().getLocation());
                        }
                    }
                }.runTaskTimer(SpyBedWars.getInstance(), 1L, 1L);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.IronGolem golem)) return;
        if (!golem.hasMetadata("spybedwars_team")) return;

        String golemTeam = golem.getMetadata("spybedwars_team").get(0).asString();
        String golemArena = golem.getMetadata("spybedwars_arena").get(0).asString();

        if (event.getTarget() instanceof Player targetPlayer) {
            Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByName(golemArena);
            if (arenaOpt.isPresent()) {
                Arena arena = arenaOpt.get();
                Team targetTeam = arena.getTeam(targetPlayer);
                
                // If target is on the same team, cancel targeting
                if (targetTeam != null && targetTeam.getName().equals(golemTeam)) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getTarget() instanceof org.bukkit.entity.IronGolem targetGolem) {
            if (targetGolem.hasMetadata("spybedwars_team")) {
                String targetTeam = targetGolem.getMetadata("spybedwars_team").get(0).asString();
                if (targetTeam.equals(golemTeam)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Snowball snowball && snowball.hasMetadata("ice_fish")) {
            if (snowball.getShooter() instanceof Player player) {
                Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
                if (arenaOpt.isPresent()) {
                    Arena arena = arenaOpt.get();
                    Location loc = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0, 1, 0) : event.getEntity().getLocation();
                    org.bukkit.entity.Silverfish silverfish = loc.getWorld().spawn(loc, org.bukkit.entity.Silverfish.class);
                    silverfish.setCustomName(ChatColor.AQUA + "Ice Fish");
                    silverfish.setCustomNameVisible(true);
                    arena.addPersistentEntity(silverfish);
                    
                    Team playerTeam = arena.getTeam(player);
                    if (playerTeam != null) {
                        silverfish.setMetadata("spybedwars_team", new org.bukkit.metadata.FixedMetadataValue(SpyBedWars.getInstance(), playerTeam.getName()));
                        // Target nearest enemy
                        for (org.bukkit.entity.Entity nearby : silverfish.getNearbyEntities(10, 10, 10)) {
                            if (nearby instanceof Player p && arena.getPlayers().contains(p.getUniqueId())) {
                                Team targetTeam = arena.getTeam(p);
                                if (targetTeam != null && !targetTeam.getName().equals(playerTeam.getName())) {
                                    silverfish.setTarget(p);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            Team team = arena.getTeams().stream()
                    .filter(t -> t.getMembers().contains(player.getUniqueId()))
                    .findFirst().orElse(null);
            
            String format;
            if (team != null) {
                format = team.getColor().getChatColor() + "[" + team.getName() + "] " + 
                         ChatColor.GRAY + player.getName() + ": " + ChatColor.WHITE + "%2$s";
            } else {
                format = ChatColor.GRAY + "[SPECTATOR] " + player.getName() + ": %2$s";
            }
            
            event.setFormat(format);
            
            // Limit chat to players in the same arena
            event.getRecipients().clear();
            event.getRecipients().addAll(org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .filter(p -> arena.getPlayers().contains(p.getUniqueId()))
                    .toList());
        }
    }

    @EventHandler
    public void onInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ArmorStand as) {
            if (as.hasMetadata("spybedwars_bed") || as.hasMetadata("spybedwars_generator")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.ArmorStand as) {
            if (as.hasMetadata("spybedwars_bed") || as.hasMetadata("spybedwars_generator")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Silverfish silverfish && silverfish.hasMetadata("spybedwars_team")) {
            if (event.getTarget() instanceof Player target) {
                String teamName = silverfish.getMetadata("spybedwars_team").get(0).asString();
                Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(target);
                if (arenaOpt.isPresent()) {
                    Team targetTeam = arenaOpt.get().getTeam(target);
                    if (targetTeam != null && targetTeam.getName().equals(teamName)) {
                        event.setCancelled(true); // Don't target teammates
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() == GameState.PLAYING) {
                // Spawn protection check
                Location placeLoc = event.getBlock().getLocation();
                for (Team team : arena.getTeams()) {
                    if (team.getSpawnPoint() != null && team.getSpawnPoint().getWorld().equals(placeLoc.getWorld())) {
                        if (team.getSpawnPoint().distanceSquared(placeLoc) < 16) { // 4 block radius
                            player.sendMessage(ChatColor.RED + "You cannot place blocks near team spawns!");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }

                if (event.getBlock().getType() == Material.TNT) {
                    event.getBlock().setType(Material.AIR);
                    org.bukkit.entity.TNTPrimed tnt = event.getBlock().getWorld().spawn(event.getBlock().getLocation().add(0.5, 0.5, 0.5), org.bukkit.entity.TNTPrimed.class);
                    tnt.setFuseTicks(52); // Approx 2.6 seconds
                    arena.addPersistentEntity(tnt); // Track for cleanup
                    return;
                }
                arena.addPlacedBlock(event.getBlock().getLocation());
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);

        if (!arenaOpt.isPresent()) return;
        Arena arena = arenaOpt.get();

        if (arena.getGameState() != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (block.getType().name().contains("_BED")) {
            event.setDropItems(false);
            for (Team team : arena.getTeams()) {
                if (team.getBedLocation() != null && team.getBedLocation().distance(block.getLocation()) < 2) {
                    if (team.getMembers().contains(player.getUniqueId())) {
                        sendMessage(player, "game.cannot-break-own-bed");
                        event.setCancelled(true);
                        return;
                    }
                    team.setBedBroken(true);
                    arena.getBedBreaks().put(player.getUniqueId(), arena.getBedBreaks().getOrDefault(player.getUniqueId(), 0) + 1);
                    broadcastMessage(arena, "game.bed-broken",
                            "{team_color}", team.getColor().getChatColor().toString(),
                            "{team_name}", team.getName(),
                            "{player}", player.getName());
                    return;
                }
            }
        } else if (!arena.isPlacedBlock(block.getLocation())) {
            sendMessage(player, "game.cannot-break-block");
            event.setCancelled(true);
        } else {
            arena.removePlacedBlock(block.getLocation());
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        org.bukkit.World world = event.getLocation().getWorld();
        if (world == null) return;

        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenas().stream()
                .filter(a -> a.getGameState() == GameState.PLAYING)
                .filter(a -> a.getWaitingLobby() != null && a.getWaitingLobby().getWorld().getName().equals(world.getName()))
                .findFirst();

        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            Iterator<Block> iterator = event.blockList().iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                // Protect non-placed blocks and blast-proof glass
                if (!arena.isPlacedBlock(block.getLocation()) || block.getType().name().contains("GLASS")) {
                    iterator.remove();
                } else {
                    arena.removePlacedBlock(block.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        // Only handle natural spawns in arena worlds
        if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL) {
            for (Arena arena : SpyBedWars.getInstance().getArenaManager().getArenas()) {
                if (arena.getTeams().isEmpty()) continue;
                org.bukkit.Location spawn = arena.getTeams().get(0).getSpawnPoint();
                if (spawn != null && spawn.getWorld().equals(event.getLocation().getWorld())) {
                    // It's an arena world, cancel natural hostile spawns
                    org.bukkit.entity.EntityType type = event.getEntityType();
                    if (isHostile(type)) {
                        event.setCancelled(true);
                    }
                    break;
                }
            }
        }
    }

    private boolean isHostile(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, WITCH, ENDERMAN, SLIME, HUSK, STRAY, SILVERFISH, MAGMA_CUBE, PHANTOM -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() == GameState.PLAYING) {
                event.setDeathMessage(null);
                
                Team playerTeam = arena.getTeam(player);
                if (playerTeam != null) {
                    // Store if they can respawn at the moment of death
                    canRespawnAtDeath.put(player.getUniqueId(), !playerTeam.isBedBroken());
                }

                // Drop resources for killer
                if (killer != null) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item == null) continue;
                        Material type = item.getType();
                        if (type == Material.IRON_INGOT || type == Material.GOLD_INGOT || 
                            type == Material.DIAMOND || type == Material.EMERALD) {
                            
                            java.util.Map<Integer, ItemStack> remaining = killer.getInventory().addItem(item.clone());
                            if (!remaining.isEmpty()) {
                                remaining.values().forEach(remainingItem -> 
                                    killer.getWorld().dropItemNaturally(killer.getLocation(), remainingItem));
                            }
                            
                            TextComponent lostMsg = new TextComponent(ChatColor.GRAY + "Lost resources to " + killer.getName());
                            lostMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.RED + "You were killed!")));
                            lostMsg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                            player.spigot().sendMessage(lostMsg);

                            TextComponent gainMsg = new TextComponent(ChatColor.GRAY + "Gained resources from " + player.getName());
                            gainMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Nice kill!")));
                            gainMsg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                            killer.spigot().sendMessage(gainMsg);
                        }
                    }
                }
                
                event.getDrops().clear(); // Prevent actual item drops on ground
                
                // Store respawn state BEFORE broadcasting messages
                boolean canRespawn = playerTeam != null && !playerTeam.isBedBroken();
                canRespawnAtDeath.put(player.getUniqueId(), canRespawn);

                if (!canRespawn) {
                    sendMessage(player, "game.eliminated");
                    if (killer != null && !killer.equals(player)) {
                        arena.getFinalKills().put(killer.getUniqueId(), arena.getFinalKills().getOrDefault(killer.getUniqueId(), 0) + 1);
                        broadcastMessage(arena, "game.final-kill", "{player}", player.getName(), "{killer}", killer.getName());
                    } else if (player.getLocation().getY() <= 0) {
                        broadcastMessage(arena, "game.final-void-death", "{player}", player.getName());
                    }
                } else {
                    if (killer != null && !killer.equals(player)) {
                        arena.getKills().put(killer.getUniqueId(), arena.getKills().getOrDefault(killer.getUniqueId(), 0) + 1);
                        broadcastMessage(arena, "game.kill", "{player}", player.getName(), "{killer}", killer.getName());
                    } else if (player.getLocation().getY() <= 0) {
                        broadcastMessage(arena, "game.void-death", "{player}", player.getName());
                    }
                    
                    // Downgrade tools on death
                    int currentPick = arena.getPickaxeLevels().getOrDefault(player.getUniqueId(), 0);
                    if (currentPick > 1) {
                        arena.getPickaxeLevels().put(player.getUniqueId(), currentPick - 1);
                    } else {
                        arena.getPickaxeLevels().remove(player.getUniqueId());
                    }

                    int currentAxe = arena.getAxeLevels().getOrDefault(player.getUniqueId(), 0);
                    if (currentAxe > 1) {
                        arena.getAxeLevels().put(player.getUniqueId(), currentAxe - 1);
                    } else {
                        arena.getAxeLevels().remove(player.getUniqueId());
                    }
                }

                // Auto respawn everyone in 1 tick
                org.bukkit.Bukkit.getScheduler().runTaskLater(SpyBedWars.getInstance(), () -> {
                    if (player.isOnline() && player.isDead()) {
                        player.spigot().respawn();
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);

        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() == GameState.PLAYING) {
                Team playerTeam = arena.getTeams().stream()
                        .filter(team -> team.getMembers().contains(player.getUniqueId()))
                        .findFirst().orElse(null);

                // Set respawn location to spectator point immediately
                if (arena.getSpectatorLocation() != null) {
                    event.setRespawnLocation(arena.getSpectatorLocation());
                }

                // Use a slight delay to set spectator mode and start sequence to ensure client sync
                org.bukkit.Bukkit.getScheduler().runTaskLater(SpyBedWars.getInstance(), () -> {
                    if (player.isOnline() && arena.getGameState() == GameState.PLAYING) {
                        startRespawnSequence(player, arena, playerTeam);
                    }
                }, 1L);
            }
        }
    }

    private void giveTools(Player player, Arena arena) {
        int pickLevel = arena.getPickaxeLevels().getOrDefault(player.getUniqueId(), 0);
        int axeLevel = arena.getAxeLevels().getOrDefault(player.getUniqueId(), 0);

        if (pickLevel > 0) {
            Material mat = Material.WOODEN_PICKAXE;
            int eff = 1;
            switch (pickLevel) {
                case 2 -> mat = Material.STONE_PICKAXE;
                case 3 -> { mat = Material.IRON_PICKAXE; eff = 2; }
                case 4 -> { mat = Material.DIAMOND_PICKAXE; eff = 3; }
            }
            ItemStack pick = new ItemStack(mat);
            ItemMeta meta = pick.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, eff, true);
            pick.setItemMeta(meta);
            player.getInventory().addItem(pick);
        }

        if (axeLevel > 0) {
            Material mat = Material.WOODEN_AXE;
            int eff = 1;
            switch (axeLevel) {
                case 2 -> mat = Material.STONE_AXE;
                case 3 -> { mat = Material.IRON_AXE; eff = 2; }
                case 4 -> { mat = Material.DIAMOND_AXE; eff = 3; }
            }
            ItemStack axe = new ItemStack(mat);
            ItemMeta meta = axe.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, eff, true);
            axe.setItemMeta(meta);
            player.getInventory().addItem(axe);
        }
    }

    private void applyPermanentArmor(Player player, int level) {
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (!arenaOpt.isPresent()) return;
        Team team = arenaOpt.get().getTeam(player);
        if (team == null) return;
        org.bukkit.Color color = team.getColor().getColor();

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        
        org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
        meta.setColor(color);
        helmet.setItemMeta(meta);
        
        meta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
        meta.setColor(color);
        chestplate.setItemMeta(meta);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);

        Material boots = Material.LEATHER_BOOTS;
        Material leggings = Material.LEATHER_LEGGINGS;
        
        switch (level) {
            case 1 -> { boots = Material.CHAINMAIL_BOOTS; leggings = Material.CHAINMAIL_LEGGINGS; }
            case 2 -> { boots = Material.IRON_BOOTS; leggings = Material.IRON_LEGGINGS; }
            case 3 -> { boots = Material.DIAMOND_BOOTS; leggings = Material.DIAMOND_LEGGINGS; }
        }
        
        ItemStack bootsItem = new ItemStack(boots);
        ItemStack leggingsItem = new ItemStack(leggings);
        
        if (boots == Material.LEATHER_BOOTS) {
            meta = (org.bukkit.inventory.meta.LeatherArmorMeta) bootsItem.getItemMeta();
            meta.setColor(color);
            bootsItem.setItemMeta(meta);
        }
        if (leggings == Material.LEATHER_LEGGINGS) {
            meta = (org.bukkit.inventory.meta.LeatherArmorMeta) leggingsItem.getItemMeta();
            meta.setColor(color);
            leggingsItem.setItemMeta(meta);
        }

        player.getInventory().setBoots(bootsItem);
        player.getInventory().setLeggings(leggingsItem);
    }

    @EventHandler
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() != GameState.PLAYING) {
                event.setCancelled(true);
            } else if (event.getItemDrop().getItemStack().getType().name().contains("_BED")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBedPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getItem().getItemStack().getType().name().contains("_BED")) {
            if (SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player).isPresent()) {
                event.setCancelled(true);
                event.getItem().remove();
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            event.setFoodLevel(20);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() != GameState.PLAYING) {
                event.setCancelled(true);
                return;
            }

            // Prevent armor damage
            if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent || 
                event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE) {
                
                // For 1.8-like BedWars, armor doesn't take durability damage
                org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents();
                for (org.bukkit.inventory.ItemStack item : armor) {
                    if (item != null && item.getType() != Material.AIR) {
                        item.setDurability((short) 0);
                    }
                }
                player.getInventory().setArmorContents(armor);
            }
        }
    }

    @EventHandler
    public void onRegen(org.bukkit.event.entity.EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() == GameState.PLAYING) {
                // BedWars disables natural regeneration (SATIATED)
                // Regeneration should only come from Golden Apples (REGEN/MAGIC) 
                // or Team Upgrade Heal Pool (which we apply via PotionEffect)
                if (event.getRegainReason() == org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() > 0) return;

        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            if (arena.getGameState() == GameState.PLAYING && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setHealth(0); // Trigger death event
                }
        }
    }

    private void startRespawnSequence(Player player, Arena arena, Team team) {
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        
        // Check if they were already doomed at the moment of death
        boolean wasBedBroken = !canRespawnAtDeath.getOrDefault(player.getUniqueId(), true);
        canRespawnAtDeath.remove(player.getUniqueId());

        if (wasBedBroken) {
            TextComponent msg = new TextComponent(ChatColor.RED + "Your bed was broken! You are now a spectator.");
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to leave the game")));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw leave"));
            player.spigot().sendMessage(msg);
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (!player.isOnline() || arena.getGameState() != GameState.PLAYING || !arena.getPlayers().contains(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                if (seconds > 0) {
                    player.sendTitle(ChatColor.RED + "YOU DIED!", ChatColor.YELLOW + "Respawning in " + ChatColor.RED + seconds + ChatColor.YELLOW + " seconds...", 0, 21, 0);
                    seconds--;
                } else {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    
                    if (team != null && team.getSpawnPoint() != null) {
                        player.teleport(team.getSpawnPoint());
                    }
                    
                    player.sendTitle(ChatColor.GREEN + "RESPAWNED!", "", 0, 20, 10);
                    
                    // Re-equip items
                    int armorLevel = arena.getArmorLevels().getOrDefault(player.getUniqueId(), 0);
                    applyPermanentArmor(player, armorLevel);
                    player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                    giveTools(player, arena);
                    if (arena.getHasShears().getOrDefault(player.getUniqueId(), false)) {
                        player.getInventory().addItem(new ItemStack(Material.SHEARS));
                    }
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(SpyBedWars.getInstance(), 20, 20);
    }

    private void sendMessage(Player player, String path) {
        String prefix = SpyBedWars.getInstance().getMessagesConfig().getString("prefix", "&8[&bSpyBedWars&8] ");
        String msg = SpyBedWars.getInstance().getMessagesConfig().getString(path, path);
        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', prefix + msg));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "SpyBedWars Notification")));
        
        if (path.contains("eliminated")) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw leave"));
        } else {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
        }
        
        player.spigot().sendMessage(component);
    }

    private void broadcastMessage(Arena arena, String path, String... placeholders) {
        String prefix = SpyBedWars.getInstance().getMessagesConfig().getString("prefix", "&8[&bSpyBedWars&8] ");
        String msg = SpyBedWars.getInstance().getMessagesConfig().getString(path, path);
        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        String finalMsg = ChatColor.translateAlternateColorCodes('&', prefix + msg);
        TextComponent component = new TextComponent(finalMsg);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Game Notification")));
        
        if (path.contains("bed-broken") || path.contains("kill")) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw leave"));
        }

        arena.getPlayers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.spigot().sendMessage(component);
        });
    }
}
