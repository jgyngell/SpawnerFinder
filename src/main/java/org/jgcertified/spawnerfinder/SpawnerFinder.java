package org.jgcertified.spawnerfinder;

import java.util.*;

import io.papermc.lib.PaperLib;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main SpawnerFinder class
 *
 * @author Julian Gyngell.
 */
public class SpawnerFinder extends JavaPlugin {

	Material markerBlock;
	boolean allowMarkers;
	int radius;
	int minDist;

	FileConfiguration config = getConfig();

	@Override
	public void onEnable() {
		PaperLib.suggestPaper(this);

		this.getLogger().info("Starting SpawnFinder");

		initConfig();
		loadConfig();

		this.getLogger().info("SpawnFinder started");

		this.getCommand("scompass").setExecutor(new CommandFind());
	}

	public void initConfig()
	{
		config.addDefault("radius", 3);
		config.addDefault("mindistance", 5);
		config.addDefault("allow_markers", true);
		config.addDefault("marker", "GOLD_BLOCK");

		config.options().copyDefaults(true);

		saveConfig();		
	}

	public void loadConfig()
	{

		allowMarkers = config.getBoolean("allow_markers");

		if (allowMarkers)
		{
			markerBlock = Material.matchMaterial(config.getString("marker"));

			if (markerBlock == null)
			{

				this.getLogger().warning(config.getString("marker") + " was not recognized as a valid block type. Defaulting to GOLD_BLOCK");

				markerBlock = Material.GOLD_BLOCK;

			} else {

				this.getLogger().info("Set the marker block to: " + config.getString("marker"));
			}
		}

		radius = config.getInt("radius");

		this.getLogger().info("Setting search radius to " + radius + " chunks");

		minDist = config.getInt("mindistance");

		this.getLogger().info("Setting minimum distance to " + minDist + " blocks");
	}

	public class CommandFind implements CommandExecutor {

		// This method is called, when somebody uses our command
		@Override
		public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
			if (sender instanceof Player) {
				Player player = (Player) sender;

				player.sendMessage("Looking for spawners");

				Location spawnerLoc = findSpawnerInChunks(player.getLocation().clone(), radius);				

				if(spawnerLoc != null)
				{
					PlayerInventory pi = player.getInventory();
					ItemStack compass = new ItemStack(Material.COMPASS);

					//player.sendMessage("Found spawner at: " + spawnerLoc.getX() + " " + spawnerLoc.getY() + " " + spawnerLoc.getZ());
					if (pi.contains(compass) || pi.getItemInOffHand().getType() == Material.COMPASS)
					{
						player.sendMessage("Spawner found! Happy hunting!");
						player.setCompassTarget(spawnerLoc);
					} else
					{
						player.sendMessage("Spawner found! But you forgot a compass! Oops.");
						return true;
					}


				} else {

					player.sendMessage("No spawners nearby. Better luck next time!");

				}
			}

			return true;
		}

		public Location findSpawnerInChunks(Location player, int radius)
		{

			Location ret = null;
			double dist = 999999;

			Chunk chunk = player.getChunk();

			for(Chunk c : around(chunk, radius))
			{
				ChunkSnapshot snap = c.getChunkSnapshot();

				for (int x = 0; x < 16; x++)
				{
					for (int z = 0; z < 16; z++)
					{
						for (int y = 0; y < snap.getHighestBlockYAt(x, z); y++)
						{
							//Is this block a spawner, and is it unmarked?
							if(snap.getBlockType(x, y, z) == Material.SPAWNER && (snap.getBlockType(x, y + 1, z) != markerBlock && allowMarkers))
							{

								Location sLoc = c.getBlock(x, y, z).getLocation();
								double newDist = player.distance(sLoc);
								if(newDist > minDist)//Ignore a spawner if you're within 5 blocks of it
								{
									if(newDist < dist)
									{
										ret = sLoc.clone();
										dist = newDist;
									}
								}
							}	
						}
					}
				}
			}

			return ret;
		}

		public Collection<Chunk> around(Chunk origin, int radius) {
			World world = origin.getWorld();

			int length = (radius * 2) + 1;
			HashSet<Chunk> chunks = new HashSet<>(length * length);

			int cX = origin.getX();
			int cZ = origin.getZ();

			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					chunks.add(world.getChunkAt(cX + x, cZ + z));
				}
			}
			return chunks;
		}

	}

}
