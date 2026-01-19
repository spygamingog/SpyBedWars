package com.spygamingog.spybedwars.arena.team;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Team {

    private final String name;
    private final TeamColor color;
    private final List<UUID> members;
    
    private Location spawnPoint;
    private Location bedLocation;
    private Location shopNPC;
    private Location upgradeNPC;
    private boolean bedBroken;
    private transient org.bukkit.entity.ArmorStand bedHologram;
    
    private final java.util.Map<String, Integer> upgrades = new java.util.HashMap<>();

    public Team(String name, TeamColor color) {
        this.name = name;
        this.color = color;
        this.members = new ArrayList<>();
        this.bedBroken = false;
    }

    public void addMember(Player player) {
        if (!members.contains(player.getUniqueId())) {
            members.add(player.getUniqueId());
        }
    }
}
