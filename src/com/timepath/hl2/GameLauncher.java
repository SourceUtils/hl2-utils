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
 * @author TimePath
 */
public class GameLauncher {

    private static Options DEFAULT = new Options() {
        {
            File base = new File(SteamUtils.getSteamApps(), "common/Team Fortress 2");
            Map<OS, String> m = new EnumMap<OS, String>(OS.class);
            m.put(OS.Windows, "hl2.exe");
            m.put(OS.OSX, "hl2_osx");
            m.put(OS.Linux, "hl2.sh");
            script = new File(base, m.get(OS.get()));
            args = new String[] {script.getPath(), "-game", "tf", "-steam"};
        }
    };

    private static final Logger LOG = Logger.getLogger(GameLauncher.class.getName());

    public static void main(String[] args) throws IOException {
        LOG.info(Arrays.toString(args));
        String[] command;
        if(args.length == 0) { // interactive
            command = choose();
            if(command == null) {
                return;
            }
        } else {
            command = args;
        }
        String dir = ""; // TODO

        // Args are tokenized correctly at this point, set up env vars
        Map<String, String> sysenv = new HashMap<>(System.getenv().size());
        sysenv.putAll(System.getenv());
        if(!sysenv.containsKey("TERM")) {
            sysenv.put("TERM", "xterm");
        }
        String[] env = new String[sysenv.size()];
        List<String> vars = new LinkedList<>();
        for(Map.Entry<String, String> entry : sysenv.entrySet()) {
            String v = String.format("%s=%s", entry.getKey(), entry.getValue());
            vars.add(v);
        }
        env = vars.toArray(env);

        // run
        start(command, env, dir, 12345);
    }

    /**
     * Split command, replace %command% with args
     * <p>
     * @param command Command string
     * @param args    %command% replacement
     * <p>
     * @return
     */
    public static String[] tokenize(String command, String[] args) {
        LOG.log(Level.INFO, "Tokenize: {0}, {1}", new Object[] {command, Arrays.toString(args)});
        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for(int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }

        String[] newcmd = new String[cmdarray.length + args.length - 1];
        int j = -1;
        for(int i = 0; i < cmdarray.length; i++) {
            if(cmdarray[i].equals("%command%")) {
                for(String sarg : args) {
                    newcmd[i + ++j] = sarg;
                }
            } else {
                newcmd[i + j] = cmdarray[i];
            }
        }
        return newcmd;
    }

