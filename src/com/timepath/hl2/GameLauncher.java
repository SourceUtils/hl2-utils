package com.timepath.hl2;

import com.timepath.DataUtils;
import com.timepath.plaf.OS;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.BVDF;
import com.timepath.steam.io.BVDF.DataNode;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author timepath
 */
public class GameLauncher {

    public static void main(String[] args) throws IOException {
        File base = new File(SteamUtils.getSteamApps(), "common/Team Fortress 2");
        Map<OS, String> m = new EnumMap<OS, String>(OS.class);
        m.put(OS.Windows, "hl2.exe");
//        m.put(OS.OSX, "hl2_osx");
//        m.put(OS.Linux, "hl2_linux");
        m.put(OS.OSX, "hl2.sh");
        m.put(OS.Linux, "hl2.sh");

        File binary = new File(base, m.get(OS.get()));
        String[] run = "-game tf -steam".split(" ");
        String[] userOpts = null;

        File f = new File(SteamUtils.getUserData(), "config/localconfig.vdf");
        VDF v = new VDF();
        v.readExternal(new FileInputStream(f));
        VDFNode apps = v.getRoot().get("UserLocalConfigStore").get("Software").get(
                "Valve").get(
                "Steam").get("apps");
        int game = 440;
        VDFNode launch = apps.get("" + game).get("LaunchOptions");

        if(launch == null) {
            System.out.println("No launch options");
        } else {
            userOpts = launch.getValue().split(" ");
            System.out.println(Arrays.toString(userOpts));
        }

        //<editor-fold defaultstate="collapsed" desc="If valve change something">
        if(!binary.exists()) {
            System.out.println("Hardcoded binary not found");
            BVDF bin = new BVDF();
            bin.readExternal(DataUtils.mapFile(new File(SteamUtils.getSteam(),
                                                        "appcache/appinfo.vdf")));
            DataNode root = bin.getRoot();
            //        for(int i = 0; i < root.getChildCount(); i++) {
            //            System.out.println(root.getChildAt(i));
            //        }
            DataNode g = root.get("" + game);
            DataNode sections = g.get("Sections");
            DataNode conf = sections.get("CONFIG");
            DataNode g2 = conf.get("" + game);
            int type = Integer.parseInt(g2.get("contenttype").value.toString());
            String installdir = g2.get("installdir").value.toString();
            File dir = new File(SteamUtils.getSteamApps(), "common/" + installdir);

            DataNode l = g2.get("launch");
            for(int i = 0; i < l.getChildCount(); i++) {
                DataNode c = (DataNode) l.getChildAt(i);
                String executable = (String) c.get("executable").value;
                String arguments = (String) c.get("arguments").value;
                String oslist = (String) c.get("config").get("oslist").value;
                System.out.println(
                        "[" + oslist + "] " + new File(dir.getPath(), executable).getPath() + " " + arguments);
            }
        }
        //</editor-fold>

        ArrayList<String> params = new ArrayList<String>();
//        script -q /dev/null "cmd" // OSX
//        script -c "cmd" /dev/null // Linux

//        params.add("unbuffer");
        
        params.add("stdbuf");
        params.add("-i0");
        params.add("-o0");
        params.add("-e0");

        params.add(binary.getPath());
        params.addAll(Arrays.asList(run));
        if(userOpts != null) {
            params.addAll(Arrays.asList(userOpts));
        }
        String[] cmd = params.toArray(new String[0]);
        System.out.println("Starting " + Arrays.toString(cmd));

        Process proc = Runtime.getRuntime().exec(cmd, null, base);
        ExternalConsole ec = new ExternalConsole();
        ec.setVisible(true);
        ec.setIn(new BufferedInputStream(proc.getInputStream()));
        ec.setErr(new BufferedInputStream(proc.getErrorStream()));
        ec.setOut(new BufferedOutputStream(proc.getOutputStream()));
    }

}
