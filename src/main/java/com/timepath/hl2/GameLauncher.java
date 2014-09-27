package com.timepath.hl2;

import com.pty4j.PtyProcess;
import com.timepath.DataUtils;
import com.timepath.io.AggregateOutputStream;
import com.timepath.plaf.OS;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.VDFNode;
import com.timepath.steam.io.bvdf.BVDF;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.timepath.plaf.OS.get;

/**
 * Starts a game and relay server.
 * TODO: Use Steam runtime on linux
 *
 * @author TimePath
 */
class GameLauncher {

    @Nullable
    private static final Options DEFAULT = new Options() {{
        @NotNull File base = new File(SteamUtils.getSteamApps(), "common/Team Fortress 2");
        @Nullable String executable = null;
        switch (OS.get()) {
            case Windows:
                executable = "hl2.exe";
                break;
            case OSX:
                executable = "hl2_osx";
                break;
            case Linux:
                executable = "hl2.sh";
                break;
        }
        script = new File(base, executable);
        args = new String[]{"-game", "tf", "-steam"};
    }};
    private static final Logger LOG = Logger.getLogger(GameLauncher.class.getName());

    private GameLauncher() {
    }

    public static void main(@NotNull String[] args) throws IOException {
        LOG.info(Arrays.toString(args));
        @Nullable String[] command = args;
        if (args.length == 0 && (command = choose()) == null) return; // Interactive
        // Args are tokenized correctly at this point, set up env vars
        @NotNull Map<String, String> env = new HashMap<>(System.getenv());
        if (!env.containsKey("TERM")) env.put("TERM", "xterm"); // Default TERM variable
        @Nullable String dir = null; // TODO
        // Run
        start(command, env, dir, 12345);
    }

    /**
     * Prompt user for execution command.
     *
     * @return tokenized args
     * @throws IOException
     */
    @Nullable
    private static String[] choose() throws IOException {
        int game = 440;
        @Nullable String userArgs = getUserOpts(game);
        @NotNull JFrame frame = new JFrame("Game Launcher");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        @NotNull JPanel p = new JPanel();
        @NotNull JTextField executableField = new JTextField();
        executableField.setText(userArgs);
        executableField.setMinimumSize(new Dimension(300, executableField.getMinimumSize().height));
        executableField.setPreferredSize(executableField.getMinimumSize());
        p.add(executableField);
        @NotNull String[] opts = {"Launch", "Auto", "Cancel"};
        int ret = JOptionPane.showOptionDialog(frame,
                p,
                "Game Launcher",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                opts,
                opts[0]);
        frame.dispose();
        if (ret == 2 || ret < 0) return null; // Cancel
        if (ret == 1) return autoDetect(game).full(); // Auto
        // Launch
        String line = executableField.getText();
        if (line == null || line.isEmpty()) return null;
        return tokenize(line, DEFAULT.full());
    }

