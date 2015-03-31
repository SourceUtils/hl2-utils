package com.timepath.hl2

import com.timepath.hl2.cvar.CVar
import com.timepath.hl2.cvar.CVarList
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.swing.StatusBar
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import kotlin.platform.platformStatic


/**
 * @author TimePath
 */
SuppressWarnings("serial")
class CVarTest
/**
 * Creates new form CVarTest
 */
private() : JFrame() {
    private val sorter: TableRowSorter<TableModel>
    private var caseSensitiveCheckBox: JCheckBox? = null
    private var jLabel1: JLabel? = null
    private var jLabel5: JLabel? = null
    private var jTable1: JTable? = null
    private var jTextField1: JTextField? = null
    private var notCheckBox: JCheckBox? = null
    private var regexCheckBox: JCheckBox? = null

    init {
        initComponents()
        sorter = TableRowSorter(jTable1!!.getModel())
        val comparator = object : Comparator<String> {
            override fun compare(o1: String, o2: String): Int {
                return o1.replaceFirst("\\+", "").replaceFirst("-", "").toLowerCase().compareTo(o2.replaceFirst("\\+", "").replaceFirst("-", "").toLowerCase())
            }
        }
        sorter.setComparator(0, comparator)
        val sortKeys = LinkedList<RowSorter.SortKey>()
        sortKeys.add(RowSorter.SortKey(0, SortOrder.ASCENDING))
        sorter.setSortKeys(sortKeys)
        jTable1!!.setRowSorter(sorter)
        jTextField1!!.getDocument().addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                filter()
            }

            override fun removeUpdate(e: DocumentEvent) {
                filter()
            }

            override fun changedUpdate(e: DocumentEvent) {
                filter()
            }
        })
    }

    private fun filter() {
        jLabel1!!.setText(Integer.toString(sorter.getModelRowCount()))
        try {
            var str = jTextField1!!.getText()
            if (str.isEmpty()) {
                sorter.setRowFilter(null)
            } else {
                if (!regexCheckBox!!.isSelected()) {
                    str = Pattern.quote(str)
                }
                if (!caseSensitiveCheckBox!!.isSelected()) {
                    str = "(?i)$str"
                }
                var rf = RowFilter.regexFilter<TableModel, Any>(str, 0, 1, 2, 3, 4, 5, 6)
                if (notCheckBox!!.isSelected()) {
                    rf = RowFilter.notFilter<TableModel, Any>(rf)
                }
                sorter.setRowFilter(rf)
            }
            jLabel5!!.setText(Integer.toString(sorter.getViewRowCount()))
            jTextField1!!.setForeground(Color.BLACK)
        } catch (e: PatternSyntaxException) {
            jTextField1!!.setForeground(Color.RED)
        }

    }

    private fun initComponents() {
        val jScrollPane1 = JScrollPane()
        jTable1 = JTable()
        val statusBar1 = StatusBar()
        val jLabel2 = JLabel()
        jLabel1 = JLabel()
        val jSeparator1 = JToolBar.Separator()
        val jLabel4 = JLabel()
        jLabel5 = JLabel()
        val jPanel1 = JPanel()
        jTextField1 = JTextField()
        val jLabel3 = JLabel()
        regexCheckBox = JCheckBox()
        caseSensitiveCheckBox = JCheckBox()
        notCheckBox = JCheckBox()
        val jMenuBar1 = JMenuBar()
        val jMenu1 = JMenu()
        val jMenuItem1 = JMenuItem()
        val jMenuItem4 = JMenuItem()
        val jMenuItem6 = JMenuItem()
        val jMenuItem5 = JMenuItem()
        val jMenuItem7 = JMenuItem()
        val jMenu2 = JMenu()
        val jMenuItem2 = JMenuItem()
        val jMenuItem3 = JMenuItem()
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        setTitle("CVar listing")
        jTable1!!.setModel(DefaultTableModel(array<Array<Any>>(), array<String>("Name", "Value", "Default", "Min", "Max", "Tags", "Description")))
        jScrollPane1.setViewportView(jTable1)
        statusBar1.setRollover(true)
        jLabel2.setText(" Total convars/concommands: ")
        statusBar1.add(jLabel2)
        jLabel1!!.setText("0")
        statusBar1.add(jLabel1!!)
        statusBar1.add(jSeparator1)
        jLabel4.setText("Showing: ")
        statusBar1.add(jLabel4)
        jLabel5!!.setText("0")
        statusBar1.add(jLabel5!!)
        jLabel3.setText("Find:")
        regexCheckBox!!.setMnemonic('R')
        regexCheckBox!!.setText("Regular Expression")
        regexCheckBox!!.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                regexCheckBoxActionPerformed()
            }
        })
        caseSensitiveCheckBox!!.setMnemonic('M')
        caseSensitiveCheckBox!!.setText("Match Case")
        caseSensitiveCheckBox!!.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                caseSensitiveCheckBoxActionPerformed()
            }
        })
        notCheckBox!!.setMnemonic('M')
        notCheckBox!!.setText("Not")
        notCheckBox!!.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                notCheckBoxActionPerformed()
            }
        })
        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1.setLayout(jPanel1Layout)
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField1!!)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(notCheckBox!!)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(caseSensitiveCheckBox!!)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(regexCheckBox!!)))
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField1!!, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3)
                                .addComponent(regexCheckBox!!)
                                .addComponent(caseSensitiveCheckBox!!)
                                .addComponent(notCheckBox!!))
                        .addGap(0, 0, 0)))
        jMenu1.setText("File")
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
        jMenuItem1.setText("Open")
        jMenuItem1.addActionListener {
            jMenuItem1ActionPerformed()
        }
        jMenu1.add(jMenuItem1)
        jMenuItem4.setText("Get cvarlist")
        jMenuItem4.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem4ActionPerformed()
            }
        })
        jMenu1.add(jMenuItem4)
        jMenuItem6.setText("Get differences")
        jMenuItem6.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem6ActionPerformed()
            }
        })
        jMenu1.add(jMenuItem6)
        jMenuItem5.setText("Reset cvars")
        jMenuItem5.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem5ActionPerformed()
            }
        })
        jMenu1.add(jMenuItem5)
        jMenuItem7.setText("Clear")
        jMenuItem7.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem7ActionPerformed()
            }
        })
        jMenu1.add(jMenuItem7)
        jMenuBar1.add(jMenu1)
        jMenu2.setText("Edit")
        jMenuItem2.setText("Copy names")
        jMenuItem2.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem2ActionPerformed()
            }
        })
        jMenu2.add(jMenuItem2)
        jMenuItem3.setText("Copy markdown")
        jMenuItem3.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem3ActionPerformed()
            }
        })
        jMenu2.add(jMenuItem3)
        jMenuBar1.add(jMenu2)
        setJMenuBar(jMenuBar1)
        val layout = GroupLayout(getContentPane())
        getContentPane().setLayout(layout)
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(statusBar1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()).addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 908, java.lang.Short.MAX_VALUE.toInt()).addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()))
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addGap(0, 0, 0).addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 201, java.lang.Short.MAX_VALUE.toInt()).addGap(0, 0, 0).addComponent(statusBar1, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
        pack()
    }

    private fun analyze(scanner: Scanner): Map<String, CVar> {
        return CVarList.analyzeList(scanner, HashMap<String, CVar>())
    }

    private fun jMenuItem1ActionPerformed() {
        try {
            val f = NativeFileChooser().setTitle("Select cvarlist").choose()
            if (f != null) {
                val worker = object : SwingWorker<Void, Array<Any?>>() {
                    throws(javaClass<Exception>())
                    override fun doInBackground(): Void? {
                        val rf = RandomAccessFile(f[0].getPath(), "r")
                        val scanner = Scanner(rf.getChannel())
                        val map = analyze(scanner)
                        for (entry in map.entrySet()) {
                            val v = entry.getValue()
                            publish(array(v.name, v.value, v.defaultValue, v.minimum, v.maximum, Arrays.toString(v.tags.copyToArray()), v.desc))
                        }
                        return null
                    }

                    override fun process(chunks: List<Array<Any?>>?) {
                        for (row in chunks!!) {
                            (jTable1!!.getModel() as DefaultTableModel).addRow(row)
                        }
                        filter()
                    }
                }
                worker.execute()
            }
        } catch (ex: IOException) {
            Logger.getLogger(javaClass<CVarTest>().getName()).log(Level.SEVERE, null, ex)
        }

    }

    private fun regexCheckBoxActionPerformed() {
        filter()
    }

    private fun caseSensitiveCheckBoxActionPerformed() {
        filter()
    }

    private fun jMenuItem2ActionPerformed() {
        val sb = StringBuilder()
        for (i in jTable1!!.getModel().getRowCount().indices) {
            val row = jTable1!!.convertRowIndexToModel(i)
            sb.append(jTable1!!.getModel().getValueAt(row, 0)).append('\n')
        }
        val selection = StringSelection(sb.toString())
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
    }

    private fun jMenuItem3ActionPerformed() {
        val m = jTable1!!.getModel()
        val sb = StringBuilder()
        val tab = "|"
        val col: Int
        val rows = m.getRowCount()
        val cols = m.getColumnCount()
        for (i in cols.indices) {
            col = jTable1!!.convertColumnIndexToModel(i)
            sb.append(tab).append(m.getColumnName(col))
        }
        val line = "\n"
        sb.append(tab).append(line)
        for (i in cols.indices) {
            sb.append(tab).append("--")
        }
        sb.append(tab).append(line)
        for (i in rows.indices) {
            val row = jTable1!!.convertRowIndexToModel(i)
            for (j in cols.indices) {
                col = jTable1!!.convertColumnIndexToModel(j)
                var obj: Any? = m.getValueAt(row, col)
                if (col == 0) {
                    obj = "[$obj](/r/tf2scripthelp/wiki/$obj#todo \"TODO\")"
                }
                sb.append(tab)
                if (obj != null) {
                    if (obj is Array<Any>) {
                        val arr = obj as Array<Any>
                        sb.append(arr[0])
                        for (k in 1..arr.size() - 1) {
                            sb.append(", ").append(arr[k])
                        }
                    } else {
                        sb.append(obj)
                    }
                }
            }
            sb.append(tab).append(line)
        }
        val selection = StringSelection(sb.toString())
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
    }

    private fun jMenuItem4ActionPerformed() {
        clear()
        var ret: String? = null
        val limit = 5
        run {
            var i = 0
            while (true) {
                val temp = ExternalConsole.exec("cvarlist; echo --end of cvarlist--", "--end of cvarlist--")
                if (temp == ret) {
                    break
                }
                if (i == limit) {
                    LOG.warning("Aborting inconsistency fixer")
                    break
                }
                ret = temp
                i++
            }
        }
        val selection = StringSelection(ret)
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
        val map = analyze(Scanner(ret!!))
        for (entry in map.entrySet()) {
            val v = entry.getValue()
            val chunks = array<Any?>(v.name, v.value, v.defaultValue, v.minimum, v.maximum, Arrays.toString(v.tags.copyToArray()), v.desc)
            (jTable1!!.getModel() as DefaultTableModel).addRow(chunks)
        }
        filter()
    }

    private fun notCheckBoxActionPerformed() {
        filter()
    }

    private fun jMenuItem5ActionPerformed() {
        val sb = StringBuilder()
        sb.append("sv_cheats 1\n")
        ExternalConsole.exec("sv_cheats 1", null)
        val m = jTable1!!.getModel()
        val rows = m.getRowCount()
        for (i in rows.indices) {
            val row = jTable1!!.convertRowIndexToModel(i)
            val name = m.getValueAt(row, jTable1!!.convertColumnIndexToModel(0))
            if ("sv_cheats" == name.toString()) {
                continue
            }
            val `val` = m.getValueAt(row, jTable1!!.convertColumnIndexToModel(2))
            if (`val` != null) {
                val strVal = "\"$`val`"
                sb.append(name).append(' ').append(strVal).append('\n')
                ExternalConsole.exec("${name.toString()} $strVal", null)
            }
        }
        sb.append("sv_cheats 0\n")
        ExternalConsole.exec("sv_cheats 0", null)
        val selection = StringSelection(sb.toString())
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
    }

    private fun jMenuItem6ActionPerformed() {
        clear()
        val ret = ExternalConsole.exec("differences; echo --end of differences--", "--end of differences--")
        val map = analyze(Scanner(ret))
        for (entry in map.entrySet()) {
            val `var` = entry.getValue()
            val chunks = array(`var`.name, `var`.value, `var`.defaultValue, `var`.minimum, `var`.maximum, Arrays.toString(`var`.tags.copyToArray()), `var`.desc)
            (jTable1!!.getModel() as DefaultTableModel).addRow(chunks)
        }
        filter()
    }

    private fun jMenuItem7ActionPerformed() {
        clear()
    }

    private fun clear() {
        val dm = jTable1!!.getModel() as DefaultTableModel
        val rowCount = dm.getRowCount()
        for (i in rowCount.indices) {
            dm.removeRow(0)
        }
        filter()
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<CVarTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                CVarTest().setVisible(true)
            }
        }
    }
}
