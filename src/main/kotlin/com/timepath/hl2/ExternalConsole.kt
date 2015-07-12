package com.timepath.hl2

import com.timepath.Logger
import com.timepath.with
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.util.Date
import java.util.logging.Level
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.swing.*
import javax.swing.text.DefaultCaret
import kotlin.concurrent.thread
import kotlin.platform.platformStatic

/**
 * http://www.perkin.org.uk/posts/how-to-fix-stdio-buffering.html
 */
public open class ExternalConsole protected constructor() : JFrame() {
    private val input: JTextField
    protected val output: JTextArea
    var engine = initScriptEngine()
        private set
    private var pw: PrintWriter? = null
    private var sock: Socket? = null

    init {
        output = JTextArea() with {
            setFont(Font("Monospaced", Font.PLAIN, 15))
            setEnabled(false)
            getCaret() as DefaultCaret with {
                setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE)
            }
        }
        getContentPane().add(JScrollPane(output) with {
            setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
            setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        }, BorderLayout.CENTER)
        input = JTextField() with {
            setEnabled(false)
            addActionListener(ActionListener {
                pw?.let { pw ->
                    output.append("] ")
                    pw.println(input.getText())
                    input.setText("")
                }
            })
        }
        getContentPane().add(input, BorderLayout.SOUTH) // TODO: work out better way of sending input
        setJMenuBar(JMenuBar() with {
            add(JMenu("File") with {
                add(JMenuItem("Reload script") with {
                    addActionListener { engine = initScriptEngine() }
                })
            })
        })
        setTitle("External console")
        // setAlwaysOnTop(true);
        // setUndecorated(true);
        setPreferredSize(Dimension(800, 600))
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                try {
                    sock?.close()
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, { null }, ex)
                }
                dispose()
            }
        })
        pack()
    }

    private fun initScriptEngine(file: String = "extern.js"): ScriptEngine {
        return ScriptEngineManager().getEngineByExtension(file.substringAfterLast("."))!! with {
            val bindings = createBindings()
            bindings["loadTime"] = Date()
            getContext().setWriter(pw)
            try {
                eval(FileReader(file))
            } catch (ex: ScriptException) {
                LOG.log(Level.SEVERE, { null }, ex)
            } catch (ex: FileNotFoundException) {
                LOG.log(Level.SEVERE, { null }, ex)
            }
        }
    }

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
                LOG.log(Level.SEVERE, { null }, ex)
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
        val m = regex.match(str)
        if (m == null) {
            System.out.println("Doesn't match")
            return
        }
        val fn = m.groups[1]!!.value
        System.out.println(fn)
        val args = m.groups[2]!!.value.splitBy(",")
        System.out.println(System.currentTimeMillis())
        try {
            (engine as Invocable).invokeFunction(fn, args)
        } catch (ex: ScriptException) {
            LOG.log(Level.SEVERE, { null }, ex)
        } catch (ex: NoSuchMethodException) {
            LOG.log(Level.SEVERE, { null }, ex)
        }
        System.out.println(System.currentTimeMillis())
    }

    fun setOut(s: OutputStream?) {
        input.setEnabled(s != null)
        pw = PrintWriter(s!!, true)
        engine.getContext().setWriter(pw)
    }

    companion object {

        private val LOG = Logger()
        private val regex = "(\\S+)\\s*[(]\\s*(\\S*)\\s*[)].*".toRegex()

        public fun exec(cmd: String, breakline: CharSequence?): String = StringBuilder {
            try {
                val sock = Socket(InetAddress.getByName(null), 12345)
                val pw = PrintWriter(sock.getOutputStream(), true)
                val br = sock.getInputStream().bufferedReader()
                pw.println(cmd)
                if (breakline != null) {
                    br.readLine() // first line is echoed
                    for (line in br.lines()) {
                        appendln(line)
                        if (breakline in line) break
                    }
                }
                sock.close()
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, { null }, ex)
            }
        }.toString()

        public platformStatic fun main(args: Array<String>) {
            ExternalConsole() let {
                it.connect(12345)
                it.setVisible(true)
            }
        }
    }
}