    /**
     * Split command, replace %command% with args.
     *
     * @param command command string
     * @param args    %command% replacement
     * @return
     */
    @NotNull
    private static String[] tokenize(String command, @NotNull String... args) {
        LOG.log(Level.INFO, "Tokenize: {0}, {1}", new Object[]{command, Arrays.toString(args)});
        @NotNull StringTokenizer st = new StringTokenizer(command);
        @NotNull String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) cmdarray[i] = st.nextToken();
        @NotNull String[] newcmd = new String[(cmdarray.length + args.length) - 1];
        for (int i = 0, j = -1; i < cmdarray.length; i++) {
            if ("%command%".equals(cmdarray[i])) {
                for (String arg : args) newcmd[i + ++j] = arg;
            } else {
                newcmd[i + j] = cmdarray[i];
            }
        }
        return newcmd;
    }

    @Nullable
    private static Options autoDetect(int appID) throws IOException {
        @NotNull BVDF bin = new BVDF();
        bin.readExternal(DataUtils.mapFile(new File(SteamUtils.getSteam(), "appcache/appinfo.vdf")));
        @NotNull BVDF.DataNode root = bin.getRoot();
        @Nullable BVDF.DataNode gm = root.get(String.valueOf(appID));
        @Nullable BVDF.DataNode sections = gm.get("Sections");
        @Nullable BVDF.DataNode conf = sections.get("CONFIG").get("config");
        String installdir = conf.get("installdir").value.toString();
        @NotNull File dir = new File(SteamUtils.getSteamApps(), "common/" + installdir);
        @Nullable BVDF.DataNode l = conf.get("launch");
        @NotNull Map<String, File> launch = new HashMap<>(l.getChildCount());
        @Nullable String[] gameArgs = null;
        for (int i = 0; i < l.getChildCount(); i++) {
            @NotNull BVDF.DataNode c = (BVDF.DataNode) l.getChildAt(i);
            gameArgs = ((String) c.get("arguments").value).split(" ");
            @NotNull String os = (String) c.get("config").get("oslist").value; // FIXME: Hopefully only one OS will be present
            @NotNull String exe = (String) c.get("executable").value;
            launch.put(os, new File(dir.getPath(), exe));
        }
        String get;
        switch (get()) {
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
     * @param appID steam application ID
     * @return user launch options prepended with %command% if not present
     */
    @Nullable
    private static String getUserOpts(int appID) {
        try {
            @NotNull File f = new File(SteamUtils.getUserData(), "config/localconfig.vdf");
            @Nullable VDFNode game = VDF.load(f).get("UserLocalConfigStore", "Software", "Valve", "Steam", "apps", appID);
            if (game == null) return null;
            @NotNull String str = (String) game.getValue("LaunchOptions");
            if (str == null) return null;
            if (!str.contains("%command%")) str = "%command% " + str;
            return str;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, null, e);
        }
        return null;
    }

    /**
     * Starts the process.
     *
     * @param cmd  command to exec
     * @param env  env vars
     * @param dir  working directory to run game from
     * @param port port to listen on
     * @throws IOException
     */
    private static void start(String[] cmd, Map<String, String> env, String dir, int port) throws IOException {
        LOG.log(Level.INFO, "Starting {0}", new Object[]{Arrays.toString(cmd)});
        LOG.log(Level.INFO, "Env: {0}", env);
        LOG.log(Level.INFO, "Dir: {0}", dir);
        final Process proc = PtyProcess.exec(cmd, env, dir, false);
        @NotNull final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null));
        int truePort = sock.getLocalPort();
        LOG.log(Level.INFO, "Listening on port {0}", truePort);
        @NotNull final Collection<String> queue = new LinkedList<>();
        @NotNull final AggregateOutputStream aggregate = new AggregateOutputStream() {
            /**
             * Ignore input in output since the PTY solution also prints that to the output...
             * TODO: Stop it from doing that, print to output manually to avoid performance hit
             * <p>
             * @throws IOException
             */
            @Override
            public void write(@NotNull byte[] b, int off, int len) throws IOException {
                synchronized (queue) {
                    @NotNull String[] test = new String(Arrays.copyOfRange(b, off, off + len)).split("\n");
                    for (String t : test) {
                        boolean intern;
                        if (queue.contains(t)) {
                            queue.remove(t);
                            intern = true;
                        } else {
                            intern = false;
                        }
                        @NotNull byte[] bytes = ((intern ? "\1" : "") + t + '\n').getBytes("UTF-8");
                        super.write(bytes, 0, bytes.length);
                    }
                }
                flush();
            }
        };
        @NotNull final Thread main = new Thread(new Proxy(proc.getInputStream(), aggregate, "server <--> game") {
            @Override
            protected boolean print(String line) {
                // System.err.println(line); // Steam listens to stderr
                return super.print(line);
            }
        }, "Subprocess");
        main.start();
        @NotNull Thread acceptor = new Thread("Acceptor") {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket client = sock.accept();
                        aggregate.register(client.getOutputStream());
                        @NotNull Proxy pipe = new Proxy(client.getInputStream(), proc.getOutputStream(), "client <--> game") {
                            @Override
                            protected boolean print(String line) {
                                synchronized (queue) {
                                    queue.add(line);
                                }
                                return super.print(line);
                            }
                        };
                        @NotNull Thread t = new Thread(pipe);
                        t.setDaemon(true);
                        t.start();
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        if (sock.isClosed()) {
                            return;
                        }
                        LOG.log(Level.SEVERE, null, e);
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
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, null, e);
                }
                LOG.info("Reaping");
                try {
                    sock.close();
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, null, e);
                }
            }
        }, "Reaper").start();
    }

    private static class Options {

        protected String[] args;
        @Nullable
        protected File script;

        protected Options() {
        }

        public Options(File script, String... args) {
            this.script = script;
            this.args = args;
        }

        @NotNull
        public String[] full() {
            @NotNull String[] full = new String[1 + args.length];
            full[0] = String.valueOf(script);
            System.arraycopy(args, 0, full, 1, args.length);
            return full;
        }
    }

    private static class Proxy implements Runnable {

        private final String name;
        @NotNull
        private final PrintWriter pw;
        @NotNull
        private final Scanner scan;

        /**
         * Pipes in to out.
         *
         * @param in
         * @param out
         * @param name
         */
        Proxy(@NotNull InputStream in, @NotNull OutputStream out, String name) {
            scan = new Scanner(in);
            pw = new PrintWriter(out, true);
            this.name = name;
        }

        @Override
        @SuppressWarnings("empty-statement")
        public void run() {
            while (scan.hasNextLine() && print(scan.nextLine())) ;
            LOG.log(Level.INFO, "Stopped proxying {0}", name);
        }

        /**
         * @param line the line to print
         * @return false if error
         */
        boolean print(String line) {
            pw.println(line);
            return !pw.checkError();
        }
    }
}
