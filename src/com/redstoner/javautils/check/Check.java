package com.redstoner.javautils.check;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.event.Listener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.nemez.cmdmgr.Command;
import com.redstoner.moduleLoader.Module;
import com.redstoner.moduleLoader.ModuleLoader;
import com.redstoner.moduleLoader.mysql.elements.ConstraintOperator;
import com.redstoner.moduleLoader.mysql.elements.MysqlConstraint;
import com.redstoner.moduleLoader.mysql.elements.MysqlDatabase;
import com.redstoner.moduleLoader.mysql.elements.MysqlTable;

public class Check extends Module implements Listener {

	MysqlTable table;

	@Override
	public String getDescription() {
		return "Get info on a player.";
	}

	@Override
	public String getName() {
		return "Check";
	}

	@Override
	public void onEnable() {

		ModuleLoader loader = ModuleLoader.getLoader();
		Map<String, String> config = loader.getConfiguration("Check.json");

		if (config == null || !config.containsKey("database") || !config.containsKey("table")) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not load the Check config file, disabling!");

			enabled = false;
			return;
		}

		try {
			MysqlDatabase database = ModuleLoader.getLoader().getMysqlHandler().getDatabase(config.get("database"));

			table = database.getTable(config.get("table"));
		} catch (NullPointerException e) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not use the Check config, disabling!");

			enabled = false;
			return;
		}

	}

	@Override
	public String getCmdManagerString() {
		// @formatter:off
		return "command check {"
				+ "  [string:player] {"
				+ "    run checkCommand player;"
				+ "    help Get info on a player;"
				+ "    perm utils.check;"
				+ "  }"
				+ "}";
		// @formatter:on
	}

	@SuppressWarnings("deprecation")
	@Command(hook = "checkCommand")
	public void checkCommand(final CommandSender sender, final String player) {
		sendHeader(sender);
		msg(sender, "&7Please notice that the data may not be fully accurate!");
		OfflinePlayer oPlayer = Bukkit.getServer().getOfflinePlayer(player);

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				getAllData(sender, oPlayer);
			}

		});
		thread.setDaemon(true);
		thread.start();
	}

	public String read(URL url) {
		String data = "";
		try {

			Scanner in = new Scanner(new InputStreamReader(url.openStream()));

			while (in.hasNextLine()) {
				data += in.nextLine();
			}

			in.close();

			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public JSONObject getIpInfo(OfflinePlayer player) {
		if (!player.isOnline())
			return null;
		try {
			URL ipinfo = new URL("http://ipinfo.io/" + ((CraftPlayer) player).getAddress().getAddress().toString() + "/json");

			String rawJson = read(ipinfo);

			return (JSONObject) new JSONParser().parse(rawJson);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getFirstJoin(OfflinePlayer player) {
		Long firstJoin = player.getFirstPlayed();
		Date date = new Date(firstJoin);
		SimpleDateFormat format = new SimpleDateFormat("y-M-d H:m");
		return format.format(date);
	}

	public String getLastSeen(OfflinePlayer player) {
		Long lastSeen = player.getLastPlayed();
		Date date = new Date(lastSeen);
		SimpleDateFormat format = new SimpleDateFormat("y-M-d H:m");
		return format.format(date);
	}

	/*
	 * 
	 * WEBSITE DATA NOT TESTED.
	 * 
	 */
	public Object[] getWebsiteData(OfflinePlayer player) {
		Object[] results = (Object[]) table.get("`id`, `email`, `confirmed`", new MysqlConstraint("uuid", ConstraintOperator.EQUAL, player.getUniqueId().toString().replace("-", "")))[0];
		return (results == null) ? new Object[] { null, null, false } : new Object[] { "http://redstoner.com/users/" + results[0], results[1], (int) results[2] > 0};
	}
	/*
	 * 
	 * WEBSITE DATA NOT TESTED.
	 * 
	 */

	public String getCountry(JSONObject data) {
		return (String) data.get("country");
	}

	public String getAllNames(OfflinePlayer player) {
		String uuid = player.getUniqueId().toString().replace("-", "");
		String nameString = "";
		try {
			String rawJson = read(new URL("https://api.mojang.com/user/profiles/" + uuid + "/names"));
			System.out.println("name for " + uuid + " : " + rawJson);
			JSONArray names = (JSONArray) new JSONParser().parse(rawJson);

			for (Object obj : names) {
				nameString += ((JSONObject) obj).get("name") + ", ";
			}

			nameString = nameString.substring(0, nameString.length() - 1);

			return nameString;
		} catch (MalformedURLException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void msg(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public void getAllData(CommandSender sender, OfflinePlayer player) {
		JSONObject data = getIpInfo(player);

		try {
			msg(sender, "&7   -- Data provided by Redstoner");
			msg(sender, "&6>  UUID: &e" + player.getUniqueId());
			msg(sender, "&6>  First joined: &7(y-m-d h:m:s) &e" + getFirstJoin(player));
			msg(sender, "&6>  Last seen: &7(y-m-d h:m:s) &e" + getLastSeen(player));
			Object[] website = getWebsiteData(player);
			msg(sender, "&6> Website account: &e" + website[0]);
			msg(sender, "&6> email: &e" + website[1]);
			if (!((boolean) website[2]))
				msg(sender, "&6> &cEmail NOT Confirmed!");
			msg(sender, "&7   -- Data provided by ipinfo.io");
			msg(sender, "&6>  Country: &e" + getCountry(data));
			msg(sender, "&7   -- Data provided by Mojang");
			msg(sender, "&6>  All ingame names used so far: &e" + getAllNames(player));
		} catch (Exception e) {
			e.printStackTrace();
			msg(sender, "&cSorry, something went wrong while fetching data");
		}
	}

	public void sendHeader(CommandSender sender) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n&2--=[ Check ]=--"));
	}

}