package com.timepath.hl2

import com.pty4j.PtyProcess
import com.timepath.DataUtils
import com.timepath.Logger
import com.timepath.io.AggregateOutputStream
import com.timepath.plaf.OS
import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDF
import com.timepath.steam.io.bvdf.BVDF
import com.timepath.with
import java.awt.Dimension
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.util.Arrays
import java.util.HashMap
import java.util.LinkedList
import java.util.StringTokenizer
import java.util.logging.Level
import javax.swing.*
import kotlin.concurrent.thread
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

/**
 * Starts a game and relay server.
 * TODO: Use Steam runtime on linux
 */
class GameLauncher private constructor() {

    /** Pipes [input] to [output]. */
    private open class Proxy(private val input: InputStream,
                             private val output: OutputStream,
                             private val name: String) : Runnable {
        private val pw = PrintWriter(output, true)

        override fun run() {
            input.reader().useLines { lines ->
                for (line in lines) {
                    if (!print(line)) break
                }
            }
            LOG.info { "Stopped proxying ${name}" }
        }

        /**
         * @param line the line to print
         * @return false if error
         */
        open fun print(line: String) = pw.println(line) let { !pw.checkError() }
    }

    companion object {

        private val DEFAULT = run {
            val base = File(SteamUtils.getSteamApps(), "common/Team Fortress 2")
            val executable = when (OS.get()) {
                OS.Windows -> "hl2.exe"
                OS.OSX -> "hl2_osx"
                OS.Linux -> "hl2.sh"
                else -> throw NoWhenBranchMatchedException()
            }
            Options(File(base, executable), listOf("-game", "tf", "-steam"))
        }
        internal val LOG = Logger()

        public platformStatic fun main(args: Array<String>) {
            LOG.info { Arrays.toString(args) }
            val command = when {
                args.isEmpty() -> choose() ?: return
                else -> args.toLinkedList()
            }
            val env = HashMap(System.getenv())
            env.getOrPut("TERM", { "xterm" }) // Default TERM variable
            val dir: String = "." // TODO
            start(command, env, dir, 12345)
        }

        /**
         * Prompt user for execution command.
         *
         * @return tokenized args
         */
        private fun choose(): MutableList<String>? {
            val appID = 440
            val userArgs = getUserOpts(appID)
            val frame = JFrame("Game Launcher") with {
                setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                setUndecorated(true)
                setVisible(true)
                setLocationRelativeTo(null)
            }
            val executableField = JTextField() with {
                setText(userArgs)
                setMinimumSize(Dimension(300, getMinimumSize().height))
                setPreferredSize(getMinimumSize())
            }
            val opts = arrayOf("Launch", "Auto", "Cancel")
            val ret = JOptionPane.showOptionDialog(frame, JPanel() with { add(executableField) }, "Game Launcher",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0])
            frame.dispose()
            if (ret == 2 || ret < 0) return null // Cancel
            if (ret == 1) return autoDetect(appID)!!.full // Auto
            // Launch
            val line = executableField.getText()
            return when {
                line.isNullOrEmpty() -> null
                else -> tokenize(line, DEFAULT.full)
            }
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
        private fun tokenize(command: String, args: List<String>): MutableList<String> {
            LOG.info { "Tokenize: ${command}, ${args}" }
            val st = StringTokenizer(command)
            val tokens = st.toList().filterIsInstance(javaClass<String>())
            return tokens.flatMapTo(LinkedList<String>()) {
                when (it) {
                    "%command%" -> args.toList()
                    else -> listOf(it)
                }
            }
        }

        throws(IOException::class)
        private fun autoDetect(appID: Int): Options? {
            val bin = BVDF()
            bin.readExternal(DataUtils.mapFile(File(SteamUtils.getSteam(), "appcache/appinfo.vdf")))
            val root = bin.root
            val gm = root[appID.toString()]!!
            val sections = gm["Sections"]!!
            val conf = sections["CONFIG"]!!["config"]!!
            val installdir = conf["installdir"]!!.value.toString()
            val dir = File(SteamUtils.getSteamApps(), "common/$installdir")
            val l = conf["launch"]!!
            val launch = HashMap<String, File>(l.getChildCount())
            var gameArgs: List<String>? = null
            repeat(l.getChildCount()) {
                val c = l.getChildAt(it) as BVDF.DataNode
                gameArgs = (c["arguments"]!!.value as String).splitBy(" ")
                val os = c["config"]!!["oslist"]!!.value as String // FIXME: Hopefully only one OS will be present
                val exe = c["executable"]!!.value as String
                launch.put(os, File(dir.getPath(), exe))
            }
            val os: String = when (OS.get()) {
                OS.Windows -> "windows"
                OS.OSX -> "macos"
                OS.Linux -> "linux"
                else -> return null
            }
            return Options(launch[os]!!, gameArgs!!)
        }

        /**
         * @param appID
         *         steam application ID
         *
         * @return user launch options prepended with %command% if not present
         */
        private fun getUserOpts(appID: Int): String? {
            val f = File(SteamUtils.getUserData(), "config/localconfig.vdf")
            val game = VDF.load(f)["UserLocalConfigStore", "Software", "Valve", "Steam", "apps", appID] ?: return null
            val str = game.getValue("LaunchOptions") as? String ?: return null
            return when {
                "%command%" in str -> str
                else -> "%command% $str"
            }
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
        private fun start(cmd: MutableList<String>, env: Map<String, String>, dir: String?, port: Int) {
            val nopty = true
            val temp = createTempFile(suffix = ".log") with {
                delete()
                deleteOnExit()
            }
            Files.createSymbolicLink(temp.toPath(), File("/proc/self/fd/1").toPath())
            cmd.add("+con_logfile ${temp.canonicalPath}")

            LOG.info { "Starting ${cmd}" }
            LOG.info { "Env: ${env}" }
            LOG.info { "Dir: ${dir}" }

            val sock = ServerSocket(port, 0, InetAddress.getByName(null))
            val truePort = sock.getLocalPort()
            LOG.info { "Listening on port ${truePort}" }

            val proc = when {
                nopty -> ProcessBuilder(*cmd.toTypedArray()).with {
                    redirectErrorStream(true)
                    environment().putAll(env)
                    directory(File(dir))
                }.start()
                else -> PtyProcess.exec(cmd.toTypedArray(), env, dir, true)
            }
            val queue = LinkedList<String>()
            val aggregate = object : AggregateOutputStream() {
                /**
                 * Ignore input in output since the PTY solution also prints that to the output...
                 * TODO: Stop it from doing that, print to output manually to avoid performance hit
                 */
                override fun write(b: ByteArray, off: Int, len: Int) {
                    synchronized (queue) {
                        StringBuilder {
                            for (str in String(Arrays.copyOfRange(b, off, off + len)).splitToSequence("\n")) {
                                if (str in queue) {
                                    queue.remove(str)
                                    append(1.toChar())
                                }
                                append(str)
                                append('\n')
                            }
                        } let {
                            val bytes = it.toString().toByteArray()
                            super.write(bytes, 0, bytes.size())
                            flush()
                        }
                    }
                }
            }
            thread(name = "Acceptor", daemon = true) {
                while (true) {
                    try {
                        val client = sock.accept()
                        aggregate.register(client.getOutputStream())
                        val proxy = object : Proxy(client.getInputStream(), proc.getOutputStream(), "client <--> game") {
                            override fun print(line: String): Boolean {
                                synchronized (queue) {
                                    queue.add(line)
                                }
                                return super.print(line)
                            }
                        }
                        thread(daemon = true) {
                            proxy.run()
                        }
                    } catch (ignored: SocketTimeoutException) {
                    } catch (e: IOException) {
                        if (sock.isClosed()) return@thread
                        LOG.log(Level.SEVERE, { null }, e)
                    }
                }
            }
            object : Proxy(proc.getInputStream(), aggregate, "server <--> game") {
                override fun print(line: String): Boolean {
                    // System.err.println(line); // Steam listens to stderr
                    return super.print(line)
                }
            }.run()
            sock.close()
        }

        private open class Options(val script: File, val args: List<String>) {
            val full by Delegates.lazy { linkedListOf(script.toString(), *args.toTypedArray()) }
        }
    }
}
