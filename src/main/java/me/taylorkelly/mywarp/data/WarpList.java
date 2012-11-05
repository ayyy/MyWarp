package me.taylorkelly.mywarp.data;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import me.taylorkelly.mywarp.LanguageManager;
import me.taylorkelly.mywarp.MyWarp;
import me.taylorkelly.mywarp.sql.WarpDataSource;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpList {
    private HashMap<String, Warp> warpList;
    private Server server;
    private HashMap<String, Warp> welcomeMessage;

    public WarpList(Server server) {
        welcomeMessage = new HashMap<String, Warp>();
        this.server = server;
        WarpDataSource.initialize();
        warpList = WarpDataSource.getMap();
    }

    public boolean warpExists(String name) {
        return warpList.containsKey(name);
    }

    public void addWarp(String name, Player player) {
        Warp warp = new Warp(name, player);
        warpList.put(name, warp);
        WarpDataSource.addWarp(warp);
    }

    private int numWarpsPlayer(Player player) {
        int size = 0;
        for (Warp warp : warpList.values()) {
            String creator = warp.creator;
            if (creator.equals(player.getName()))
                size++;
        }
        return size;
    }

    public boolean playerCanBuildWarp(Player player) {
        if (MyWarp.getWarpPermissions().disobeyTotalLimit(player)) {
            return true;
        } else {
            return numWarpsPlayer(player) < MyWarp.getWarpPermissions().maxTotalWarps(
                    player);
        }
    }

    private int numPublicWarpsPlayer(Player player) {
        int size = 0;
        for (Warp warp : warpList.values()) {
            boolean publicAll = warp.publicAll;
            String creator = warp.creator;
            if (creator.equals(player.getName()) && publicAll)
                size++;
        }
        return size;
    }

    public boolean playerCanBuildPublicWarp(Player player) {
        if (MyWarp.getWarpPermissions().disobeyPublicLimit(player)) {
            return true;
        } else {
            return numPublicWarpsPlayer(player) < MyWarp.getWarpPermissions()
                    .maxPublicWarps(player);
        }
    }

    public boolean playerCanBuildPrivateWarp(Player player) {
        if (MyWarp.getWarpPermissions().disobeyPrivateLimit(player)) {
            return true;
        } else {
            return numPrivateWarpsPlayer(player) < MyWarp.getWarpPermissions()
                    .maxPrivateWarps(player);
        }
    }

    public void addWarpPrivate(String name, Player player) {
        Warp warp = new Warp(name, player, false);
        warpList.put(name, warp);
        WarpDataSource.addWarp(warp);
    }

    private int numPrivateWarpsPlayer(Player player) {
        int size = 0;
        for (Warp warp : warpList.values()) {
            boolean privateAll = !warp.publicAll;
            String creator = warp.creator;
            if (creator.equals(player.getName()) && privateAll)
                size++;
        }
        return size;
    }

    public void blindAdd(Warp warp) {
        warpList.put(warp.name, warp);
    }

    public void warpTo(String name, Player player) {
        Warp warp = warpList.get(name);
        if (warp.warp(player, server)) {
            warp.visits++;
            WarpDataSource.updateVisits(warp);
            player.sendMessage(warp.getSpecificWelcomeMessage(player));
        }
    }

    public String getMatche(String name, Player player) {
        MatchList matches = this.getMatches(name, player);
        return matches.getMatch(name);
    }

    public Warp getWarp(String name) {
        return warpList.get(name);
    }

    public void deleteWarp(String name) {
        Warp warp = warpList.get(name);
        warpList.remove(name);
        WarpDataSource.deleteWarp(warp);
    }

    public void privatize(String name) {
        Warp warp = warpList.get(name);
        warp.publicAll = false;
        WarpDataSource.publicizeWarp(warp, false);
    }

    public void invitePlayer(String name, String inviteeName) {
        Warp warp = warpList.get(name);
        warp.invite(inviteeName);
        WarpDataSource.updatePermissions(warp);
    }

    public void inviteGroup(String name, String inviteeName) {
        Warp warp = warpList.get(name);
        warp.inviteGroup(inviteeName);
        WarpDataSource.updateGroupPermissions(warp);
    }

    public void publicize(String name) {
        Warp warp = warpList.get(name);
        warp.publicAll = true;
        WarpDataSource.publicizeWarp(warp, true);
    }

    public void uninvitePlayer(String name, String inviteeName) {
        Warp warp = warpList.get(name);
        warp.uninvite(inviteeName);
        WarpDataSource.updatePermissions(warp);
    }

    public void uninviteGroup(String name, String inviteeName) {
        Warp warp = warpList.get(name);
        warp.uninviteGroup(inviteeName);
        WarpDataSource.updateGroupPermissions(warp);
    }

    public ArrayList<Warp> getSortedWarps(Player player, int start, int size) {
        return getSortedWarpsPerCreator(player, null, start, size);
    }

    public int getSize() {
        return warpList.size();
    }

    public ArrayList<Warp> getSortedWarpsPerCreator(Player player, String creator,
            int start, int size) {
        ArrayList<Warp> ret = new ArrayList<Warp>();
        List<String> names = new ArrayList<String>(warpList.keySet());
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(names, collator);

        int index = 0;
        int currentCount = 0;
        while (index < names.size() && ret.size() < size) {
            String currName = names.get(index);
            Warp warp = warpList.get(currName);
            if ((player != null ? warp.playerCanWarp(player) : true)
                    && (creator != null ? warp.playerIsCreator(creator) : true)) {
                if (currentCount >= start) {
                    ret.add(warp);
                } else {
                    currentCount++;
                }
            }
            index++;
        }
        return ret;
    }

    public MatchList getMatches(String name, Player player) {
        ArrayList<Warp> exactMatches = new ArrayList<Warp>();
        ArrayList<Warp> matches = new ArrayList<Warp>();

        List<String> names = new ArrayList<String>(warpList.keySet());
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(names, collator);

        for (String currName : names) {
            Warp warp = warpList.get(currName);
            if (player != null ? warp.playerCanWarp(player) : true) {
                if (warp.name.equalsIgnoreCase(name)) {
                    exactMatches.add(warp);
                } else if (warp.name.toLowerCase().contains(name.toLowerCase())) {
                    matches.add(warp);
                }
            }
        }
        if (exactMatches.size() > 1) {
            for (int i = 0; i < exactMatches.size(); i++) {
                Warp warp = exactMatches.get(i);
                if (!warp.name.equals(name)) {
                    exactMatches.remove(warp);
                    matches.add(0, warp);
                    i--;
                }
            }
        }
        return new MatchList(exactMatches, matches);
    }

    public String getMatchingCreator(Player player, String creator) {
        ArrayList<String> matches = new ArrayList<String>();
        List<String> names = new ArrayList<String>(warpList.keySet());
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(names, collator);

        for (int i = 0; i < names.size(); i++) {
            String currName = names.get(i);
            Warp warp = warpList.get(currName);
            if (player != null ? warp.playerCanWarp(player) : true) {
                if (warp.creator.equalsIgnoreCase(creator)) {
                    return creator;
                } else if (warp.creator.toLowerCase().contains(creator.toLowerCase())
                        && !matches.contains(warp.creator)) {
                    matches.add(warp.creator);
                }
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return "";
    }

    public void give(String name, Player givee) {
        Warp warp = warpList.get(name);
        warp.setCreator(givee.getName());
        WarpDataSource.updateCreator(warp);
    }

    public double getMaxWarps(Player player) {
        return getMaxWarpsPerCreator(player, null);
    }

    public double getMaxWarpsPerCreator(Player player, String creator) {
        int count = 0;
        for (Warp warp : warpList.values()) {
            if ((player != null ? warp.playerCanWarp(player) : true)
                    && (creator != null ? warp.playerIsCreator(creator) : true)) {
                count++;
            }
        }
        return count;
    }

    public void updateLocation(String name, Player player) {
        Warp warp = warpList.get(name);
        warp.setLocation(player.getLocation());
        WarpDataSource.updateLocation(warp);
    }

    public void welcomeMessage(String name, Player player) {
        Warp warp = warpList.get(name);
        welcomeMessage.put(player.getName(), warp);
    }

    public boolean waitingForWelcome(Player player) {
        return welcomeMessage.containsKey(player.getName());
    }

    public void setWelcomeMessage(Player player, String message) {
        if (welcomeMessage.containsKey(player.getName())) {
            Warp warp = welcomeMessage.get(player.getName());
            warp.welcomeMessage = message;
            WarpDataSource.updateWelcomeMessage(warp);
            player.sendMessage(LanguageManager.getString("warp.welcome.received"));
            player.sendMessage(message);
        }

    }

    public void notWaiting(Player player) {
        welcomeMessage.remove(player.getName());
    }

    public void list(CommandSender executor, Player player) {
        ArrayList<Warp> results = warpsInvitedTo(player);

        if (results.size() == 0) {
            executor.sendMessage(LanguageManager.getString("alist.noWarps"));
        } else {
            executor.sendMessage(LanguageManager.getString("alist.list"));
            executor.sendMessage(results.toString().replace("[", "").replace("]", ""));
        }
    }

    private ArrayList<Warp> warpsInvitedTo(Player player) {
        ArrayList<Warp> results = new ArrayList<Warp>();

        List<String> names = new ArrayList<String>(warpList.keySet());
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        Collections.sort(names, collator);

        for (String name : names) {
            Warp warp = warpList.get(name);
            if (player != null ? warp.playerCanWarp(player) : true) {
                results.add(warp);
            }
        }
        return results;
    }

    public void point(String name, Player player) {
        Warp warp = warpList.get(name);
        player.setCompassTarget(warp.getLocation(server));
    }
}
