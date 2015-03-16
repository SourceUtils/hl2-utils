package com.timepath.hl2

import com.timepath.hl2.gameinfo.Player

import java.io.IOException
import java.util.LinkedList
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
SuppressWarnings("serial")
public class ExternalScoreboard private() : ExternalConsole() {
    private val players = LinkedList<Player>()

            ;{
        setTitle("External killfeed")
    }

    override fun parse(lines: String) {
        output.setText("")
        val strings = lines.split("\n")
        for (s in strings) {
            if (s.contains(" killed ")) {
                notify(s)
            }
            //            if(s.endsWith(" suicided.")) {
            //                // possible team/class switch
            //            }
            //            if(s.endsWith(" connected")) {
            //            }
            //            if(s.startsWith("Dropped") && s.contains("from server")) {
            //            }
            //            // names defended/captured 'capname' for team#
            //            if(s.contains(" for team #")) {
            //                // team 0 = spectator, team 2 = red, team 3 = blu
            //            }
            //            if(s.equals("Teams have been switched.")) {
            //            }
        }
        output.append("\nPlayers:\n")
        for (player in players) {
            output.append("$player\n")
        }
        val me = getPlayer("TimePath")
        output.append("\nAllies:\n")
        for (i in me.getAllies().size().indices) {
            output.append("${me.getAllies()[i]}\n")
        }
        output.append("\nEnemies:\n")
        for (i in me.getEnemies().size().indices) {
            output.append("${me.getEnemies()[i]}\n")
        }
        output.append("\n")
    }

    private fun notify(s: String) {
        val killer = getPlayer(s.split(" killed ")[0])
        val victim = getPlayer(s.split(" killed ")[1].split(" with ")[0])
        var weapon = s.split(" killed ")[1].split(" with ")[1]
        Player.exchangeInfo(victim, killer)
        val crit = weapon.endsWith("(crit)")
        weapon = weapon.substring(0, weapon.indexOf('.'))
        if (crit) {
            weapon = '*' + weapon + '*'
        }
        val sb = StringBuilder()
        sb.append(killer.name).append(" = ").append(weapon).append(" -> ").append(victim.name)
        output.append(sb.toString() + "\n")
    }

    private fun getPlayer(name: String): Player {
        for (p in players) {
            if (p.name == name) {
                return p
            }
        }
        val p = Player(name)
        players.add(p)
        return p
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<ExternalScoreboard>().getName())

        throws(javaClass<IOException>())
        platformStatic fun main(args: Array<String>) {
            val es = ExternalScoreboard()
            es.connect(12345)
            es.setVisible(true)
        }
    }
}
