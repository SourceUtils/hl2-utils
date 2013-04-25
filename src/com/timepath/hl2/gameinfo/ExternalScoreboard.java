package com.timepath.hl2.gameinfo;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
@SuppressWarnings("serial")
public class ExternalScoreboard extends ExternalConsole {

    private static final Logger LOG = Logger.getLogger(ExternalScoreboard.class.getName());

    public ExternalScoreboard() {
        super();
        this.setTitle("External killfeed");
    }

    @Override
    protected void parse(String lines) {
        output.setText("");
        String[] strings = lines.split("\n");
        for(int i = 0; i < strings.length; i++) {
            String s = strings[i];
            if(s.contains(" killed ")) {
                notify(s);
            }
            if(s.endsWith(" suicided.")) {
                // possible team/class switch
            }
            if(s.endsWith(" connected")) {
            }
            if(s.startsWith("Dropped") && s.contains("from server")) {
            }
            // names defended/captured 'capname' for team#
            if(s.contains(" for team #")) {
                // team 0 = spectator, team 2 = red, team 3 = blu
            }
            if(s.equals("Teams have been switched.")) {
            }
        }

        output.append("\nPlayers:\n");
        for(int i = 0; i < players.size(); i++) {
            output.append(players.get(i).toString() + "\n");
        }

        Player me = getPlayer("TimePath");
        output.append("\nAllies:\n");
        for(int i = 0; i < me.getAllies().size(); i++) {
            output.append(me.getAllies().get(i) + "\n");
        }
        output.append("\nEnemies:\n");
        for(int i = 0; i < me.getEnemies().size(); i++) {
            output.append(me.getEnemies().get(i) + "\n");
        }
        output.append("\n");
    }

    private ArrayList<Player> players = new ArrayList<Player>();

    private Player getPlayer(String name) {
        for(int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if(p.getName().equals(name)) {
                return p;
            }
        }
        players.add(new Player(name));
        Player p = getPlayer(name);
        return p;
    }

    private void notify(String s) {
        Player killer = getPlayer(s.split(" killed ")[0]);
        Player victim = getPlayer(s.split(" killed ")[1].split(" with ")[0]);
        String weapon = s.split(" killed ")[1].split(" with ")[1];

        Player.exchangeInfo(victim, killer);

        boolean crit = weapon.endsWith("(crit)");
        weapon = weapon.substring(0, weapon.indexOf('.'));
        if(crit) {
            weapon = "*" + weapon + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(killer.getName()).append(" = ").append(weapon).append(" -> ").append(victim.getName());
        output.append(sb.toString() + "\n");
    }

    public static void main(String... args) {
        new ExternalScoreboard().setVisible(true);
    }
}
