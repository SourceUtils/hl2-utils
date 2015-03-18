package com.timepath.hl2

import com.timepath.steam.SteamUtils

import javax.swing.*
import java.awt.*
import java.io.*
import kotlin.platform.platformStatic

/**
 * A simple program for TF2 to create 9 class configs and a reset.cfg.
 *
 * @author TimePath
 */
object CFGSetup {

    throws(javaClass<IOException>())
    public platformStatic fun main(args: Array<String>) {
        val dir = File("${SteamUtils.getSteamApps()}/common/Team Fortress 2/tf/custom/${SteamUtils.getUser()!!.user!!.toLowerCase().replaceAll("[^a-z0-9]", "")}/cfg")
        dir.mkdirs()
        val classes = array("scout", "soldier", "pyro", "demoman", "heavyweapons", "engineer", "medic", "sniper", "spy")
        for (clazz in classes) {
            val f = File(dir, "$clazz.cfg")
            if (f.exists()) continue
            val out = createWriter(f)
            out.println("// This file is executed when you play $clazz")
            out.println("exec reset // Set $clazz specific settings after this line")
            out.println()
        }
        val autoexec = File(dir, "autoexec.cfg")
        if (!autoexec.exists()) {
            val out = createWriter(autoexec)
            out.println("// This file is executed on startup, exec other files from here")
            out.println("// Example: exec dx9frames")
            out.println()
        }
        val reset = File(dir, "reset.cfg")
        if (!reset.exists()) {
            val out = createWriter(reset)
            out.println("// This file is executed on class change, put all your 'normal' settings here")
            out.println("// You will no longer be able to change these settings in-game")
            out.println("\nexec binds\n")
            out.println()
        }
        val binds = File(dir, "binds.cfg")
        // Always overwrite binds
        run {
            val out = createWriter(binds)
            out.println("// This file was generated from your config.cfg in /tf/cfg/config.cfg")
            out.println()
            val config = File(dir.getParentFile().getParentFile().getParentFile(), "cfg/config.cfg")
            for (line in config.readLines()) {
                if ("bind" !in line) break
                out.println(line)
            }
            out.println()
        }
        JOptionPane.showMessageDialog(null, "CFG files created")
        Desktop.getDesktop().open(dir)
    }

    throws(javaClass<IOException>())
    private fun createReader(f: File): BufferedReader {
        return BufferedReader(InputStreamReader(BufferedInputStream(FileInputStream(f))))
    }

    throws(javaClass<IOException>())
    private fun createWriter(f: File): PrintWriter {
        return PrintWriter(BufferedOutputStream(FileOutputStream(f)), true)
    }
}
