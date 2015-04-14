package com.timepath.hl2

import com.timepath.hl2.io.captions.VCCD
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.io.VDF
import com.timepath.steam.io.storage.ACF
import com.timepath.util.Trie
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.*
import java.util.HashMap
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import java.util.zip.CRC32
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import kotlin.concurrent.thread
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

class VCCDTest : JFrame {
    private val trie = Trie()
    private val hashmap = HashMap<Int, StringPair>()
    private var consoleMode: JCheckBoxMenuItem by Delegates.notNull()
    private var saveFile: File? = null
    private var jTable1: JTable by Delegates.notNull()
    private var jTextField3: JTextField by Delegates.notNull()
    private var jTextField4: JTextField by Delegates.notNull()

    inline fun <T> T.configure(configure: T.(T) -> Unit): T = run { configure(this); this }

    /** Load known mappings from preferences */
    fun loadPrefs() {
        for (channel in prefs.childrenNames()) {
            for (name in prefs.node(channel).keys()) {
                val hash = prefs.getInt(name, -1)
                LOG.log(Level.FINER, "$name = $hash")
                if (hash != -1) {
                    hashmap[hash] = StringPair(name, channel)
                    trie.add(name)
                }
            }
        }
    }

    init {
        loadPrefs()
    }

    constructor() {
        setTitle("Caption Editor")
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        setJMenuBar(JMenuBar().configure {
            add (JMenu("File").configure {
                add(JMenuItem("New").configure {
                    setMnemonic('N')
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { createNew() })
                })
                add(JMenuItem("Open").configure {
                    setMnemonic('O')
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { loadCaptions() })
                })
                add(JMenuItem("Import").configure {
                    setMnemonic('I')
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { importCaptions() })
                })
                add(JMenuItem("Export").configure {
                    setMnemonic('X')
                    addActionListener(ActionListener { export() })
                })
                add(JMenuItem("Export all").configure {
                    setMnemonic('E')
                    addActionListener(ActionListener { exportAll() })
                })
                add(JMenuItem("Generate hash codes").configure {
                    addActionListener(ActionListener { generateHash() })
                })
                add(JMenuItem("Save").configure {
                    setMnemonic('S')
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { saveCaptions() })
                })
                add(JMenuItem("Save As...").configure {
                    setMnemonic('V')
                    addActionListener(ActionListener { saveCaptionsAs() })
                })
            })

            add(JMenu("Edit").configure {
                add(JMenuItem("Insert row").configure {
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { insertRow() })
                })
                add(JMenuItem("Delete row").configure {
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK))
                    addActionListener(ActionListener { deleteRow() })
                })
                add(JMenuItem("Goto").configure {
                    addActionListener(ActionListener { gotoRow() })
                })
            })

            add(JMenu("Settings").configure {
                add(JCheckBoxMenuItem("Console compatible").configure {
                    consoleMode = this
                })
            })
            add(JMenu("Help").configure {
                add(JMenuItem("Formatting").configure {
                    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0))
                    addActionListener(ActionListener { formattingHelp() })
                })
            })
        })
        getContentPane().configure {
            setLayout(BoxLayout(this, BoxLayout.Y_AXIS))
            add(JPanel().configure {
                setBorder(BorderFactory.createTitledBorder("CRC32"))
                setMaximumSize(Dimension(Int.MAX_VALUE, 83))
                setLayout(BorderLayout())
                add(JTextField().configure {
                    jTextField3 = this
                    getDocument().addDocumentListener(object : DocumentListener {
                        fun updateHash() = jTextField4.setText(hexFormat(VCCD.hash(jTextField3.getText())))
                        override fun insertUpdate(e: DocumentEvent) = updateHash()
                        override fun removeUpdate(e: DocumentEvent) = updateHash()
                        override fun changedUpdate(e: DocumentEvent) = updateHash()
                    })
                }, BorderLayout.PAGE_START)
                add(JTextField().configure {
                    jTextField4 = this
                    setEditable(false)
                    setText("The CRC will appear here")
                }, BorderLayout.PAGE_END)
            })
            add(JScrollPane(JTable().configure {
                jTable1 = this
                setAutoCreateRowSorter(true)
                setModel(object : DefaultTableModel(array("CRC32", "Key", "Value"), 0) {
                    val types = array(javaClass<Any>(), javaClass<String>(), javaClass<String>())
                    override fun getColumnClass(columnIndex: Int) = types[columnIndex]
                    val canEdit = booleanArray(false, true, true)
                    override fun isCellEditable(row: Int, column: Int) = canEdit[column]
                })
                setCursor(Cursor(Cursor.DEFAULT_CURSOR))
                setRowHeight(24)
                val tableColumnModel = getColumnModel()
                if (tableColumnModel.getColumnCount() > 0) {
                    tableColumnModel.getColumn(0).configure {
                        setMinWidth(85)
                        setPreferredWidth(85)
                        setMaxWidth(85)
                    }
                    tableColumnModel.getColumn(1).configure {
                        setPreferredWidth(160)
                        setCellEditor(getKeyEditor())
                    }
                    tableColumnModel.getColumn(2).configure {
                        setPreferredWidth(160)
                    }
                }
            }))
        }
    }

    private fun generateHash() {
        thread {
            val pb = JProgressBar().configure {
                setIndeterminate(true)
            }
            val frame = JFrame("Generating hash codes...").configure {
                add(pb)
                setMinimumSize(Dimension(300, 50))
                setLocationRelativeTo(null)
                setVisible(true)
            }
            val map = HashMap<Int, StringPair>()
            LOG.info("Generating hash codes ...")
            try {
                val crc = CRC32()
                val captions = ACF.fromManifest(440).find("game_sounds")
                pb.configure {
                    setMaximum(captions.size())
                    setIndeterminate(false)
                }
                captions.forEachIndexed { i, file ->
                    LOG.log(Level.INFO, "Parsing $file")
                    file.openStream()?.let {
                        VDF.load(it).getNodes().forEach {
                            val str = it.getCustom() as String
                            val channel = it.getValue("channel") as? String ?: CHAN_UNKNOWN
                            LOG.log(Level.FINER, str)
                            crc.reset()
                            crc.update(str.toByteArray())
                            map[crc.getValue().toInt()] = StringPair(str, channel)
                        }
                    }
                    pb.setValue(i + 1)
                }
            } catch (ex: IOException) {
                LOG.log(Level.WARNING, "Error generating hash codes", ex)
            }
            hashmap.putAll(map)
            persistHashmap(hashmap)
            frame.dispose()
        }
    }

    private fun attemptDecode(hash: Int): String? {
        if (hash !in hashmap) LOG.log(Level.FINE, "hashmap does not contain ${hash}")
        return hashmap[hash]?.name
    }

    private fun loadCaptions() {
        try {
            val fc = NativeFileChooser().let {
                it.setTitle("Open")
                it.addFilter(BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"))
                it.setParent(this)
                it
            }
            val files = fc.choose()
            if (files == null) {
                return
            }
            val entries: kotlin.List<VCCD.VCCDEntry>
            try {
                entries = VCCD.load(FileInputStream(files[0]))!!
            } catch (ex: FileNotFoundException) {
                LOG.log(Level.SEVERE, null, ex)
                return
            }

            LOG.log(Level.INFO, "Entries: {0}", entries.size())
            val model = jTable1.getModel() as DefaultTableModel
            run {
                var i = model.getRowCount() - 1
                while (i >= 0) {
                    model.removeRow(i)
                    i--
                }
            }
            for (entry in entries) {
                model.addRow(array(hexFormat(entry.hash), attemptDecode(entry.hash), entry.value))
            }
            saveFile = files[0]
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun save(flag: Boolean) {
        if ((saveFile == null) || flag) {
            try {
                val fc = NativeFileChooser().let {
                    it.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG)
                    it.setTitle("Save (as closecaption_<language>.dat)")
                    it.addFilter(BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"))
                    it.setParent(this)
                    it
                }
                val fs = fc.choose()
                if (fs == null) {
                    return
                }
                saveFile = fs[0]
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
                return
            }

        }
        if (jTable1.isEditing()) {
            jTable1.getCellEditor().stopCellEditing()
        }
        val entries = LinkedList<VCCD.VCCDEntry>()
        val model = jTable1.getModel()
        for (i in model.getRowCount().indices) {
            var crc = model.getValueAt(i, 0)
            if ((model.getValueAt(i, 1) != null) && !model.getValueAt(i, 1).toString().isEmpty()) {
                crc = hexFormat(VCCD.hash(model.getValueAt(i, 1).toString()))
            }
            val hash = java.lang.Long.parseLong(crc.toString().toLowerCase(), 16).toInt()
            val key = model.getValueAt(i, 1)
            val token = when (key) {
                is String -> key.toString()
                else -> null
            }
            if (hash !in hashmap) {
                hashmap[hash] = StringPair(token, CHAN_UNKNOWN)
            }
            entries.add(VCCD.VCCDEntry(hash, model.getValueAt(i, 2).toString()))
        }
        persistHashmap(hashmap)
        try {
            VCCD.save(entries, FileOutputStream(saveFile!!), consoleMode.isSelected(), consoleMode.isSelected())
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }
    }

    private fun saveCaptions() = save(false)

    private fun importCaptions() {
        try {
            val fc = NativeFileChooser().configure {
                setTitle("Import")
                setParent(this@VCCDTest)
                addFilter(BaseFileChooser.ExtensionFilter("VCCD Source Files", ".txt"))
            }
            val files = fc.choose()
            if (files == null) {
                return
            }
            val entries = VCCD.parse(FileInputStream(files[0]))
            LOG.log(Level.INFO, "Entries: {0}", entries.size())
            val model = jTable1.getModel() as DefaultTableModel
            model.getRowCount().indices.reversed().forEach { model.removeRow(it) }
            entries.forEach {
                val hash = it.hash
                val token = it.key
                if (hash !in hashmap) {
                    hashmap[hash] = StringPair(token, CHAN_UNKNOWN)
                }
                model.addRow(array(hexFormat(it.hash), it.key, it.value))
            }
            persistHashmap(hashmap)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun insertRow() {
        val model = jTable1.getModel() as DefaultTableModel
        model.addRow(array(0, "", ""))
    }

    private fun deleteRow() {
        val model = jTable1.getModel() as DefaultTableModel
        val row = jTable1.getSelectedRow()
        if (row == -1) return
        var newRow = Math.min(row, jTable1.getRowCount() - 1)
        if (row == (jTable1.getRowCount() - 1)) {
            newRow = jTable1.getRowCount() - 2
        }
        LOG.log(Level.FINER, "New row: ${newRow}")
        model.removeRow(row)
        if (jTable1.getRowCount() > 0) {
            jTable1.setRowSelectionInterval(newRow, newRow)
        }
    }

    private fun createNew() {
        val model = jTable1.getModel() as DefaultTableModel
        model.getRowCount().indices.reversed().forEach { model.removeRow(it) }
        model.addRow(array(0, "", ""))
    }

    private fun formattingHelp() {
        val message = try {
            javaClass.getResource("/VCCDTest.txt").readText()
        } catch (ignored: IOException) {
            "Unable to load"
        }

        val jsp = JScrollPane(JTextArea(message)).configure {
            setPreferredSize(Dimension(500, 500))
        }
        JOptionPane(jsp, JOptionPane.INFORMATION_MESSAGE).createDialog(this, "Formatting").configure {
            setResizable(true)
            setModal(false)
            setVisible(true)
        }
    }

    private fun export() {
        val sb = StringBuilder(jTable1.getRowCount() * 100) // Rough estimate
        val model = jTable1.getModel()
        for (i in model.getRowCount().indices) {
            sb.append("${model.getValueAt(i, 0)}\t${model.getValueAt(i, 2)}\n")
        }
        val pane = JTextArea(sb.toString()).configure {
            val s = Toolkit.getDefaultToolkit().getScreenSize()
            setLineWrap(false)
            setPreferredSize(Dimension(s.width / 3, s.height / 2))
            setEditable(false)
            setOpaque(false)
            setBackground(Color(0, 0, 0, 0))
        }
        val jsp = JScrollPane(pane).configure {
            setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        }
        JOptionPane.showMessageDialog(this, jsp, "Hash List", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showInputDialog(): Int {

        val inputValue = JOptionPane.showInputDialog("Enter row", jTable1.getSelectedRow() + 1)
        var intValue = -1
        if (inputValue != null) {
            try {
                intValue = Integer.parseInt(inputValue) - 1
            } catch (e: NumberFormatException) {
                showInputDialog()
            }

            if (intValue < 0) {
                showInputDialog()
            }
        }
        return intValue
    }

    private fun gotoRow() {
        var row = showInputDialog()
        if (row < 0) {
            return
        }
        if (row > jTable1.getRowCount()) {
            row = jTable1.getRowCount()
        }
        jTable1.setRowSelectionInterval(row, row)
        jTable1.scrollRectToVisible(jTable1.getCellRect(row, 0, true))
    }

    private fun saveCaptionsAs() = save(true)

    private fun exportAll() {
        try {
            val fc = NativeFileChooser().let {
                it.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG)
                it.setTitle("Export")
                it.addFilter(BaseFileChooser.ExtensionFilter("XML", ".xml"))
                it.setParent(this)
            }
            val fs = fc.choose()
            if (fs == null) {
                return
            }
            val file = fs[0]
            saveFile = file
            prefs.exportSubtree(FileOutputStream(file))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: BackingStoreException) {
            LOG.log(Level.SEVERE, null, ex)
        }
    }

    private class StringPair(var name: String?, var channel: String)

    companion object {

        private val LOG = Logger.getLogger(javaClass<VCCDTest>().getName())
        private val prefs = Preferences.userRoot().node("timepath").node("hl2-caption-editor")
        private val CHAN_UNKNOWN = "CHAN_UNKNOWN"

        public platformStatic fun main(args: Array<String>) {
            try {
                if (args.size() > 0) {
                    val input = VCCD.parse(FileInputStream(args[0]))
                    val hashmap = HashMap<Int, StringPair>()
                    for (i in input) {
                        // Learning
                        val crc = i.hash
                        val token = i.key
                        val hash = java.lang.Long.parseLong(crc.toString().toLowerCase(), 16)
                        hashmap[hash.toInt()] = StringPair(token, CHAN_UNKNOWN)
                    }
                    persistHashmap(hashmap)
                    VCCD.save(input, FileOutputStream("closecaption_english.dat"))
                    return
                }
            } catch (ex: FileNotFoundException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            EventQueue.invokeLater {
                VCCDTest().let {
                    it.pack()
                    it.setLocationRelativeTo(null)
                    it.setVisible(true)
                }
            }
        }

        private fun persistHashmap(map: Map<Int, StringPair>) {
            for (entry in map.entrySet()) {
                val key = entry.getKey()
                val value = entry.getValue().name
                if (value == null) {
                    continue
                }
                prefs.node(entry.getValue().channel).putInt(value, key)
            }
        }

        private fun hexFormat(i: Int): String {
            var str = Integer.toHexString(i).toUpperCase()
            val pad = 8 - str.length()
            if (pad > 0) {
                str = "0".repeat(pad) + str
            }
            return str
        }

        private fun getKeyEditor(): TableCellEditor {
            val t = JTextField()
            // new StringAutoCompleter(t, trie, 2);
            return DefaultCellEditor(t)
        }
    }
}
