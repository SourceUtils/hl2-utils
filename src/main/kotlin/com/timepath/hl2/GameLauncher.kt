package com.timepath.hl2

import com.pty4j.PtyProcess
import com.timepath.DataUtils
import com.timepath.io.AggregateOutputStream
import com.timepath.plaf.OS
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.steam.io.bvdf.BVDF
import java.awt.Dimension
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.*
import kotlin.concurrent.thread
import kotlin.platform.platformStatic

/**
 * Starts a game and relay server.
 * TODO: Use Steam runtime on linux
 *
 * @author TimePath
 */
class GameLauncher private() {

    private open class Proxy
    /**
     * Pipes in to out.
     *
     * @param in
     * @param out
     * @param name
     */
    (`in`: InputStream, out: OutputStream, private val name: String) : Runnable {
        private val pw: PrintWriter
        private val scan: Scanner

        init {
            scan = Scanner(`in`)
            pw = PrintWriter(out, true)
        }

        SuppressWarnings("empty-statement")
        override fun run() {
            while (scan.hasNextLine() && print(scan.nextLine()))
                LOG.log(Level.INFO, "Stopped proxying {0}", name)
        }

        /**
         * @param line
        *         the line to print
        *
         * @return false if error
         */
        open fun print(line: String): Boolean {
            pw.println(line)
            return !pw.checkError()
        }
    }

    companion object {

        private val DEFAULT = object : Options() {
            init {
                val base = File(SteamUtils.getSteamApps(), "common/Team Fortress 2")
                val executable = when (OS.get()) {
                    OS.Windows -> "hl2.exe"
                    OS.OSX -> "hl2_osx"
                    OS.Linux -> "hl2.sh"
                    else -> throw NoWhenBranchMatchedException()
                }
                script = File(base, executable)
                args = array("-game", "tf", "-steam")
            }
        }
        private val LOG = Logger.getLogger(javaClass<GameLauncher>().getName())

        throws(javaClass<IOException>())
        public platformStatic fun main(args: Array<String>) {
            LOG.info(Arrays.toString(args))
            var command: Array<out String> = args
            if (args.size() == 0) {
                // Interactive
                val choose = choose()
                if (choose == null) {
                    return
                }
                command = choose
            }
            // Args are tokenized correctly at this point, set up env vars
            val env = HashMap(System.getenv())
            if (!env.containsKey("TERM")) env.put("TERM", "xterm") // Default TERM variable
            val dir: String? = null // TODO
            // Run
            start(command, env, dir, 12345)
        }

        /**
         * Prompt user for execution command.
         *
         * @return tokenized args
        *
         * @throws IOException
         */
        throws(javaClass<IOException>())
        private fun choose(): Array<out String>? {
            val game = 440
            val userArgs = getUserOpts(game)
            val frame = JFrame("Game Launcher")
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            frame.setUndecorated(true)
            frame.setVisible(true)
            frame.setLocationRelativeTo(null)
            val p = JPanel()
            val executableField = JTextField()
            executableField.setText(userArgs)
            executableField.setMinimumSize(Dimension(300, executableField.getMinimumSize().height))
            executableField.setPreferredSize(executableField.getMinimumSize())
            p.add(executableField)
            val opts = array<String>("Launch", "Auto", "Cancel")
            val ret = JOptionPane.showOptionDialog(frame, p, "Game Launcher", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0])
            frame.dispose()
            if (ret == 2 || ret < 0) return null // Cancel
            if (ret == 1) return autoDetect(game)!!.full() // Auto
            // Launch
            val line = executableField.getText()
            if (line == null || line.isEmpty()) return null
            return tokenize(line, *DEFAULT.full())
        }

        /**
         * Split command, replace %command% with args.
         *
         * @param command
        *         command string
         * @param args
        *         %command% replacement
        *
         * @return
         */
        private fun tokenize(command: String, vararg args: String): Array<out String> {
            LOG.log(Level.INFO, "Tokenize: {0}, {1}", array<Any>(command, Arrays.toString(args)))
            val st = StringTokenizer(command)
            val cmdarray = arrayOfNulls<String>(st.countTokens())
            run {
                var i = 0
                while (st.hasMoreTokens()) {
                    cmdarray[i] = st.nextToken()
                    i++
                }
            }
            val newcmd = arrayOfNulls<String>((cmdarray.size() + args.size()) - 1)
            run {
                var i = 0
                var j = -1
                while (i < cmdarray.size()) {
                    if ("%command%" == cmdarray[i]) {
                        for (arg in args) newcmd[i + ++j] = arg
                    } else {
                        newcmd[i + j] = cmdarray[i]
                    }
                    i++
                }
            }
            return newcmd.requireNoNulls()
        }

