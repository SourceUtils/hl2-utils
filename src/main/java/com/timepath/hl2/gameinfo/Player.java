package com.timepath.hl2.gameinfo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Player {

    private static final Logger       LOG     = Logger.getLogger(Player.class.getName());
    private final        List<Player> allies  = new LinkedList<>();
    private final        List<Player> enemies = new LinkedList<>();
    private String name;

    public Player(String name) {
        this.name = name;
    }

    /**
     * Makes players enemies
     * Function:
     * Adds a new enemy.
     * Makes your enemy's enemies your allies.
     * Makes your enemy's allies your enemies.
     * Informs all your allies of your new enemy.
     *
     * @param v
     *         Victim
     * @param k
     *         Killer
     */
    public static void exchangeInfo(Player v, Player k) {
        Iterable<Player> vAllies = new LinkedList<>(v.allies);
        Iterable<Player> vEnemies = new LinkedList<>(v.enemies);
        Collection<Player> kAllies = new LinkedList<>(k.allies);
        Collection<Player> kEnemies = new LinkedList<>(k.enemies);
        if(k.allies.contains(v) || v.allies.contains(k)) { // Traitor
            for(Player kAlly : k.allies) {
                kAlly.allies.remove(k);
                kAlly.enemies.remove(k);
                kAlly.enemies.add(k);
            }
            for(Player vAlly : v.allies) {
                vAlly.allies.remove(k);
                vAlly.enemies.remove(k);
                vAlly.enemies.add(k);
            }
            for(Player kEnemy : k.enemies) {
                kEnemy.enemies.remove(k);
                kEnemy.allies.remove(k);
                kEnemy.allies.add(k);
            }
            for(Player vEnemy : v.enemies) {
                vEnemy.enemies.remove(k);
                vEnemy.allies.remove(k);
                vEnemy.allies.add(k);
            }
            k.allies.clear();
            k.enemies.clear();
            k.allies.addAll(kEnemies);
            //            k.allies.addAll(vEnemies);
            k.enemies.addAll(kAllies);
            //            k.enemies.addAll(vAllies);
        } else {
            v.addEnemy(k);
            k.addEnemy(v);
            v.addAllies(kEnemies);
            v.addEnemies(kAllies);
            k.addAllies(vEnemies);
            k.addEnemies(vAllies);
        }
    }

    void addAllies(Iterable<Player> list) {
        for(Player ally : list) {
            addAllyR(ally);
            for(Player enemy : enemies) {
                enemy.addEnemyR(ally);
            }
        }
    }

    void addAlly(Player other) {
        if(other == this) {
            return;
        }
        if(!allies.contains(other)) {
            allies.add(other);
        }
        if(!other.hasAlly(this)) {
            other.allies.add(this);
        }
    }

    void addAllyR(Player other) {
        addAlly(other);
        for(Player ally : allies) {
            ally.addAlly(other);
        }
    }

    void addEnemies(Iterable<Player> enemies) {
        for(Player list1 : enemies) {
            addEnemyR(list1);
            for(Player ally : allies) {
                ally.addEnemyR(list1);
            }
        }
    }

    void addEnemy(Player other) {
        if(other == this) {
            return;
        }
        if(!enemies.contains(other)) {
            enemies.add(other);
        }
        if(!other.hasEnemy(this)) {
            other.enemies.add(this);
        }
    }

    void addEnemyR(Player other) {
        addEnemy(other);
        for(Player ally : allies) {
            ally.addEnemy(other);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" (e:{");
        List<Player> myEnemies = enemies;
        for(int i = 0; i < enemies.size(); i++) {
            sb.append(myEnemies.get(i).name);
            if(( i + 1 ) < enemies.size()) {
                sb.append(", ");
            }
        }
        sb.append("}, a:{");
        List<Player> myAllies = allies;
        for(int i = 0; i < allies.size(); i++) {
            sb.append(myAllies.get(i).name);
            if(( i + 1 ) < allies.size()) {
                sb.append(", ");
            }
        }
        sb.append("})");
        return sb.toString();
    }

    /**
     * @return the allies
     */
    public List<Player> getAllies() {
        return allies;
    }

    /**
     * @return the enemies
     */
    public List<Player> getEnemies() {
        return enemies;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *         the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    private boolean hasAlly(Player player) {
        return allies.contains(player);
    }

    private boolean hasEnemy(Player player) {
        return enemies.contains(player);
    }
}