    private static Options autoDetect(int appID) throws IOException {
        String[] gameArgs = null;
        BVDF bin = new BVDF();
        bin.readExternal(DataUtils.mapFile(new File(SteamUtils.getSteam(), "appcache/appinfo.vdf")));
        DataNode root = bin.getRoot();
        DataNode gm = root.get("" + appID);
        DataNode sections = gm.get("Sections");
        DataNode conf = sections.get("CONFIG");
        DataNode g2 = conf.get("" + appID);
        String installdir = g2.get("installdir").value.toString();
        File dir = new File(SteamUtils.getSteamApps(), "common/" + installdir);

        DataNode l = g2.get("launch");
        Map<String, File> launch = new HashMap<String, File>(l.getChildCount());
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
            default: return null;
        }
        return new Options(launch.get(get), gameArgs);
    }

    /**
     * Prompt user for execution command
     * <p>
     * @return tokenized args
     * <p>
     * @throws IOException
     */
    private static String[] choose() throws IOException {
        final int game = 440;

        String userArgs = getUserOpts(game);

        JFrame frame = new JFrame("Game Launcher");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        JPanel p = new JPanel();
        final JTextField executableField = new JTextField();

        executableField.setText(userArgs);
        executableField.setMinimumSize(new Dimension(300, executableField.getMinimumSize().height));
        executableField.setPreferredSize(executableField.getMinimumSize());
        p.add(executableField);

        String[] opts = new String[] {"Launch", "Auto", "Cancel"};
        int ret = JOptionPane.showOptionDialog(frame, p, "Game Launcher",
                                               JOptionPane.DEFAULT_OPTION,
                                               JOptionPane.PLAIN_MESSAGE, null,
                                               opts, opts[0]);

        frame.dispose();

        if(ret == 2) { // Cancel button
            return null;
        }
        if(ret < 0) { // Other cancel
            return null;
        }
        String line = null;
        if(ret == 1) { // Auto
            return autoDetect(game).args;
        } else if(ret == 0) { // Launch
            line = executableField.getText();
        }

        if(line == null || line.length() == 0) {
            return null;
        }
        return tokenize(line, DEFAULT.args);
    }

    /**
     *
     * @param appID Steam application ID
     * <p>
     * @return User launch options prepended with %command% if not present
     */
    private static String getUserOpts(int appID) {
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
            String str = launch.getValue();
            if(!str.contains("%command%")) {
                str = "%command% " + str;
            }
            return str;
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Starts the process
     * <p>
     * @param cmd  Command to exec
     * @param env  Env vars
     * @param dir  Working directory to run game from
     * @param port Port to listen on
     * <p>
     * @throws IOException
     */
    private static void start(String[] cmd, String[] env, String dir, int port) throws IOException {
        LOG.log(Level.INFO, "Starting {0}", new Object[] {Arrays.toString(cmd)});
        LOG.log(Level.INFO, "Env: {0}", Arrays.toString(env));
        LOG.log(Level.INFO, "Dir: {0}", dir);

//        final Process proc = Runtime.getRuntime().exec(cmd, env, dir); // old
        final Process proc = PtyProcess.exec(cmd, env);//, dir); // TODO

        final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null));
        int truePort = sock.getLocalPort();

        LOG.log(Level.INFO, "Listening on port {0}", truePort);

        final List<String> queue = new LinkedList<>();

        final AggregateOutputStream aggregate = new AggregateOutputStream() {

            /**
             * Ignore input in output since the PTY solution also prints that to the output...
             * TODO: Stop it from doing that, print to output manually to avoid performance hit
             * <p>
             * @throws IOException
             */
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                synchronized(queue) {
                    String[] test = new String(Arrays.copyOfRange(b, off, off + len)).split("\n");
                    boolean intern;
                    for(String t : test) {
                        if(queue.contains(t)) {
                            queue.remove(t);
                            intern = true;
                        } else {
                            intern = false;
                        }
                        byte[] bytes = ((intern ? "\1" : "") + t + "\n").getBytes("UTF-8");
                        super.write(bytes, 0, bytes.length);
                    }
                }
                super.flush();
            }

        };
        final Thread main = new Thread(new Proxy(proc.getInputStream(), aggregate, "server <--> game") {

            @Override
            protected boolean print(String line) {
//                System.err.println(line); // Steam listens to stderr
                return super.print(line);
            }
        }, "Subprocess");
        main.start();
        final Thread acceptor = new Thread("Acceptor") {
            @Override
            public void run() {
                for(;;) {
                    try {
                        final Socket client = sock.accept();
                        aggregate.register(client.getOutputStream());
                        Proxy pipe = new Proxy(client.getInputStream(), proc.getOutputStream(), "client <--> game") {

                            @Override
                            protected boolean print(String line) {
                                synchronized(queue) {
                                    queue.add(line);
                                }
                                return super.print(line);
                            }

                        };
                        Thread t = new Thread(pipe);
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

    private static class Options {

        String[] args;

        File script;

        Options() {
        }

        Options(File script, String[] args) {
            this.script = script;
            this.args = args;
        }

    }

    private static class Proxy implements Runnable {

        private final String name;

        private final PrintWriter pw;

        private final Scanner scan;

        /**
         * Pipes in to out
         * <p>
         * @param in
         * @param out
         * @param name
         */
        Proxy(InputStream in, OutputStream out, String name) {
            this.scan = new Scanner(in);
            this.pw = new PrintWriter(out, true);
            this.name = name;
        }

        @SuppressWarnings("empty-statement")
        public void run() {
            while(scan.hasNextLine() && print(scan.nextLine()));
            LOG.log(Level.INFO, "Stopped proxying {0}", name);
        }

        /**
         *
         * @param line The line to print
         * <p>
         * @return false if error
         */
        protected boolean print(String line) {
            pw.println(line);
            return !pw.checkError();
        }

    }

}
