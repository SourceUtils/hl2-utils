package com.timepath.hl2;

import com.pty4j.PtyProcess;
import com.timepath.DataUtils;
import com.timepath.io.AggregateOutputStream;
import com.timepath.plaf.OS;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.bvdf.BVDF;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.util.VDFNode1;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.timepath.plaf.OS.*;

/**
 * Starts a game and relay server.
 * TODO: Use Steam runtime on linux
 *
 * @author TimePath
 */
class GameLauncher {

    private static final Options DEFAULT = new Options() {
        {
            File base = new File(SteamUtils.getSteamApps(), "common/Team Fortress 2");
            Map<OS, String> m = new EnumMap<>(OS.class);
            m.put(Windows, "hl2.exe");
            m.put(OSX, "hl2_osx");
            m.put(Linux, "hl2.sh");
            script = new File(base, m.get(get()));
            args = new String[] { script.getPath(), "-game", "tf", "-steam" };
        }
    };
    private static final Logger  LOG     = Logger.getLogger(GameLauncher.class.getName());

    private GameLauncher() {}

    public static void main(String... args) throws IOException {
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
        String dir = ""; // TODO
        start(command, env, dir, 12345);
    }

    /**
     * Prompt user for execution command
     *
     * @return tokenized args
     *
     * @throws IOException
     */
    private static String[] choose() throws IOException {
        int game = 440;
        String userArgs = getUserOpts(game);
        JFrame frame = new JFrame("Game Launcher");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        JPanel p = new JPanel();
        JTextField executableField = new JTextField();
        executableField.setText(userArgs);
        executableField.setMinimumSize(new Dimension(300, executableField.getMinimumSize().height));
        executableField.setPreferredSize(executableField.getMinimumSize());
        p.add(executableField);
        String[] opts = { "Launch", "Auto", "Cancel" };
        int ret = JOptionPane.showOptionDialog(frame,
                                               p,
                                               "Game Launcher",
                                               JOptionPane.DEFAULT_OPTION,
                                               JOptionPane.PLAIN_MESSAGE,
                                               null,
                                               opts,
                                               opts[0]);
        frame.dispose();
        if(ret == 2) { // Cancel button
            return null;
        }
        if(ret < 0) { // Other cancel
            return null;
        }
        if(ret == 1) { // Auto
            return autoDetect(game).args;
        }
        String line = null;
        if(ret == 0) { // Launch
            line = executableField.getText();
        }
        if(( line == null ) || line.isEmpty()) {
            return null;
        }
        return tokenize(line, DEFAULT.args);
    }

    /**
     * Split command, replace %command% with args
     *
     * @param command
     *         Command string
     * @param args
     *         %command% replacement
     *
     * @return
     */
    private static String[] tokenize(String command, String... args) {
        LOG.log(Level.INFO, "Tokenize: {0}, {1}", new Object[] { command, Arrays.toString(args) });
        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for(int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
        String[] newcmd = new String[( cmdarray.length + args.length ) - 1];
        int j = -1;
        for(int i = 0; i < cmdarray.length; i++) {
            if("%command%".equals(cmdarray[i])) {
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
        BVDF bin = new BVDF();
        bin.readExternal(DataUtils.mapFile(new File(SteamUtils.getSteam(), "appcache/appinfo.vdf")));
        BVDF.DataNode root = bin.getRoot();
        BVDF.DataNode gm = root.get(String.valueOf(appID));
        BVDF.DataNode sections = gm.get("Sections");
        BVDF.DataNode conf = sections.get("CONFIG");
        BVDF.DataNode g2 = conf.get(String.valueOf(appID));
        String installdir = g2.get("installdir").value.toString();
        File dir = new File(SteamUtils.getSteamApps(), "common/" + installdir);
        BVDF.DataNode l = g2.get("launch");
        Map<String, File> launch = new HashMap<>(l.getChildCount());
        String[] gameArgs = null;
        for(int i = 0; i < l.getChildCount(); i++) {
            BVDF.DataNode c = (BVDF.DataNode) l.getChildAt(i);
            gameArgs = ( (String) c.get("arguments").value ).split(" ");
            String os = (String) c.get("config").get("oslist").value; // hopefully just one OS will be present
            String exe = (String) c.get("executable").value;
            launch.put(os, new File(dir.getPath(), exe));
        }
        String get = null;
        switch(get()) {
            case Windows:
                get = "windows";
                break;
            case OSX:
                get = "macos";
                break;
            case Linux:
                get = "linux";
                break;
            default:
                return null;
        }
        return new Options(launch.get(get), gameArgs);
    }

    /**
     * @param appID
     *         Steam application ID
     *
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
                             .get(String.valueOf(appID));
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
     *
     * @param cmd
     *         Command to exec
     * @param env
     *         Env vars
     * @param dir
     *         Working directory to run game from
     * @param port
     *         Port to listen on
     *
     * @throws IOException
     */
    private static void start(String[] cmd, String[] env, String dir, int port) throws IOException {
        LOG.log(Level.INFO, "Starting {0}", new Object[] { Arrays.toString(cmd) });
        LOG.log(Level.INFO, "Env: {0}", Arrays.toString(env));
        LOG.log(Level.INFO, "Dir: {0}", dir);
        //        final Process proc = Runtime.getRuntime().exec(cmd, env, dir); // old
        final Process proc = PtyProcess.exec(cmd, env);//, dir); // TODO
        final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null));
        int truePort = sock.getLocalPort();
        LOG.log(Level.INFO, "Listening on port {0}", truePort);
        final Collection<String> queue = new LinkedList<>();
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
                    for(String t : test) {
                        boolean intern;
                        if(queue.contains(t)) {
                            queue.remove(t);
                            intern = true;
                        } else {
                            intern = false;
                        }
                        byte[] bytes = ( ( intern ? "\1" : "" ) + t + '\n' ).getBytes("UTF-8");
                        super.write(bytes, 0, bytes.length);
                    }
                }
                flush();
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
        Thread acceptor = new Thread("Acceptor") {
            @Override
            public void run() {
                while(true) {
                    try {
                        Socket client = sock.accept();
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
                    } catch(SocketTimeoutException ignored) {
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
        new Thread(new Runnable() {
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
        }, "Reaper").start();
    }

    private static class Options {

        String[] args;
        File     script;

        Options() {
        }

        Options(File script, String... args) {
            this.script = script;
            this.args = args;
        }
    }

    private static class Proxy implements Runnable {

        private final String      name;
        private final PrintWriter pw;
        private final Scanner     scan;

        /**
         * Pipes in to out
         *
         * @param in
         * @param out
         * @param name
         */
        Proxy(InputStream in, OutputStream out, String name) {
            scan = new Scanner(in);
            pw = new PrintWriter(out, true);
            this.name = name;
        }

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            while(scan.hasNextLine() && print(scan.nextLine())) ;
            LOG.log(Level.INFO, "Stopped proxying {0}", name);
        }

        /**
         * @param line
         *         The line to print
         *
         * @return false if error
         */
        boolean print(String line) {
            pw.println(line);
            return !pw.checkError();
        }
    }
}
