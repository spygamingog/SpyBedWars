package com.spygamingog.spybedwars.npc;

import com.spygamingog.spynpcs.SpyNPCs;
import com.spygamingog.spynpcs.models.SpyNPC;
import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class NPCManager {

    private boolean isSpyNpcsEnabled() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SpyNPCs");
    }

    public void spawnShopNPC(Location location, String arenaName, String teamName) {
        if (!isSpyNpcsEnabled()) {
            SpyBedWars.getInstance().getLogger().warning("SpyNPCs is not enabled! Cannot spawn Shop NPC.");
            return;
        }

        String name = ChatColor.YELLOW + "" + ChatColor.BOLD + "ITEM SHOP";
        UUID uuid = UUID.nameUUIDFromBytes((arenaName + "shop" + teamName).getBytes());
        
        // If NPC already exists, don't respawn
        if (SpyNPCs.getInstance().getNpcManager().getNPC(uuid) != null) {
            return;
        }
        
        // Remove existing if any (just in case)
        removeNPC(uuid);

        SpyNPC npc = SpyNPC.builder()
                .uuid(uuid)
                .entityId((int) (Math.random() * 1000000) + 2000)
                .name(name)
                .location(location)
                .type(EntityType.VILLAGER)
                .actions(new ArrayList<>())
                .build();

        // Add action to open shop
        npc.getActions().add(SpyNPC.NPCAction.builder()
                .type(SpyNPC.ActionType.COMMAND)
                .value("bw npcshop")
                .build());

        SpyNPCs.getInstance().getNpcManager().createNPC(npc);
        SpyBedWars.getInstance().getLogger().info("Spawned Shop NPC for arena: " + arenaName + " team: " + teamName);
    }

    public void spawnUpgradeNPC(Location location, String arenaName, String teamName) {
        if (!isSpyNpcsEnabled()) {
            SpyBedWars.getInstance().getLogger().warning("SpyNPCs is not enabled! Cannot spawn Upgrade NPC.");
            return;
        }

        String name = ChatColor.AQUA + "" + ChatColor.BOLD + "TEAM UPGRADES";
        UUID uuid = UUID.nameUUIDFromBytes((arenaName + "upgrade" + teamName).getBytes());

        // If NPC already exists, don't respawn
        if (SpyNPCs.getInstance().getNpcManager().getNPC(uuid) != null) {
            return;
        }

        // Remove existing if any (just in case)
        removeNPC(uuid);

        SpyNPC npc = SpyNPC.builder()
                .uuid(uuid)
                .entityId((int) (Math.random() * 1000000) + 2000)
                .name(name)
                .location(location)
                .type(EntityType.WITCH)
                .actions(new ArrayList<>())
                .build();

        // Add action to open upgrades
        npc.getActions().add(SpyNPC.NPCAction.builder()
                .type(SpyNPC.ActionType.COMMAND)
                .value("bw npcupgrade")
                .build());

        SpyNPCs.getInstance().getNpcManager().createNPC(npc);
        SpyBedWars.getInstance().getLogger().info("Spawned Upgrade NPC for arena: " + arenaName + " team: " + teamName);
    }

    public void removeNPCs(Arena arena) {
        if (!isSpyNpcsEnabled()) return;

        for (Team team : arena.getTeams()) {
            UUID shopUuid = UUID.nameUUIDFromBytes((arena.getArenaName() + "shop" + team.getName()).getBytes());
            UUID upgradeUuid = UUID.nameUUIDFromBytes((arena.getArenaName() + "upgrade" + team.getName()).getBytes());
            
            removeNPC(shopUuid);
            removeNPC(upgradeUuid);
        }
        SpyBedWars.getInstance().getLogger().info("Removed all NPCs for arena: " + arena.getArenaName());
    }

    public void removeNPC(UUID uuid) {
        if (!isSpyNpcsEnabled()) return;
        SpyNPCs.getInstance().getNpcManager().removeNPC(uuid);
    }

    public void onPlayerJoin(Player player) {
        // SpyNPCs handles visibility automatically
    }
}
