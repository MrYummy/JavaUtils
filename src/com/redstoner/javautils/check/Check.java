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
	
	@SuppressWarnings("deprecation")
	@Command(hook = "checkCommand")
	public void checkCommand(final CommandSender sender, final String player) {
		msg(sender, "\n&2--=[ Check ]=--");
		msg(sender, "&7Please notice that the data may not be fully accurate!");
		OfflinePlayer oPlayer = Bukkit.getServer().getOfflinePlayer(player);
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				sendData(sender, oPlayer);
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
		String ip = "";
		
		if (player.isOnline()) {
			ip = player.getPlayer().getAddress().getHostString();
		} else {
			try {
				ip = (String) table.get("last_ip", new MysqlConstraint("uuid", ConstraintOperator.EQUAL, player.getUniqueId().toString().replace("-", "")))[0];
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		try {
			URL ipinfo = new URL("http://ipinfo.io/" + ip + "/json");
			
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
	
	public Object[] getWebsiteData(OfflinePlayer player) {
		MysqlConstraint constraint = new MysqlConstraint("uuid", ConstraintOperator.EQUAL, player.getUniqueId().toString().replace("-", ""));
		
		try {
			int id = (int) table.get("id", constraint)[0];
			String email = (String) table.get("email", constraint)[0];
			boolean confirmed = (boolean) table.get("confirmed", constraint)[0];
			
			return new Object[] {"https://redstoner.com/users/" + id, email, confirmed};
		} catch (Exception e) {
			return new Object[] {null};
		}
	}
	
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
			
			nameString = nameString.substring(0, nameString.length() - 2);
			
			return nameString;
		} catch (MalformedURLException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void msg(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}
	
	public void sendData(CommandSender sender, OfflinePlayer player) {
		JSONObject ipInfo = getIpInfo(player);
		
		try {
			// data
			String firstJoin = getFirstJoin(player);
			String lastSeen = getFirstJoin(player);
			firstJoin = (firstJoin.equals("1970-1-1 1:0")) ? "&eNever" : "&7(y-m-d h:m:s) &e" + firstJoin;
			lastSeen = (lastSeen.equals("1970-1-1 1:0")) ? "&eNever" : "&7(y-m-d h:m:s) &e" + lastSeen;
			
			Object[] websiteData = getWebsiteData(player);
			String websiteUrl = (websiteData[0] == null) ? "None" : (String) websiteData[0];
			String email = (websiteData[0] == null) ? "Unknown" : (String) websiteData[1];
			boolean emailNotConfirmed = (websiteData[0] == null) ? false : !((boolean) websiteData[2]);
			
			String country = (ipInfo == null) ? "Unknown" : getCountry(ipInfo);
			
			String namesUsed = getAllNames(player);
			if (namesUsed == null) namesUsed = "None";
			
			// messages
			msg(sender, "&7Data provided by Redstoner:");
			msg(sender, "&6>  UUID: &e" + player.getUniqueId());
			msg(sender, "&6>  First joined: " + firstJoin);
			msg(sender, "&6>  Last seen: " + lastSeen);
			
			msg(sender, "&6>  Website account: &e" + websiteUrl);
			msg(sender, "&6>  email: &e" + email);
			if (emailNotConfirmed) msg(sender, "&6> &4Email NOT Confirmed!");
			
			msg(sender, "&7Data provided by ipinfo:");
			msg(sender, "&6>  Country: &e" + country);
			
			msg(sender, "&7Data provided by Mojang:");
			msg(sender, "&6>  All ingame names used so far: &e" + namesUsed);
		} catch (Exception e) {
			e.printStackTrace();
			msg(sender, "&cSorry, something went wrong while fetching data");
		}
	}
}
