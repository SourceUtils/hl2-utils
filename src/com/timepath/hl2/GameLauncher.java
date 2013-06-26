package com.timepath.hl2;

import com.timepath.DataUtils;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.BVDF;
import com.timepath.steam.io.BVDF.DataNode;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import java.io.File;
import java.io.FileInputStream;

/**
 *
 * @author timepath
 */
public class GameLauncher {

    public static void main(String[] args) throws Exception {
        File f = new File(SteamUtils.getUserData(), "config/localconfig.vdf");
        VDF v = new VDF();
        v.readExternal(new FileInputStream(f));
        VDFNode apps = v.getRoot().get("UserLocalConfigStore").get("Software").get("Valve").get(
                "Steam").get("apps");
        int game = 440;
        VDFNode launch = apps.get("" + game).get("LaunchOptions");
        if(launch == null) {
            System.out.println("No launch options");
        } else {
            System.out.println(launch.getValue());
        }
        BVDF bin = new BVDF();
        bin.readExternal(DataUtils.mapFile(new File(SteamUtils.getSteam(), "appcache/appinfo.vdf")));
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

}
