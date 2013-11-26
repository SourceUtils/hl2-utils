package com.timepath.hl2;

import com.pty4j.PtyProcess;
import com.timepath.DataUtils;
import com.timepath.io.AggregateOutputStream;
import com.timepath.plaf.OS;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.BVDF;
import com.timepath.steam.io.BVDF.DataNode;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.util.VDFNode1;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import static com.timepath.plaf.OS.Linux;
import static com.timepath.plaf.OS.OSX;
import static com.timepath.plaf.OS.Windows;

/**
 *
 * Starts a game and relay server.
 * TODO: Use Steam runtime on linux
 * <p/>
 * echo toggleconsole > /dev/tcp/localhost/12345
 * <p/>
 * N=bottles of beer
 * for I in $(seq 99 -1 1); do echo "echo $I $N on the wall, $I $N. Take 1 down, pass it around,
 * `expr $I - 1` $N on the wall" > /dev/tcp/localhost/12345; sleep 1; done
 * <p/>
 * for I in $(seq 40 -1 0); do echo "say_team Uber in $I seconds" > /dev/tcp/localhost/12345; sleep
 * 1; done
 * <p/>
 * @author TimePath
 */
public class GameLauncher {

    private static final Logger LOG = Logger.getLogger(GameLauncher.class.getName());