        throws(javaClass<IOException>())
        private fun autoDetect(appID: Int): Options? {
            val bin = BVDF()
            bin.readExternal(DataUtils.mapFile(File(SteamUtils.getSteam(), "appcache/appinfo.vdf")))
            val root = bin.getRoot()
            val gm = root[appID.toString()]!!
            val sections = gm["Sections"]!!
            val conf = sections["CONFIG"]!!["config"]!!
            val installdir = conf["installdir"]!!.value.toString()
            val dir = File(SteamUtils.getSteamApps(), "common/$installdir")
            val l = conf["launch"]!!
            val launch = HashMap<String, File>(l.getChildCount())
            var gameArgs: Array<String>? = null
            for (i in l.getChildCount().indices) {
                val c = l.getChildAt(i) as BVDF.DataNode
                gameArgs = (c["arguments"]!!.value as String).split(" ")
                val os = c["config"]!!["oslist"]!!.value as String // FIXME: Hopefully only one OS will be present
                val exe = c["executable"]!!.value as String
                launch.put(os, File(dir.getPath(), exe))
            }
            val get: String = when (OS.get()) {
                OS.Windows -> "windows"
                OS.OSX -> "macos"
                OS.Linux -> "linux"
                else -> return null
            }
            return Options(launch[get], *gameArgs!!)
        }

        /**
         * @param appID
        *         steam application ID
        *
         * @return user launch options prepended with %command% if not present
         */
        private fun getUserOpts(appID: Int): String? {
            try {
                val f = File(SteamUtils.getUserData(), "config/localconfig.vdf")
                val game = VDF.load(f)["UserLocalConfigStore", "Software", "Valve", "Steam", "apps", appID]
                if (game == null) return null
                var str: String? = game.getValue("LaunchOptions") as String
                if (str == null) return null
                if (!str!!.contains("%command%")) str = "%command% $str"
                return str
            } catch (e: IOException) {
                LOG.log(Level.SEVERE, null, e)
            }

            return null
        }

        /**
         * Starts the process.
         *
         * @param cmd
        *         command to exec
         * @param env
        *         env vars
         * @param dir
        *         working directory to run game from
         * @param port
        *         port to listen on
        *
         * @throws IOException
         */
        throws(javaClass<IOException>())
        private fun start(cmd: Array<out String>, env: Map<String, String>, dir: String?, port: Int) {
            LOG.log(Level.INFO, "Starting {0}", array<Any>(Arrays.toString(cmd)))
            LOG.log(Level.INFO, "Env: {0}", env)
            LOG.log(Level.INFO, "Dir: {0}", dir)
            val proc = PtyProcess.exec(cmd, env, dir, false)
            val sock = ServerSocket(port, 0, InetAddress.getByName(null))
            val truePort = sock.getLocalPort()
            LOG.log(Level.INFO, "Listening on port {0}", truePort)
            val queue = LinkedList<String>()
            val aggregate = object : AggregateOutputStream() {
                /**
                 * Ignore input in output since the PTY solution also prints that to the output...
                 * TODO: Stop it from doing that, print to output manually to avoid performance hit
                 * <p>
                 * @throws IOException
                 */
                throws(javaClass<IOException>())
                override fun write(b: ByteArray, off: Int, len: Int) {
                    synchronized (queue) {
                        val test = String(Arrays.copyOfRange(b, off, off + len)).split("\n")
                        for (t in test) {
                            val intern: Boolean
                            if (queue.contains(t)) {
                                queue.remove(t)
                                intern = true
                            } else {
                                intern = false
                            }
                            val bytes = ("${if (intern) 1.toChar().toString() else ""}$t\n").toByteArray()
                            super.write(bytes, 0, bytes.size())
                        }
                    }
                    flush()
                }
            }
            val main = Thread(object : Proxy(proc.getInputStream(), aggregate, "server <--> game") {
                override fun print(line: String): Boolean {
                    // System.err.println(line); // Steam listens to stderr
                    return super.print(line)
                }
            }, "Subprocess")
            main.start()
            val acceptor = object : Thread("Acceptor") {
                override fun run() {
                    while (true) {
                        try {
                            val client = sock.accept()
                            aggregate.register(client.getOutputStream())
                            val pipe = object : Proxy(client.getInputStream(), proc.getOutputStream(), "client <--> game") {
                                override fun print(line: String): Boolean {
                                    synchronized (queue) {
                                        queue.add(line)
                                    }
                                    return super.print(line)
                                }
                            }
                            val t = Thread(pipe)
                            t.setDaemon(true)
                            t.start()
                        } catch (ignored: SocketTimeoutException) {
                        } catch (e: IOException) {
                            if (sock.isClosed()) {
                                return
                            }
                            LOG.log(Level.SEVERE, null, e)
                        }

                    }
                }
            }
            acceptor.setDaemon(true)
            acceptor.start()
            thread(name = "Reaper") {
                try {
                    main.join()
                } catch (e: InterruptedException) {
                    LOG.log(Level.SEVERE, null, e)
                }

                LOG.info("Reaping")
                try {
                    sock.close()
                } catch (e: IOException) {
                    LOG.log(Level.SEVERE, null, e)
                }
            }
        }

        private open class Options(var script: File? = null, vararg var args: String) {

            public fun full(): Array<out String> {
                val full = arrayOfNulls<String>(1 + args.size())
                full[0] = script.toString()
                System.arraycopy(args, 0, full, 1, args.size())
                return full.requireNoNulls()
            }
        }
    }
}
