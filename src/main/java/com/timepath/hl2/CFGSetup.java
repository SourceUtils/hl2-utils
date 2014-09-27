package com.timepath.hl2;

import com.timepath.steam.SteamUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * A simple program for TF2 to create 9 class configs and a reset.cfg.
 *
 * @author TimePath
 */
public class CFGSetup {

    private CFGSetup() {
    }

    public static void main(String[] args) throws IOException {
        @NotNull File dir = new File(SteamUtils.getSteamApps() +
                "/common/Team Fortress 2/tf/custom/" +
                SteamUtils.getUser().getUser().toLowerCase().replaceAll("[^a-z0-9]", "") +
                "/cfg");
        dir.mkdirs();
        @NotNull String[] classes = {
                "scout", "soldier", "pyro", "demoman", "heavyweapons", "engineer", "medic", "sniper", "spy"
        };
        for (String clazz : classes) {
            @NotNull File f = new File(dir, clazz + ".cfg");
            if (f.exists()) continue;
            @NotNull PrintWriter out = createWriter(f);
            out.println("// This file is executed when you play " + clazz);
            out.println("exec reset // Set " + clazz + " specific settings after this line");
            out.println();
        }
        @NotNull File autoexec = new File(dir, "autoexec.cfg");
        if (!autoexec.exists()) {
            @NotNull PrintWriter out = createWriter(autoexec);
            out.println("// This file is executed on startup, exec other files from here");
            out.println("// Example: exec dx9frames");
            out.println();
        }
        @NotNull File reset = new File(dir, "reset.cfg");
        if (!reset.exists()) {
            @NotNull PrintWriter out = createWriter(reset);
            out.println("// This file is executed on class change, put all your 'normal' settings here");
            out.println("// You will no longer be able to change these settings in-game");
            out.println("\nexec binds\n");
            out.println();
        }
        @NotNull File binds = new File(dir, "binds.cfg");
        // Always overwrite binds
        {
            @NotNull PrintWriter out = createWriter(binds);
            out.println("// This file was generated from your config.cfg in /tf/cfg/config.cfg");
            out.println();
            @NotNull File config = new File(dir.getParentFile().getParentFile().getParentFile(), "cfg/config.cfg");
            @NotNull BufferedReader br = createReader(config);
            for (String line; (line = br.readLine()) != null; ) {
                if (!line.contains("bind")) break;
                out.println(line);
            }
            out.println();
        }
        JOptionPane.showMessageDialog(null, "CFG files created");
        Desktop.getDesktop().open(dir);
    }

    @NotNull
    private static BufferedReader createReader(@NotNull File f) throws IOException {
        return new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(f))));
    }

    @NotNull
    private static PrintWriter createWriter(@NotNull File f) throws IOException {
        return new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)), true);
    }
}
