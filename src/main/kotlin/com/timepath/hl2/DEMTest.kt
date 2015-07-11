package com.timepath.hl2

import com.timepath.Logger
import com.timepath.hex.HexEditor
import com.timepath.hl2.io.demo.HL2DEM
import com.timepath.hl2.io.demo.Message
import com.timepath.hl2.io.demo.MessageType
import com.timepath.hl2.io.demo.Packet
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.SteamUtils
import com.timepath.with
import org.jdesktop.swingx.JXFrame
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTree
import org.jdesktop.swingx.decorator.AbstractHighlighter
import org.jdesktop.swingx.decorator.ComponentAdapter
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.awt.*
import java.awt.event.ActionListener
import java.beans.PropertyVetoException
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.event.TreeSelectionListener
import javax.swing.table.AbstractTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.platform.platformStatic


public class DEMTest() : JPanel() {
    protected val menu: JMenuBar
    protected val hex: HexEditor
    protected val tabs: JTabbedPane
    protected val table: JXTable
    protected val tree: JXTree
    protected val tableModel: MessageModel

    init {
        tableModel = MessageModel()
        table = JXTable() with {
            setAutoCreateRowSorter(true)
            setColumnControlVisible(true)
            setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED)
            setModel(tableModel)
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        }
        tree = JXTree() with {
            setModel(DefaultTreeModel(DefaultMutableTreeNode("root")))
            setRootVisible(false)
            setShowsRootHandles(true)
        }
        tabs = JTabbedPane() with {
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
            addTab("Hierarchy", JScrollPane(tree))
        }
        hex = HexEditor()
        table.addHighlighter(object : AbstractHighlighter() {
            override fun doHighlight(component: Component, adapter: ComponentAdapter): Component {
                if (adapter.row >= 0 && tableModel.messages.size() > 0 && adapter.row < tableModel.messages.size()) {
                    val msg = tableModel.messages[table.convertRowIndexToModel(adapter.row)]
                    component.setBackground(when {
                        adapter.isSelected() -> component.getBackground()
                        else -> when {
                            msg.incomplete -> Color.ORANGE
                            else -> when (msg.type) {
                                MessageType.Signon, MessageType.Packet -> Color.CYAN
                                MessageType.UserCmd -> Color.GREEN
                                MessageType.ConsoleCmd -> Color.PINK
                                else -> Color.WHITE
                            }
                        }
                    })
                }
                return component
            }
        })
        table.getSelectionModel().addListSelectionListener(ListSelectionListener {
            val row = table.getSelectedRow()
            if (row == -1) return@ListSelectionListener
            val frame = tableModel.messages[table.convertRowIndexToModel(row)]
            hex.setData(frame.data)
            val root = DefaultMutableTreeNode(frame)
            recurse(frame.meta, root)
            tree.setModel(DefaultTreeModel(DefaultMutableTreeNode() with { add(root) }))
            run {
                var i = -1
                while (++i < tree.getRowCount()) {
                    // Expand all
                    val node = tree.getPathForRow(i).getLastPathComponent() as DefaultMutableTreeNode
                    if (node.getLevel() < 3) tree.expandRow(i)
                }
            }
        })
        tree.getSelectionModel().addTreeSelectionListener(TreeSelectionListener {
            val selectionPath = tree.getSelectionPath() ?: return@TreeSelectionListener
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
                } catch (e: PropertyVetoException) {
                    e.printStackTrace()
                }
            }
        })
        menu = JMenuBar() with {
            add(JMenu("File") with {
                setMnemonic('F')
                add(JMenuItem("Open") with {
                    addActionListener(ActionListener { open() })
                })
                add(JMenuItem("Dump commands") with {
                    addActionListener(ActionListener { showCommands() })
                })
            })
        }
        setLayout(BorderLayout())
        add(JSplitPane() with {
            setResizeWeight(1.0)
            setContinuousLayout(true)
            setOneTouchExpandable(true)
            setLeftComponent(JScrollPane(table))
            setRightComponent(JSplitPane() with {
                setOrientation(JSplitPane.VERTICAL_SPLIT)
                setResizeWeight(1.0)
                setContinuousLayout(true)
                setOneTouchExpandable(true)
                setTopComponent(tabs)
                setRightComponent(hex)
            })
        })
    }

    protected fun recurse(iter: List<*>, parent: DefaultMutableTreeNode) {
        for (e in iter) {
            if (e !is Pair<*, *>) continue
            val v = e.second
            when (v) {
                is List<*> -> recurse(v, DefaultMutableTreeNode(e.first) with { parent.add(this) })
                else -> parent.add(DefaultMutableTreeNode(e))
            }
        }
    }

    protected fun open() {
        try {
            val fs = NativeFileChooser()
                    .setTitle("Open DEM")
                    .setDirectory(File(SteamUtils.getSteamApps(), "common/Team Fortress 2/tf/."))
                    .addFilter(BaseFileChooser.ExtensionFilter("Demo files", "dem"))
                    .choose() ?: return
            object : SwingWorker<HL2DEM, Message>() {
                val listEvt = DefaultListModel<Pair<*, *>>()
                val listMsg = DefaultListModel<Pair<*, *>>()
                var incomplete = 0

                override fun doInBackground(): HL2DEM {
                    tableModel.messages.clear()
                    val demo = HL2DEM.load(fs[0])
                    val frames = demo.frames // TODO: Stream
                    publish(*frames.toTypedArray())
                    return demo
                }

                override fun process(chunks: List<Message>) {
                    for (msg in chunks) {
                        if (msg.incomplete) incomplete++
                        tableModel.messages.add(msg)
                        when (msg.type) { MessageType.Packet, MessageType.Signon ->
                            for ((k, v) in msg.meta) {
                                if (k !is Packet) continue
                                if (v !is List<*>) continue
                                for (e in v) {
                                    if (e !is Pair<*, *>) continue
                                    when (k.type) {
                                        Packet.svc_GameEvent -> listEvt
                                        Packet.svc_UserMessage -> listMsg
                                        else -> null
                                    }?.addElement(e)
                                }
                            }
                        }
                    }
                    tableModel.fireTableDataChanged() // FIXME
                }

                override fun done() {
                    val demo = try {
                        get()
                    } catch (ignored: InterruptedException) {
                        return
                    } catch (e: ExecutionException) {
                        LOG.log(Level.SEVERE, { null }, e)
                        return
                    }

                    LOG.info({ "Total incomplete messages: ${incomplete} / ${demo.frames.size()}" })
                    while (tabs.getTabCount() > 1) tabs.remove(1) // Remove previous events and messages
                    tabs.add("Events", JScrollPane(JList(listEvt)) with {
                        getVerticalScrollBar().setUnitIncrement(16)
                    })
                    tabs.add("Messages", JScrollPane(JList(listMsg)) with {
                        getVerticalScrollBar().setUnitIncrement(16)
                    })
                    table.setModel(tableModel)
                }
            }.execute()
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, { null }, e)
        }
    }

    protected fun showCommands() {
        val sb = StringBuilder()
        for (m in tableModel.messages) {
            if (m.type != MessageType.ConsoleCmd) continue
            for (p in m.meta) {
                sb.append('\n').append(p.singletonOrEmptyList())
            }
        }
        JOptionPane.showMessageDialog(this,
                JScrollPane(JTextArea(if (sb.length() > 0) sb.substring(1) else "")) with {
                    setPreferredSize(Dimension(500, 500))
                }
        )
    }

    protected inner class MessageModel : AbstractTableModel() {

        val messages: MutableList<Message> = ArrayList()

        override fun getRowCount() = messages.size()

        protected val columns: Array<String> = arrayOf("Tick", "Type", "Size")
        protected val types: Array<Class<*>> = arrayOf(javaClass<Int>(), javaClass<Enum<*>>(), javaClass<Int>())

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
                2 -> return m.data?.capacity()
            }
            return null
        }
    }

    companion object {

        private val LOG = Logger()

        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater {
                JXFrame("netdecode") with {
                    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                    val demTest = DEMTest()
                    setContentPane(demTest)
                    setJMenuBar(demTest.menu)
                    pack()
                    setLocationRelativeTo(null)
                    setVisible(true)
                }
            }
        }
    }
}
