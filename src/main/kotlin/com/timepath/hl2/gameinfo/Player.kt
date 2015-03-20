package com.timepath.hl2.gameinfo

import java.util.LinkedList
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class Player(public var name: String?) {
    val allies = LinkedList<Player>()
    val enemies = LinkedList<Player>()

    fun addAllies(list: Iterable<Player>) {
        for (ally in list) {
            addAllyR(ally)
            for (enemy in enemies) {
                enemy.addEnemyR(ally)
            }
        }
    }

    fun addAlly(other: Player) {
        if (other == this) {
            return
        }
        if (other !in allies) {
            allies.add(other)
        }
        if (!other.hasAlly(this)) {
            other.allies.add(this)
        }
    }

    fun addAllyR(other: Player) {
        addAlly(other)
        for (ally in allies) {
            ally.addAlly(other)
        }
    }

    fun addEnemies(enemies: Iterable<Player>) {
        for (list1 in enemies) {
            addEnemyR(list1)
            for (ally in allies) {
                ally.addEnemyR(list1)
            }
        }
    }

    fun addEnemy(other: Player) {
        if (other == this) {
            return
        }
        if (other !in enemies) {
            enemies.add(other)
        }
        if (!other.hasEnemy(this)) {
            other.enemies.add(this)
        }
    }

    fun addEnemyR(other: Player) {
        addEnemy(other)
        for (ally in allies) {
            ally.addEnemy(other)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(name)
        sb.append(" (e:{")
        val myEnemies = enemies
        for (i in enemies.indices) {
            sb.append(myEnemies[i].name)
            if ((i + 1) < enemies.size()) {
                sb.append(", ")
            }
        }
        sb.append("}, a:{")
        val myAllies = allies
        for (i in allies.indices) {
            sb.append(myAllies[i].name)
            if ((i + 1) < allies.size()) {
                sb.append(", ")
            }
        }
        sb.append("})")
        return sb.toString()
    }

    private fun hasAlly(player: Player) = player in allies

    private fun hasEnemy(player: Player) = player in enemies

    companion object {

        private val LOG = Logger.getLogger(javaClass<Player>().getName())

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
        public fun exchangeInfo(v: Player, k: Player) {
            val vAllies = LinkedList(v.allies)
            val vEnemies = LinkedList(v.enemies)
            val kAllies = LinkedList(k.allies)
            val kEnemies = LinkedList(k.enemies)
            if (v in k.allies || k in v.allies) {
                // Traitor
                for (kAlly in k.allies) {
                    kAlly.allies.remove(k)
                    kAlly.enemies.remove(k)
                    kAlly.enemies.add(k)
                }
                for (vAlly in v.allies) {
                    vAlly.allies.remove(k)
                    vAlly.enemies.remove(k)
                    vAlly.enemies.add(k)
                }
                for (kEnemy in k.enemies) {
                    kEnemy.enemies.remove(k)
                    kEnemy.allies.remove(k)
                    kEnemy.allies.add(k)
                }
                for (vEnemy in v.enemies) {
                    vEnemy.enemies.remove(k)
                    vEnemy.allies.remove(k)
                    vEnemy.allies.add(k)
                }
                k.allies.clear()
                k.enemies.clear()
                k.allies.addAll(kEnemies)
                //            k.allies.addAll(vEnemies);
                k.enemies.addAll(kAllies)
                //            k.enemies.addAll(vAllies);
            } else {
                v.addEnemy(k)
                k.addEnemy(v)
                v.addAllies(kEnemies)
                v.addEnemies(kAllies)
                k.addAllies(vEnemies)
                k.addEnemies(vAllies)
            }
        }
    }
}
