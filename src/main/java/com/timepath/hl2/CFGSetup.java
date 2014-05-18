package com.timepath.hl2;

import com.timepath.steam.SteamUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.logging.Logger;

class CFGSetup {

    private static final Logger LOG = Logger.getLogger(CFGSetup.class.getName());

    private CFGSetup() {}

    public static void main(String... args) throws IOException {
        File dir = new File(MessageFormat.format("{0}{1}{2}/cfg",
                                                 SteamUtils.getSteamApps(),
                                                 "/common/Team Fortress 2/tf/custom/",
                                                 SteamUtils.getUser().getUser().toLowerCase().replaceAll("[^a-z0-9]", "")));
        if(!dir.exists()) {
            dir.mkdirs();
        }
        String[] classes = {
                "scout", "soldier", "pyro", "demoman", "heavyweapons", "engineer", "medic", "sniper", "spy"
        };
        for(String clazz : classes) {
            File f = new File(dir, clazz + ".cfg");
            if(f.exists()) {
                continue;
            }
            PrintWriter out = createWriter(f);
            out.println("// This file is executed when you play " + clazz);
            out.println("exec reset // Set " + clazz + " specific settings after this line");
            out.println();
        }
        File autoexec = new File(dir, "autoexec.cfg");
        if(!autoexec.exists()) {
            PrintWriter out = createWriter(autoexec);
            out.println("// This file is executed on startup, exec other files from here");
            out.println("// Example: exec dx9frames");
            out.println();
        }
        File reset = new File(dir, "reset.cfg");
        if(!reset.exists()) {
            PrintWriter out = createWriter(reset);
            out.println("// This file is executed on class change, put all your 'normal' settings here");
            out.println("// You will no longer be able to change these settings in-game");
            out.println("\nexec binds\n");
            out.println();
        }
        PrintWriter binds = createWriter(new File(dir, "binds.cfg"));
        binds.println("// This file was generated from your config.cfg in /tf/cfg/config.cfg");
        binds.println();
        File config = new File(dir.getParentFile().getParentFile().getParentFile(), "cfg/config.cfg");
        BufferedReader br = createReader(config);
        for(String line; ( line = br.readLine() ) != null; ) {
            if(!line.contains("bind")) {
                break;
            }
            binds.println(line);
        }
        binds.println();
        JOptionPane.showMessageDialog(null, "CFG files created");
        Desktop.getDesktop().open(dir);
    }

    private static BufferedReader createReader(File f) throws IOException {
        return new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(f))));
    }

    private static PrintWriter createWriter(File f) throws IOException {
        return new PrintWriter(new BufferedOutputStream(new FileOutputStream(f)), true);
    }
}
