package simple.parkour;

import java.util.*;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public class InWorldSystem implements CommandExecutor, Listener {

    // Settings
    private static final Material doneBlocksMaterial = Material.IRON_BLOCK;
    private static final Material nextBlockMaterial = Material.DIAMOND_BLOCK;
    private static final String permission = "command.in-world";

    // Data
    public static final Map<UUID, Location> playerNextJump = new HashMap<>();
    public static Map<UUID, List<Location>> blocks = new HashMap<>();
    public static final Map<UUID, Integer> points = new HashMap<>();
    public static String color = Main.color;

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player player) {
            if (!player.hasPermission(permission)) {
                player.sendMessage("§8[" + color + "parkour§8]§7 You need following permission: " + permission);
                return false;
            }

            Block blockUnder = player.getWorld().getBlockAt(player.getLocation().add(0.0, -1.0, 0.0));
            UUID uuid = player.getUniqueId();
            if (playerNextJump.get(uuid) != null) {
                player.sendMessage("§8[" + color + "parkour§8]§7 It seems like you are still in a parkour..");
                return false;
            }

            if (!blockUnder.getType().equals(Material.AIR)) {
                player.teleport(player.getLocation().add(0.0, 30.0, 0.0));
            }

            blockUnder = player.getWorld().getBlockAt(player.getLocation().add(0.0, -1.0, 0.0));
            player.sendMessage("§8[" + color + "parkour§8]§r The parkour has startet!");
            blockUnder.setType(Material.IRON_BLOCK);
            Location nextJumLocation = calcNextJump(blockUnder.getLocation());
            player.getWorld().getBlockAt(nextJumLocation).setType(Material.DIAMOND_BLOCK);
            playerNextJump.put(uuid, nextJumLocation);
            List<Location> removeBlocks = new ArrayList<>();
            removeBlocks.add(blockUnder.getLocation());
            removeBlocks.add(nextJumLocation);
            blocks.put(uuid, removeBlocks);
            points.getOrDefault(uuid, 0);
        } else sender.sendMessage("§8[" + color + "parkour§8]§7 Only players can play parkour!");
        return false;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        Player player = event.getPlayer();
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
            Block targetBlock = to.getBlock().getRelative(BlockFace.DOWN);
            Location nextJumLocation;
            UUID uuid = player.getUniqueId();
            if (playerNextJump.containsKey(uuid) && player.getLocation().getBlockY() < (playerNextJump.get(uuid)).getBlockY()) {
                // Failed System
                player.sendMessage("§8[" + color + "parkour§8]§c Failed with " + points.getOrDefault(uuid, 0) + " Points!");
                playerNextJump.remove(uuid);

                for (Location location : blocks.get(uuid)) {
                    event.getPlayer().getWorld().getBlockAt(location).setType(Material.AIR);
                    event.getPlayer().getWorld().spawnParticle(Particle.FLASH, location, 5);
                }

                blocks.remove(uuid);
                points.remove(uuid);

            } else if (targetBlock.getType() == nextBlockMaterial && playerNextJump.get(uuid) != null && playerNextJump.get(uuid).equals(targetBlock.getLocation())) {
                // Next Block System
                targetBlock.setType(doneBlocksMaterial);
                player.playSound(targetBlock.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                int points = InWorldSystem.points.getOrDefault(uuid, 0) + 1;
                InWorldSystem.points.put(uuid, points);
                String color;

                switch (points) {
                    case 25 -> color = "§3";
                    case 50,100,150,200,250 -> color = "§e";
                    case 159 -> color = "§b"; // Developer Rekord
                    default -> color = "";
                }

                player.sendActionBar(Component.text(color + points + " Point" + (points > 1 ? "s" : "")));

                do {
                    nextJumLocation = calcNextJump(targetBlock.getLocation());
                } while (event.getPlayer().getWorld().getBlockAt(nextJumLocation).getType() != Material.AIR);

                event.getPlayer().getWorld().getBlockAt(nextJumLocation).setType(Material.DIAMOND_BLOCK);
                playerNextJump.put(uuid, nextJumLocation);
                List<Location> removeBlocks = blocks.get(uuid);
                removeBlocks.add(nextJumLocation);
                blocks.put(uuid, removeBlocks);
            }
        }
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        boolean parkourBlock = false;
        Block block = event.getBlock();
        if (block.getType() != doneBlocksMaterial && block.getType() != nextBlockMaterial) return;
        for (List<Location> lists : blocks.values()) {
            for (Location location : lists) {
                if (parkourBlock) break;
                if (block.getLocation().equals(location)) {
                    parkourBlock = true;
                    break;
                }
            }
        }

        if (parkourBlock) {
            player.sendMessage("§8[" + color + "parkour§8]§r You cant mine a Block from a parkour!");
            event.setCancelled(true);
        }
    }

    private Location calcNextJump(Location old) {
        int deltaX = this.getRandomDelta(3);
        int deltaY = this.getRandomDelta(1);
        int deltaZ = this.getRandomDelta(3);
        return deltaX == 0 && deltaZ == 0 ? old.clone().add(1.0, deltaY, 1.0) : old.clone().add(deltaX, deltaY, deltaZ);
    }

    private int getRandomDelta(int bound) {
        Random random = new Random();
        return bound == 1 ? random.nextInt(2) : random.nextInt(bound * 2) - bound;
    }

    public static void removeBlocks() {
        if (blocks.isEmpty()) return;
        for (List<Location> value : blocks.values()) {
            for (Location location : value) location.getBlock().setType(Material.AIR);
        }


        for (Player player : Bukkit.getOnlinePlayers()) {
            Block block = player.getLocation().add(0, -1, 0).getBlock();
            if (block.getType() == Material.AIR) block.setType(Material.DEEPSLATE_TILES);
        }
    }
}
