package com.timepath.hl2

import com.timepath.hl2.io.captions.VCCD
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.io.VDF
import com.timepath.steam.io.storage.ACF
import com.timepath.util.Trie

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.*
import java.text.MessageFormat
import java.util.HashMap
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences
import java.util.zip.CRC32
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
SuppressWarnings("serial")
class VCCDTest private() : JFrame() {
    private val trie = Trie()
    private val hashmap = HashMap<Int, StringPair>()
    private var consoleMode: JCheckBoxMenuItem? = null
    private var saveFile: File? = null
    private var jTable1: JTable? = null
    private var jTextField3: JTextField? = null
    private var jTextField4: JTextField? = null

    {
        // Load known mappings from preferences
        try {
            for (channel in prefs.childrenNames()) {
                for (name in prefs.node(channel).keys()) {
                    val hash = prefs.getInt(name, -1)
                    LOG.log(Level.FINER, "{0} = {1}", array<Any>(name, hash))
                    if (hash != -1) {
                        hashmap.put(hash, StringPair(name, channel))
                        trie.add(name)
                    }
                }
            }
        } catch (ex: BackingStoreException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        initComponents()
        jTextField3!!.getDocument().addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                updateHash()
            }

            override fun removeUpdate(e: DocumentEvent) {
                updateHash()
            }

            override fun changedUpdate(e: DocumentEvent) {
                updateHash()
            }

            public fun updateHash() {
                jTextField4!!.setText(hexFormat(VCCD.hash(jTextField3!!.getText())))
            }
        })
    }

    private fun generateHash() {
        Thread {
            val frame = JFrame("Generating hash codes...")
            val pb = JProgressBar()
            pb.setIndeterminate(true)
            frame.add(pb)
            frame.setMinimumSize(Dimension(300, 50))
            frame.setLocationRelativeTo(null)
            frame.setVisible(true)
            val map = HashMap<Int, StringPair>()
            LOG.info("Generating hash codes ...")
            try {
                val crc = CRC32()
                val caps = ACF.fromManifest(440)!!.find("game_sounds")
                pb.setMaximum(caps.size())
                pb.setIndeterminate(false)
                var i = 0
                for (f in caps) {
                    LOG.log(Level.INFO, "Parsing {0}", f)
                    val root = VDF.load(f.openStream()!!)
                    for (node in root.getNodes()) {
                        val str = node.getCustom() as String
                        val channel = node.getValue("channel", CHAN_UNKNOWN) as String
                        LOG.log(Level.FINER, str)
                        crc.reset()
                        crc.update(str.toByteArray())
                        map.put(crc.getValue().toInt(), StringPair(str, channel))
                    }
                    pb.setValue(++i)
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
        if (!hashmap.containsKey(hash)) {
            //            logger.log(Level.INFO, "hashmap does not contain {0}", hash);
            return null
        }
        return hashmap[hash].name
    }

    private fun initMenu() {
        val menuBar = JMenuBar()
        val jMenu1 = JMenu("File")
        jMenu1.add(object : JMenuItem("New") {
            {
                setMnemonic('N')
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        createNew(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Open") {
            {
                setMnemonic('O')
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        loadCaptions(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Import") {
            {
                setMnemonic('I')
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        importCaptions(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Export") {
            {
                setMnemonic('X')
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        export(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Export all") {
            {
                setMnemonic('E')
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        exportAll(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Generate hash codes") {
            {
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        generateHash(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Save") {
            {
                setMnemonic('S')
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        saveCaptions(e)
                    }
                })
            }
        })
        jMenu1.add(object : JMenuItem("Save As...") {
            {
                setMnemonic('V')
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        saveCaptionsAs(e)
                    }
                })
            }
        })
        menuBar.add(jMenu1)
        val jMenu2 = JMenu("Edit")
        jMenu2.add(object : JMenuItem("Insert row") {
            {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        insertRow(e)
                    }
                })
            }
        })
        jMenu2.add(object : JMenuItem("Delete row") {
            {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        deleteRow(e)
                    }
                })
            }
        })
        jMenu2.add(object : JMenuItem("Goto") {
            {
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        gotoRow(e)
                    }
                })
            }
        })
        menuBar.add(jMenu2)
        menuBar.add(object : JMenu("Settings") {
            {
                consoleMode = JCheckBoxMenuItem("Console compatible")
                add(consoleMode)
            }
        })
        val jMenu3 = JMenu("Help")
        jMenu3.add(object : JMenuItem("Formatting") {
            {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0))
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        formattingHelp(e)
                    }
                })
            }
        })
        menuBar.add(jMenu3)
        setJMenuBar(menuBar)
    }

    private fun initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        setTitle("Caption Editor")
        getContentPane().setLayout(BoxLayout(getContentPane(), BoxLayout.Y_AXIS))
        getContentPane().add(object : JPanel() {
            {
                setBorder(BorderFactory.createTitledBorder("CRC32"))
                setMaximumSize(Dimension(2147483647, 83))
                setLayout(BorderLayout())
                jTextField3 = JTextField()
                add(jTextField3, BorderLayout.PAGE_START)
                jTextField4 = JTextField()
                jTextField4!!.setEditable(false)
                jTextField4!!.setText("The CRC will appear here")
                add(jTextField4, BorderLayout.PAGE_END)
            }
        })
        jTable1 = JTable()
        jTable1!!.setAutoCreateRowSorter(true)
        jTable1!!.setModel(object : DefaultTableModel(array("CRC32", "Key", "Value"), 0) {
            var types = array(javaClass<Any>(), javaClass<String>(), javaClass<String>())
            var canEdit = booleanArray(false, true, true)

            override fun getColumnClass(columnIndex: Int): Class<out Any> {
                return types[columnIndex]
            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                return canEdit[column]
            }
        })
        jTable1!!.setCursor(Cursor(Cursor.DEFAULT_CURSOR))
        jTable1!!.setRowHeight(24)
        if (jTable1!!.getColumnModel().getColumnCount() > 0) {
            jTable1!!.getColumnModel().getColumn(0).setMinWidth(85)
            jTable1!!.getColumnModel().getColumn(0).setPreferredWidth(85)
            jTable1!!.getColumnModel().getColumn(0).setMaxWidth(85)
            jTable1!!.getColumnModel().getColumn(1).setPreferredWidth(160)
            jTable1!!.getColumnModel().getColumn(1).setCellEditor(getKeyEditor())
            jTable1!!.getColumnModel().getColumn(2).setPreferredWidth(160)
        }
        getContentPane().add(JScrollPane(jTable1))
        initMenu()
        pack()
    }

    private fun loadCaptions(evt: ActionEvent) {
        try {
            val fc = NativeFileChooser()
            fc.setTitle("Open")
            fc.addFilter(BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"))
            fc.setParent(this)
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
            val model = jTable1!!.getModel() as DefaultTableModel
            run {
                var i = model.getRowCount() - 1
                while (i >= 0) {
                    model.removeRow(i)
                    i--
                }
            }
            for (entry in entries) {
                model.addRow(array<Any>(hexFormat(entry.hash), attemptDecode(entry.hash)!!, entry.value!!))
            }
            saveFile = files[0]
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun save(flag: Boolean) {
        if ((saveFile == null) || flag) {
            try {
                val fc = NativeFileChooser()
                fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG)
                fc.setTitle("Save (as closecaption_<language>.dat)")
                fc.addFilter(BaseFileChooser.ExtensionFilter("VCCD Binary Files", ".dat"))
                fc.setParent(this)
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
        if (jTable1!!.isEditing()) {
            jTable1!!.getCellEditor().stopCellEditing()
        }
        val entries = LinkedList<VCCD.VCCDEntry>()
        val model = jTable1!!.getModel()
        for (i in model.getRowCount().indices) {
            var crc = model.getValueAt(i, 0)
            if ((model.getValueAt(i, 1) != null) && !model.getValueAt(i, 1).toString().isEmpty()) {
                crc = hexFormat(VCCD.hash(model.getValueAt(i, 1).toString()))
            }
            val hash = java.lang.Long.parseLong(crc.toString().toLowerCase(), 16).toInt()
            val key = model.getValueAt(i, 1)
            val token = if ((key is String)) key.toString() else null
            if (!hashmap.containsKey(hash)) {
                hashmap.put(hash, StringPair(token, CHAN_UNKNOWN))
            }
            entries.add(VCCD.VCCDEntry(hash, model.getValueAt(i, 2).toString()))
        }
        persistHashmap(hashmap)
        try {
            VCCD.save(entries, FileOutputStream(saveFile), consoleMode!!.isSelected(), consoleMode!!.isSelected())
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun saveCaptions(evt: ActionEvent) {
        save(false)
    }

    private fun importCaptions(evt: ActionEvent) {
        try {
            val fc = NativeFileChooser()
            fc.setTitle("Import")
            fc.setParent(this)
            fc.addFilter(BaseFileChooser.ExtensionFilter("VCCD Source Files", ".txt"))
            val files = fc.choose()
            if (files == null) {
                return
            }
            val entries = VCCD.parse(FileInputStream(files[0]))
            LOG.log(Level.INFO, "Entries: {0}", entries.size())
            val model = jTable1!!.getModel() as DefaultTableModel
            run {
                var i = model.getRowCount() - 1
                while (i >= 0) {
                    model.removeRow(i)
                    i--
                }
            }
            for (entrie in entries) {
                val hash = entrie.hash
                val token = entrie.key
                if (!hashmap.containsKey(hash)) {
                    hashmap.put(hash, StringPair(token, CHAN_UNKNOWN))
                }
                model.addRow(array<Any>(hexFormat(entrie.hash), entrie.key!!, entrie.value!!))
            }
            persistHashmap(hashmap)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun insertRow(evt: ActionEvent) {
        val model = jTable1!!.getModel() as DefaultTableModel
        model.addRow(array<Any>(0, "", ""))
    }

    private fun deleteRow(evt: ActionEvent) {
        val model = jTable1!!.getModel() as DefaultTableModel
        var newRow = Math.min(jTable1!!.getSelectedRow(), jTable1!!.getRowCount() - 1)
        if (jTable1!!.getSelectedRow() == (jTable1!!.getRowCount() - 1)) {
            newRow = jTable1!!.getRowCount() - 2
        }
        LOG.log(Level.FINER, "New row: {0}", newRow)
        model.removeRow(jTable1!!.getSelectedRow())
        if (jTable1!!.getRowCount() > 0) {
            jTable1!!.setRowSelectionInterval(newRow, newRow)
        }
    }

    private fun createNew(evt: ActionEvent) {
        val model = jTable1!!.getModel() as DefaultTableModel
        run {
            var i = model.getRowCount() - 1
            while (i >= 0) {
                model.removeRow(i)
                i--
            }
        }
        model.addRow(array<Any>(0, "", ""))
    }

    private fun formattingHelp(evt: ActionEvent) {
        val message = try {
            javaClass.getResource("/VCCDTest.txt").readText()
        } catch (ignored: IOException) {
            "Unable to load"
        }

        val jsp = JScrollPane(JTextArea(message))
        jsp.setPreferredSize(Dimension(500, 500))
        val dialog = JOptionPane(jsp, JOptionPane.INFORMATION_MESSAGE).createDialog(this, "Formatting")
        dialog.setResizable(true)
        dialog.setModal(false)
        dialog.setVisible(true)
    }

    private fun export(evt: ActionEvent) {
        val sb = StringBuilder(jTable1!!.getRowCount() * 100) // rough estimate
        val model = jTable1!!.getModel()
        for (i in model.getRowCount().indices) {
            sb.append(MessageFormat.format("{0}\t{1}\n", model.getValueAt(i, 0), model.getValueAt(i, 2)))
        }
        val pane = JTextArea(sb.toString())
        val s = Toolkit.getDefaultToolkit().getScreenSize()
        pane.setLineWrap(false)
        pane.setPreferredSize(Dimension(s.width / 3, s.height / 2))
        pane.setEditable(false)
        pane.setOpaque(false)
        pane.setBackground(Color(0, 0, 0, 0))
        val jsp = JScrollPane(pane)
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
        JOptionPane.showMessageDialog(this, jsp, "Hash List", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun showInputDialog(): Int {
        val inputValue = JOptionPane.showInputDialog("Enter row", jTable1!!.getSelectedRow() + 1)
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

    private fun gotoRow(evt: ActionEvent) {
        var row = showInputDialog()
        if (row < 0) {
            return
        }
        if (row > jTable1!!.getRowCount()) {
            row = jTable1!!.getRowCount()
        }
        jTable1!!.setRowSelectionInterval(row, row)
        jTable1!!.scrollRectToVisible(jTable1!!.getCellRect(row, 0, true))
    }

    private fun saveCaptionsAs(evt: ActionEvent) {
        save(true)
    }

    private fun generateHash(evt: ActionEvent) {
        generateHash()
    }

    private fun exportAll(evt: ActionEvent) {
        try {
            val fc = NativeFileChooser()
            fc.setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG)
            fc.setTitle("Export")
            fc.addFilter(BaseFileChooser.ExtensionFilter("XML", ".xml"))
            fc.setParent(this)
            val fs = fc.choose()
            if (fs == null) {
                return
            }
            saveFile = fs[0]
            prefs.exportSubtree(FileOutputStream(saveFile))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: BackingStoreException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private class StringPair(var name:
                             String?, var channel: String)

    class object {

        private val LOG = Logger.getLogger(javaClass<VCCDTest>().getName())
        private val prefs = Preferences.userRoot().node("timepath").node("hl2-caption-editor")
        private val CHAN_UNKNOWN = "CHAN_UNKNOWN"

        throws(javaClass<IOException>())
        public platformStatic fun main(args: Array<String>) {
            try {
                if (args.size() > 0) {
                    val `in` = VCCD.parse(FileInputStream(args[0]))
                    val hashmap = HashMap<Int, StringPair>()
                    for (i in `in`) {
                        // Learning
                        val crc = i.hash
                        val token = i.key
                        val hash = java.lang.Long.parseLong(crc.toString().toLowerCase(), 16)
                        hashmap.put(hash.toInt(), StringPair(token, CHAN_UNKNOWN))
                    }
                    persistHashmap(hashmap)
                    VCCD.save(`in`, FileOutputStream("closecaption_english.dat"))
                    return
                }
            } catch (ex: FileNotFoundException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            EventQueue.invokeLater {
                val c = VCCDTest()
                c.setLocationRelativeTo(null)
                c.setVisible(true)
            }
        }

        private fun persistHashmap(map: Map<Int, StringPair>) {
            for (entry in map.entrySet()) {
                val key = entry.getKey()
                val value = entry.getValue().name
                if ((key == null) || (value == null)) {
                    continue
                }
                prefs.node(entry.getValue().channel).putInt(value, key)
            }
        }

        private fun hexFormat(`in`: Int): String {
            var str = Integer.toHexString(`in`).toUpperCase()
            while (str.length() < 8) {
                str = '0' + str
            }
            return str
        }

        private fun getKeyEditor(): TableCellEditor {
            val t = JTextField()
            //        new StringAutoCompleter(t, trie, 2);
            return DefaultCellEditor(t)
        }
    }
}
