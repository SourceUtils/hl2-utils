package com.timepath.hl2

import com.timepath.hex.HexEditor
import com.timepath.hl2.io.demo.HL2DEM
import com.timepath.hl2.io.demo.Message
import com.timepath.hl2.io.demo.MessageType
import com.timepath.hl2.io.demo.Packet
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.SteamUtils
import org.jdesktop.swingx.JXFrame
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTree
import org.jdesktop.swingx.decorator.AbstractHighlighter
import org.jdesktop.swingx.decorator.ComponentAdapter
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyVetoException
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.*
import javax.swing.event.*
import javax.swing.table.AbstractTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.platform.platformStatic


/**
 * @author TimePath
 */
SuppressWarnings("serial")
public class DEMTest protected() : JPanel() {
    public val menu: JMenuBar
    protected var hex: HexEditor
    protected var tabs: JTabbedPane
    protected var table: JXTable
    protected var tree: JXTree
    protected var tableModel: MessageModel

    init {
        setLayout(BorderLayout())
        add(object : JSplitPane() {
            init {
                setResizeWeight(1.0)
                setContinuousLayout(true)
                setOneTouchExpandable(true)
                table = object : JXTable() {
                    init {
                        setAutoCreateRowSorter(true)
                        setColumnControlVisible(true)
                        setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED)
                        tableModel = MessageModel()
                        setModel(tableModel)
                        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                    }
                }
                setLeftComponent(JScrollPane(table))
                setRightComponent(object : JSplitPane() {
                    init {
                        setOrientation(JSplitPane.VERTICAL_SPLIT)
                        setResizeWeight(1.0)
                        setContinuousLayout(true)
                        setOneTouchExpandable(true)
                        tabs = object : JTabbedPane() {
                            init {
                                setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
                                tree = object : JXTree() {
                                    init {
                                        setModel(DefaultTreeModel(DefaultMutableTreeNode("root")))
                                        setRootVisible(false)
                                        setShowsRootHandles(true)
                                    }
                                }
                                addTab("Hierarchy", JScrollPane(tree))
                            }
                        }
                        setTopComponent(tabs)
                        hex = HexEditor()
                        setRightComponent(hex)
                    }
                })
            }
        })
        table.addHighlighter(object : AbstractHighlighter() {
            override fun doHighlight(component: Component, adapter: ComponentAdapter): Component {
                if (adapter.row >= 0 && tableModel.messages.size() > 0 && adapter.row < tableModel.messages.size()) {
                    val f = tableModel.messages[this@DEMTest.table.convertRowIndexToModel(adapter.row)]
                    val c: Color
                    if (f.incomplete) {
                        c = Color.ORANGE
                    } else {
                        when (f.type) {
                            MessageType.Signon, MessageType.Packet -> c = Color.CYAN
                            MessageType.UserCmd -> c = Color.GREEN
                            MessageType.ConsoleCmd -> c = Color.PINK
                            else -> c = Color.WHITE
                        }
                    }
                    component.setBackground(if (adapter.isSelected()) component.getBackground() else c)
                }
                return component
            }
        })
        table.getSelectionModel().addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent) {
                val row = table.getSelectedRow()
                if (row == -1) return
                val frame = tableModel.messages[table.convertRowIndexToModel(row)]
                hex.setData(frame.data)
                val root = DefaultMutableTreeNode(frame)
                recurse(frame.meta, root)
                val container = DefaultMutableTreeNode()
                container.add(root)
                val tm = DefaultTreeModel(container)
                tree.setModel(tm)
                run {
                    var i = -1
                    while (++i < tree.getRowCount()) {
                        // Expand all
                        val node = tree.getPathForRow(i).getLastPathComponent() as DefaultMutableTreeNode
                        if (node.getLevel() < 3) tree.expandRow(i)
                    }
                }
            }
        })
        tree.getSelectionModel().addTreeSelectionListener(object : TreeSelectionListener {
            override fun valueChanged(e: TreeSelectionEvent) {
                val selectionPath = tree.getSelectionPath()
                if (selectionPath == null) return
                val lastPathComponent = selectionPath.getLastPathComponent()
                val o = (lastPathComponent as DefaultMutableTreeNode).getUserObject()
                if (o is Packet) {
                    try {
                        val offsetBytes = o.offset / 8
                        val offsetBits = o.offset % 8
                        hex.seek((offsetBytes - (offsetBytes % 16)).toLong()) // Start of row
                        hex.caretLocation = (offsetBytes.toLong())
                        hex.bitShift = (offsetBits)
                        hex.update()
                    } catch (e1: PropertyVetoException) {
                        e1.printStackTrace()
                    }

                }
            }
        })
        menu = object : JMenuBar() {
            init {
                add(object : JMenu("File") {
                    init {
                        setMnemonic('F')
                        add(object : JMenuItem("Open") {
                            init {
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(e: ActionEvent) {
                                        open()
                                    }
                                })
                            }
                        })
                        add(object : JMenuItem("Dump commands") {
                            init {
                                addActionListener(object : ActionListener {
                                    override fun actionPerformed(e: ActionEvent) {
                                        showCommands()
                                    }
                                })
                            }
                        })
                    }
                })
            }
        }
    }

    protected fun recurse(i: Iterable<*>, root: DefaultMutableTreeNode): Unit = i.forEach { entry ->
        when (entry) {
            is Pair<*, *> -> expand(entry, entry.first, entry.second, root)
            is Map.Entry<*, *> -> expand(entry, entry.getKey(), entry.getValue(), root)
            else -> root.add(DefaultMutableTreeNode(entry))
        }
    }

    protected fun expand(entry: Any, k: Any?, v: Any?, root: DefaultMutableTreeNode) {
        if (v is Iterable<*>) {
            val n = DefaultMutableTreeNode(k)
            root.add(n)
            recurse(v, n)
        } else {
            root.add(DefaultMutableTreeNode(entry))
        }
    }

    protected fun open() {
        try {
            val fs = NativeFileChooser().setTitle("Open DEM").setDirectory(File(SteamUtils.getSteamApps(), "common/Team Fortress 2/tf/.")).addFilter(BaseFileChooser.ExtensionFilter("Demo files", "dem")).choose()
            if (fs == null) return
             object : SwingWorker<HL2DEM, Message>() {
                var listEvt = DefaultListModel<Pair<*, *>>()
                var listMsg = DefaultListModel<Pair<*, *>>()
                var incomplete = 0

                throws(javaClass<Exception>())
                override fun doInBackground(): HL2DEM {
                    tableModel.messages.clear()
                    val demo = HL2DEM.load(fs[0])
                    val frames = demo.frames // TODO: Stream
                    publish(*frames.copyToArray())
                    return demo
                }

                override fun process(chunks: List<Message>?) {
                    for (m in chunks!!) {
                        if (m.incomplete) incomplete++
                        tableModel.messages.add(m)
                        when (m.type) {
                            MessageType.Packet, MessageType.Signon -> for (ents in m.meta) {
                                if (ents.first !is Packet) break
                                if (ents.second !is Iterable<*>) break
                                for (o in ents.second as Iterable<*>) {
                                    if (o !is Pair<*, *>) break
                                    when ((ents.first as Packet).type) {
                                        Packet.Type.svc_GameEvent -> listEvt.addElement(o)
                                        Packet.Type.svc_UserMessage -> listMsg.addElement(o)
                                    }
                                }
                            }
                        }
                    }
                    tableModel.fireTableDataChanged() // FIXME
                }

                override fun done() {
                    val demo: HL2DEM
                    try {
                        demo = get()
                    } catch (ignored: InterruptedException) {
                        return
                    } catch (e: ExecutionException) {
                        LOG.log(Level.SEVERE, null, e)
                        return
                    }

                    LOG.info("Total incomplete messages: ${incomplete} / ${demo.frames.size()}")
                    while (tabs.getTabCount() > 1) tabs.remove(1) // Remove previous events and messages
                    val jsp = JScrollPane(JList(listEvt))
                    jsp.getVerticalScrollBar().setUnitIncrement(16)
                    tabs.add("Events", jsp)
                    val jsp2 = JScrollPane(JList(listMsg))
                    jsp2.getVerticalScrollBar().setUnitIncrement(16)
                    tabs.add("Messages", jsp2)
                    table.setModel(tableModel)
                }
            }.execute()
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, null, e)
        }

    }

    protected fun showCommands() {
        val sb = StringBuilder()
        for (m in tableModel.messages) {
            if (m.type != MessageType.ConsoleCmd) continue
            for (p in m.meta) {
                sb.append('\n').append(p.second)
            }
        }
        val jsp = JScrollPane(JTextArea(if (sb.length() > 0) sb.substring(1) else ""))
        jsp.setPreferredSize(Dimension(500, 500))
        JOptionPane.showMessageDialog(this, jsp)
    }

    protected inner class MessageModel : AbstractTableModel() {

        var messages: ArrayList<Message> = ArrayList()

        override fun getRowCount() = messages.size()

        protected var columns: Array<String> = array<String>("Tick", "Type", "Size")
        protected var types: Array<Class<*>> = array(javaClass<Int>(), javaClass<Enum<*>>(), javaClass<Int>())

        override fun getColumnCount() = columns.size()

        override fun getColumnName(columnIndex: Int) = columns[columnIndex]

        override fun getColumnClass(columnIndex: Int) = types[columnIndex]

        override fun isCellEditable(rowIndex: Int, columnIndex: Int) = false

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            if (messages.isEmpty()) return null
            val m = messages[rowIndex]
            when (columnIndex) {
                0 -> return m.tick
                1 -> return m.type
                2 -> return if ((m.data == null)) null else m.data!!.capacity()
            }
            return null
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) = Unit

        override fun addTableModelListener(l: TableModelListener) = Unit

        override fun removeTableModelListener(l: TableModelListener) = Unit
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<DEMTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater {
                val f = JXFrame("netdecode")
                f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                val demTest = DEMTest()
                f.add(demTest)
                f.setJMenuBar(demTest.menu)
                f.pack()
                f.setLocationRelativeTo(null)
                f.setVisible(true)
            }
        }
    }
}
