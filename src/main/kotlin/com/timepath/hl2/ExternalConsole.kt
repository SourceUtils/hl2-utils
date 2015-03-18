package com.timepath.hl2

import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.swing.*
import javax.swing.text.DefaultCaret
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import kotlin.platform.platformStatic
import kotlin.concurrent.thread

/**
 * http://www.perkin.org.uk/posts/how-to-fix-stdio-buffering.html
 *
 * @author TimePath
 */
SuppressWarnings("serial")
public open class ExternalConsole protected() : JFrame() {
    private val input: JTextField
    protected val output: JTextArea
    var engine = initScriptEngine()
        private set
    private var pw: PrintWriter? = null
    private var sock: Socket? = null

    {
        output = JTextArea()
        output.setFont(Font("Monospaced", Font.PLAIN, 15))
        output.setEnabled(false)
        val caret = output.getCaret() as DefaultCaret
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)
        val jsp = JScrollPane(output)
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        input = JTextField()
        input.setEnabled(false)
        input.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                if (pw == null) {
                    return
                }
                output.append("] ")
                pw!!.println(input.getText())
                input.setText("")
            }
        })
        val jmb = JMenuBar()
        setJMenuBar(jmb)
        val fileMenu = JMenu("File")
        jmb.add(fileMenu)
        val reload = JMenuItem("Reload script")
        fileMenu.add(reload)
        reload.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                engine = initScriptEngine()
            }
        })
        setTitle("External console")
        //        setAlwaysOnTop(true);
        //        setUndecorated(true);
        setPreferredSize(Dimension(800, 600))
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                if (sock != null) {
                    try {
                        sock!!.close()
                    } catch (ex: IOException) {
                        LOG.log(Level.SEVERE, null, ex)
                    }

                }
                dispose()
            }
        })
        getContentPane().add(jsp, BorderLayout.CENTER)
        getContentPane().add(input, BorderLayout.SOUTH) // TODO: work out better way of sending input
        pack()
    }

    private fun initScriptEngine(): ScriptEngine {
        val factory = ScriptEngineManager()
        val scriptEngine = factory.getEngineByName("JavaScript")
        //        Bindings bindings = engine.createBindings();
        //        bindings.put("loadTime", new Date());
        scriptEngine.getContext().setWriter(pw)
        try {
            scriptEngine.eval(FileReader("extern.js"))
        } catch (ex: ScriptException) {
            Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
        } catch (ex: FileNotFoundException) {
            Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
        }

        return scriptEngine
    }

    throws(javaClass<IOException>())
    protected fun connect(port: Int) {
        sock = Socket(InetAddress.getByName(null), port)
        setIn(sock!!.getInputStream())
        setOut(sock!!.getOutputStream())
    }

    fun setIn(s: InputStream?) {
        output.setEnabled(s != null)
        thread {
            try {
                s?.reader()?.forEachLine {
                    update(it)
                }
            } catch (ex: IOException) {
                Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
            }
        }
    }

    fun update(str: String) {
        parse(str)
        appendOutput(str)
    }

    private fun appendOutput(str: String) = output.append("$str\n")

    protected open fun parse(lines: String) {
        if (!lines.startsWith(">>>")) {
            return
        }
        val str = lines.substring(3)
        System.out.println("Matching $str")
        val m = regex.matcher(str)
        if (!m.matches()) {
            System.out.println("Doesn't match")
            return
        }
        val fn = m.group(1)
        System.out.println(fn)
        val args = m.group(2).split(",")
        System.out.println(System.currentTimeMillis())
        val inv = engine as Invocable
        try {
            inv.invokeFunction(fn, args)
        } catch (ex: ScriptException) {
            Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
        } catch (ex: NoSuchMethodException) {
            Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
        }

        System.out.println(System.currentTimeMillis())
    }

    fun setOut(s: OutputStream?) {
        input.setEnabled(s != null)
        pw = PrintWriter(s, true)
        engine.getContext().setWriter(pw)
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<ExternalConsole>().getName())
        private val regex = Pattern.compile("(\\S+)\\s*[(]\\s*(\\S*)\\s*[)].*")

        public fun exec(cmd: String, breakline: CharSequence?): String {
            val sb = StringBuilder()
            try {
                val sock = Socket(InetAddress.getByName(null), 12345)
                val pw = PrintWriter(sock.getOutputStream(), true)
                val `in` = BufferedReader(InputStreamReader(sock.getInputStream()))
                pw.println(cmd)
                if (breakline != null) {
                    `in`.readLine() // first line is echoed
                    for (line in `in`.lines()) {
                        sb.append(line).append('\n')
                        if (line.contains(breakline)) {
                            break
                        }
                    }
                }
                sock.close()
            } catch (ex: IOException) {
                Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
            }

            return sb.toString()
        }

        throws(javaClass<Exception>())
        public platformStatic fun main(args: Array<String>) {
            val ec = ExternalConsole()
            ec.connect(12345)
            ec.setVisible(true)
        }

        public fun setErr(s: InputStream) {
            thread {
                try {
                    s.reader().forEachLine {
                        System.err.println(it)
                    }
                    System.err.println("Stopped reading stderr")
                } catch (ex: IOException) {
                    Logger.getLogger(javaClass<ExternalConsole>().getName()).log(Level.SEVERE, null, ex)
                }
            }
        }
    }
}
