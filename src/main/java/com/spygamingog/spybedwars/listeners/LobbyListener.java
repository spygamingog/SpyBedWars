package com.spygamingog.spybedwars.listeners;

import com.spygamingog.spybedwars.SpyBedWars;
import com.spygamingog.spybedwars.arena.Arena;
import com.spygamingog.spybedwars.arena.TeamSelector;
import com.spygamingog.spybedwars.managers.LobbyItemManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class LobbyListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        LobbyItemManager lim = SpyBedWars.getInstance().getLobbyItemManager();

        if (lim.isLeaveGame(item)) {
            event.setCancelled(true);
            Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
            if (arenaOpt.isPresent()) {
                arenaOpt.get().removePlayer(player);
                TextComponent msg = new TextComponent(ChatColor.YELLOW + "You have left the game.");
                msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to see all arenas")));
                msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                player.spigot().sendMessage(msg);
            } else {
                TextComponent msg = new TextComponent(ChatColor.RED + "You are not in a game!");
                msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to join an arena")));
                msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw list"));
                player.spigot().sendMessage(msg);
            }
        } else if (isTeamSelector(item)) {
            event.setCancelled(true);
            Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
            arenaOpt.ifPresent(arena -> TeamSelector.open(player, arena));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Select a Team")) return;
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null || !item.hasItemMeta()) return;
        
        Optional<Arena> arenaOpt = SpyBedWars.getInstance().getArenaManager().getArenaByPlayer(player);
        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            String teamName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            arena.getTeams().stream()
                .filter(t -> t.getName().equalsIgnoreCase(teamName))
                .findFirst()
                .ifPresent(team -> {
                    if (team.getMembers().size() >= arena.getArenaType().getTeamSize()) {
                        TextComponent msg = new TextComponent(ChatColor.RED + "That team is full!");
                        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to select another team")));
                        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bw help"));
                        player.spigot().sendMessage(msg);
                        return;
                    }
                    
                    // Remove from old team
                    arena.getTeams().forEach(t -> t.getMembers().remove(player.getUniqueId()));
                    
                    // Add to new team
                    team.addMember(player);
                    TextComponent msg = new TextComponent(ChatColor.GREEN + "You joined the " + team.getColor().getChatColor() + team.getName() + ChatColor.GREEN + " team!");
                    msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GRAY + "Click to leave the game")));
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bw leave"));
                    player.spigot().sendMessage(msg);
                    
                    // Update wool color in hand
                    ItemStack selector = TeamSelector.getTeamSelectorItem();
                    selector.setType(team.getColor().getMaterial());
                    player.getInventory().setItemInMainHand(selector);

                    // Update Tab and Name color
                    player.setDisplayName(team.getColor().getChatColor() + player.getName() + ChatColor.WHITE);
                    player.setPlayerListName(team.getColor().getChatColor() + player.getName());

                    player.closeInventory();
                });
        }
    }

    private boolean isTeamSelector(ItemStack item) {
        ItemStack target = TeamSelector.getTeamSelectorItem();
        if (item == null || !item.hasItemMeta() || !target.hasItemMeta()) return false;
        // Check display name instead of type, as type changes based on selected team
        return item.getItemMeta().getDisplayName().equals(target.getItemMeta().getDisplayName());
    }
}