    public static void main(String[] args) throws IOException {
        final int game = 440;

        File executable = null;
        String[] gameArgs = null;
        String[] userArgs = getUserOpts(game);
        boolean noPrefs = true;
        if(noPrefs) {
            //<editor-fold defaultstate="collapsed" desc="Define some defaults">
            gameArgs = new String[] {"-game", "tf", "-steam"};
            File base = new File(SteamUtils.getSteamApps(), "common/Team Fortress 2");
            Map<OS, String> m = new EnumMap<OS, String>(OS.class);
            m.put(OS.Windows, "hl2.exe");
            m.put(OS.OSX, "hl2_osx");
            m.put(OS.Linux, "hl2.sh");
            executable = new File(base, m.get(OS.get()));
            //</editor-fold>
        }

        JFrame frame = new JFrame("Game Launcher");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        JPanel p = new JPanel();
        final JTextField executableField = new JTextField();
        if(executable != null) {
            executableField.setText(executable.getPath());
        }
        executableField.setMinimumSize(new Dimension(300, executableField.getMinimumSize().height));
        executableField.setPreferredSize(executableField.getMinimumSize());
        p.add(executableField, BorderLayout.NORTH);
        final JTextField argsField = new JTextField();
        //<editor-fold defaultstate="collapsed" desc="Set the args field">
        String g = "";
        if(gameArgs != null) {
            for(String gameArg : gameArgs) {
                g += gameArg + " ";
            }
        }
        if(userArgs != null) {
            for(String userArg : userArgs) {
                g += userArg + " ";
            }
        }
        g = g.trim();
        argsField.setText(g);
        //</editor-fold>
        argsField.setMinimumSize(new Dimension(300, argsField.getMinimumSize().height));
        argsField.setPreferredSize(argsField.getMinimumSize());
        p.add(argsField, BorderLayout.SOUTH);

        String[] opts = new String[] {"Launch", "Auto", "Cancel"};
        int ret = JOptionPane.showOptionDialog(frame, p, "Game Launcher",
                                               JOptionPane.DEFAULT_OPTION,
                                               JOptionPane.PLAIN_MESSAGE, null,
                                               opts, opts[0]);

        frame.dispose();

        if(ret < 0) {
            return;
        }

        if(ret == 2) {
            return;
        } else if(ret == 1) {
            BVDF bin = new BVDF();
            bin.readExternal(DataUtils.mapFile(
                new File(SteamUtils.getSteam(), "appcache/appinfo.vdf")));
            DataNode root = bin.getRoot();
            DataNode gm = root.get("" + game);
            DataNode sections = gm.get("Sections");
            DataNode conf = sections.get("CONFIG");
            DataNode g2 = conf.get("" + game);
            int type = Integer.parseInt(g2.get("contenttype").value.toString());
            String installdir = g2.get("installdir").value.toString();
            File dir = new File(SteamUtils.getSteamApps(), "common/" + installdir);

            DataNode l = g2.get("launch");
            Map<String, File> launch = new HashMap<String, File>();
            for(int i = 0; i < l.getChildCount(); i++) {
                DataNode c = (DataNode) l.getChildAt(i);
                gameArgs = ((String) c.get("arguments").value).split(" ");
                String os = (String) c.get("config").get("oslist").value; // hopefully just one OS will be present
                String exe = (String) c.get("executable").value;
                launch.put(os, new File(dir.getPath(), exe));
            }
            String get = null;
            switch(OS.get()) {
                case Windows:
                    get = "windows";
                    break;
                case OSX:
                    get = "macos";
                    break;
                case Linux:
                    get = "linux";
                    break;
            }
            executable = launch.get(get);
        } else if(ret == 0) {
            executable = new File(executableField.getText());
            gameArgs = argsField.getText().split(" ");
            userArgs = null;
        }

        if(executable == null) {
            return;
        }

        ArrayList<String> params = new ArrayList<String>();
        params.add(executable.getPath());
        params.addAll(Arrays.asList(gameArgs));
        if(userArgs != null) {
            params.addAll(Arrays.asList(userArgs));
        }
        String[] cmd = params.toArray(new String[params.size()]);
        LOG.log(Level.INFO, "Starting {0}", Arrays.toString(cmd));

        HashMap<String, String> sysenv = new HashMap<String, String>();
        sysenv.putAll(System.getenv());
        if(!sysenv.containsKey("TERM")) {
            sysenv.put("TERM", "xterm");
        }
        String[] env = new String[sysenv.size()];
        ArrayList<String> vars = new ArrayList<String>();
        for(String envName : sysenv.keySet()) {
            String v = String.format("%s=%s", envName, sysenv.get(envName));
            vars.add(v);
        }
        env = vars.toArray(env);
        LOG.log(Level.INFO, "Env: {0}", Arrays.toString(env));

//        final Process proc = Runtime.getRuntime().exec(cmd, env, executable.getParentFile()); // old
        final Process proc = PtyProcess.exec(cmd, env, executable.getParent());

        int port = 12345;
        final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null));
        int truePort = sock.getLocalPort();

        LOG.log(Level.INFO, "Listening on port {0}", truePort);

        final AggregateOutputStream aggregate = new AggregateOutputStream();
        final Thread main = new Thread(new Proxy(proc.getInputStream(), aggregate, "< game"), "Subprocess");
        main.start();
        final Thread acceptor = new Thread("Acceptor") {
            @Override
            public void run() {
                for(;;) {
                    try {
                        final Socket client = sock.accept();
                        aggregate.register(client.getOutputStream());
                        Thread t = new Thread(
                            new Proxy(client.getInputStream(), proc.getOutputStream(),
                                      "client > game"));
                        t.setDaemon(true);
                        t.start();
                    } catch(SocketTimeoutException ex) {
                    } catch(IOException ex) {
                        if(sock.isClosed()) {
                            return;
                        }
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }

            }
        };
        acceptor.setDaemon(true);
        acceptor.start();
        new Thread("Reaper") {

            @Override
            public void run() {
                try {
                    main.join();
                } catch(InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                LOG.info("Reaping");
                try {
                    sock.close();
                } catch(IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    private static String[] getUserOpts(int appID) {
        try {
            File f = new File(SteamUtils.getUserData(), "config/localconfig.vdf");
            VDF1 v = new VDF1();
            v.readExternal(new FileInputStream(f));
            VDFNode1 game = v.getRoot()
                .get("UserLocalConfigStore")
                .get("Software")
                .get("Valve")
                .get("Steam")
                .get("apps")
                .get("" + appID);
            if(game == null) {
                return null;
            }
            VDFNode1 launch = game.get("LaunchOptions");
            if(launch == null) {
                return null;
            }
            return launch.getValue().split(" ");
        } catch(FileNotFoundException ex) {
            Logger.getLogger(GameLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static class Proxy implements Runnable {

        private final String name;

        private final PrintWriter pw;

        private final Scanner scan;

        Proxy(InputStream in, OutputStream out, String name) {
            this.scan = new Scanner(in);
            this.pw = new PrintWriter(out, true);
            this.name = name;
        }

        public void run() {
            while(scan.hasNextLine()) {
                String line = scan.nextLine();
                pw.println(line);
                if(pw.checkError()) {
                    break;
                }
            }
            LOG.log(Level.INFO, "Stopped proxying {0}", name);
        }

    }

}
