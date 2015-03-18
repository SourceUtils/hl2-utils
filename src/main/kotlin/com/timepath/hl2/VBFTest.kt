package com.timepath.hl2

import com.timepath.hl2.io.font.VBF
import com.timepath.hl2.io.image.VTF
import com.timepath.hl2.swing.VBFCanvas
import com.timepath.plaf.x.filechooser.BaseFileChooser
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.swing.ReorderableJTree

import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.*
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
SuppressWarnings("serial")
class VBFTest
/**
 * Creates new form VBFTest
 */
private() : JFrame() {
    private var canvas: VBFCanvas? = null
    private var currentGlyph: VBF.BitmapGlyph? = null
    private var data: VBF? = null
    private var heightSpinner: JSpinner? = null
    private var image: VTF? = null
    private var jPopupMenu1: JPopupMenu? = null
    private var jTree1: ReorderableJTree? = null
    private var jTree2: ReorderableJTree? = null
    private var toCopy: Char = ' '
    private var widthSpinner: JSpinner? = null
    private var xSpinner: JSpinner? = null
    private var ySpinner: JSpinner? = null

    {
        initComponents()
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                val choice = JOptionPane.showInternalConfirmDialog(getContentPane(), "Do you want to save?")
                if (choice == JOptionPane.NO_OPTION) {
                    dispose()
                }
            }
        })
        canvas!!.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                var which: JTree = jTree1!!
                if (jTree2!!.getSelectionRows() != null) {
                    which = jTree2!!
                }
                jTree1!!.setSelectionRow(-1)
                jTree2!!.setSelectionRow(-1)
                val seek = canvas!!.selected.firstOrNull()
                if (seek == null) {
                    return
                }
                for (i in which.getModel().getChildCount(which.getModel().getRoot()).indices) {
                    val node = which.getModel().getChild(which.getModel().getRoot(), i) as DefaultMutableTreeNode
                    if (node.getUserObject() == seek) {
                        which.setSelectionRow(node.getParent().getIndex(node) + 1)
                        break
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (currentGlyph == null) {
                    return
                }
                val r = currentGlyph!!.getBounds()
                xSpinner!!.setValue(r.x)
                ySpinner!!.setValue(r.y)
                widthSpinner!!.setValue(r.width)
                heightSpinner!!.setValue(r.height)
            }
        })
        jTree2!!.setModel(jTree1!!.getModel())
        jTree1!!.setMinDragLevel(2)
        jTree1!!.setMinDropLevel(1)
        jTree1!!.setMaxDropLevel(1)
        jTree1!!.setDropMode(DropMode.ON)
        jTree2!!.setMinDragLevel(2)
        jTree2!!.setMinDropLevel(1)
        jTree2!!.setMaxDropLevel(1)
        jTree2!!.setDropMode(DropMode.ON)
        val renderer = object : DefaultTreeCellRenderer() {

            override fun getTreeCellRendererComponent(tree: JTree, value: Any, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
                val tn = value as TreeNode
                return super.getTreeCellRendererComponent(tree, value, sel, expanded, (tn.getParent() != null) && tn.getParent() != tree.getModel().getRoot(), row, hasFocus)
            }

            public fun init(): TreeCellRenderer {
                setLeafIcon(null)
                return this
            }
        }.init()
        jTree1!!.setCellRenderer(renderer)
        jTree2!!.setCellRenderer(renderer)
        val spinners = false
        xSpinner!!.addChangeListener(object : ChangeListener {
            private var old: Int = 0

            override fun stateChanged(e: ChangeEvent) {
                if ((data == null) || (currentGlyph == null)) {
                    xSpinner!!.setValue(0)
                    return
                }
                val current = (xSpinner!!.getValue() as Number).toInt()
                currentGlyph!!.getBounds().x = current
                doRepaint(Math.min(old, current), (ySpinner!!.getValue() as Number).toInt(), Math.max(old, current) + (widthSpinner!!.getValue() as Number).toInt(), (heightSpinner!!.getValue() as Number).toInt())
                old = current
                val wide = if ((image != null)) image!!.width else data!!.getWidth().toInt()
                if (spinners) {
                    (widthSpinner!!.getModel() as SpinnerNumberModel).setMaximum(wide - (xSpinner!!.getValue() as Number).toInt())
                }
            }
        })
        widthSpinner!!.addChangeListener(object : ChangeListener {
            private var old: Int = 0

            override fun stateChanged(e: ChangeEvent) {
                if ((data == null) || (currentGlyph == null)) {
                    widthSpinner!!.setValue(0)
                    return
                }
                val current = (widthSpinner!!.getValue() as Number).toInt()
                currentGlyph!!.getBounds().width = current
                doRepaint((xSpinner!!.getValue() as Number).toInt(), (ySpinner!!.getValue() as Number).toInt(), Math.max(old, current), (heightSpinner!!.getValue() as Number).toInt())
                old = current
                val wide = if ((image != null)) image!!.width else data!!.getWidth().toInt()
                if (spinners) {
                    (xSpinner!!.getModel() as SpinnerNumberModel).setMaximum(wide - (widthSpinner!!.getValue() as Number).toInt())
                }
            }
        })
        ySpinner!!.addChangeListener(object : ChangeListener {
            private var old: Int = 0

            override fun stateChanged(e: ChangeEvent) {
                if ((data == null) || (currentGlyph == null)) {
                    ySpinner!!.setValue(0)
                    return
                }
                val current = (ySpinner!!.getValue() as Number).toInt()
                currentGlyph!!.getBounds().y = current
                doRepaint((xSpinner!!.getValue() as Number).toInt(), Math.min(old, current), (widthSpinner!!.getValue() as Number).toInt(), Math.max(old, current) + (heightSpinner!!.getValue() as Number).toInt())
                old = current
                val high = if ((image != null)) image!!.width else data!!.getHeight().toInt()
                if (spinners) {
                    (heightSpinner!!.getModel() as SpinnerNumberModel).setMaximum(high - (ySpinner!!.getValue() as Number).toInt())
                }
            }
        })
        heightSpinner!!.addChangeListener(object : ChangeListener {
            private var old: Int = 0

            override fun stateChanged(e: ChangeEvent) {
                if ((data == null) || (currentGlyph == null)) {
                    heightSpinner!!.setValue(0)
                    return
                }
                val current = (heightSpinner!!.getValue() as Number).toInt()
                currentGlyph!!.getBounds().height = current
                doRepaint((xSpinner!!.getValue() as Number).toInt(), (ySpinner!!.getValue() as Number).toInt(), (widthSpinner!!.getValue() as Number).toInt(), Math.max(old, current))
                old = current
                val high = if ((image != null)) image!!.width else data!!.getHeight().toInt()
                if (spinners) {
                    (ySpinner!!.getModel() as SpinnerNumberModel).setMaximum(high - (heightSpinner!!.getValue() as Number).toInt())
                }
            }
        })
    }

    private fun createGlyph(evt: ActionEvent) {
        val g = VBF.BitmapGlyph()
        if (data == null) {
            data = VBF()
            canvas!!.setVBF(data!!)
        }
        for (i in 256.indices) {
            if (!data!!.hasGlyph(i)) {
                g.setIndex(i.toByte())
                break
            }
            if (i == data!!.getGlyphs().size()) {
                g.setIndex((i + 1).toByte())
            }
        }
        data!!.getGlyphs().add(g)
        insertGlyph(jTree1!!.getModel() as DefaultTreeModel, g)
    }

    private fun doRepaint(x: Int, y: Int, w: Int, h: Int) {
        canvas!!.repaint()//x, y, h, h);
    }

    private fun initComponents() {
        jPopupMenu1 = JPopupMenu()
        val jMenuItem4 = JMenuItem()
        val jSplitPane1 = JSplitPane()
        val jPanel1 = JPanel()
        val jSplitPane2 = JSplitPane()
        val jScrollPane2 = JScrollPane()
        jTree1 = ReorderableJTree()
        val jScrollPane3 = JScrollPane()
        jTree2 = ReorderableJTree()
        val jPanel2 = JPanel()
        val jPanel3 = JPanel()
        xSpinner = JSpinner()
        ySpinner = JSpinner()
        val jPanel5 = JPanel()
        widthSpinner = JSpinner()
        heightSpinner = JSpinner()
        val jScrollPane1 = JScrollPane()
        canvas = VBFCanvas()
        val jMenuBar1 = JMenuBar()
        val jMenu1 = JMenu()
        val jMenuItem1 = JMenuItem()
        val jMenuItem2 = JMenuItem()
        val jMenu2 = JMenu()
        val jMenuItem3 = JMenuItem()
        jMenuItem4.setText("Copy")
        jMenuItem4.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                jMenuItem4ActionPerformed(e)
            }
        })
        jPopupMenu1!!.add(jMenuItem4)
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
        setTitle("Bitmap Font Editor")
        setPreferredSize(Dimension(640, 480))
        jSplitPane1.setDividerLocation(250)
        jSplitPane1.setContinuousLayout(true)
        jSplitPane1.setPreferredSize(Dimension(360, 403))
        jSplitPane2.setDividerSize(2)
        jSplitPane2.setResizeWeight(0.5)
        jSplitPane2.setEnabled(false)
        val treeNode1 = DefaultMutableTreeNode("Glyphs")
        jTree1!!.setModel(DefaultTreeModel(treeNode1))
        jTree1!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                jTree1MouseClicked(e)
            }
        })
        jTree1!!.addTreeSelectionListener(object : TreeSelectionListener {
            override fun valueChanged(e: TreeSelectionEvent) {
                treeInteraction(e)
            }
        })
        jScrollPane2.setViewportView(jTree1)
        jSplitPane2.setLeftComponent(jScrollPane2)
        jTree2!!.setModel(jTree1!!.getModel())
        jTree2!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                jTree2MouseClicked(e)
            }
        })
        jTree2!!.addTreeSelectionListener(object : TreeSelectionListener {
            override fun valueChanged(e: TreeSelectionEvent) {
                treeInteraction(e)
            }
        })
        jScrollPane3.setViewportView(jTree2)
        jSplitPane2.setRightComponent(jScrollPane3)
        jPanel2.setPreferredSize(Dimension(200, 195))
        jPanel3.setBorder(BorderFactory.createTitledBorder("Position"))
        jPanel3.setLayout(BoxLayout(jPanel3, BoxLayout.LINE_AXIS))
        xSpinner!!.setModel(SpinnerNumberModel(0, 0, null, 1))
        jPanel3.add(xSpinner)
        ySpinner!!.setModel(SpinnerNumberModel(0, 0, null, 1))
        jPanel3.add(ySpinner)
        jPanel5.setBorder(BorderFactory.createTitledBorder("Dimensions"))
        jPanel5.setLayout(BoxLayout(jPanel5, BoxLayout.LINE_AXIS))
        widthSpinner!!.setModel(SpinnerNumberModel(0, 0, null, 1))
        jPanel5.add(widthSpinner)
        heightSpinner!!.setModel(SpinnerNumberModel(0, 0, null, 1))
        jPanel5.add(heightSpinner)
        val jPanel2Layout = GroupLayout(jPanel2)
        jPanel2.setLayout(jPanel2Layout)
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jPanel5, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()).addComponent(jPanel3, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()))
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(jPanel2Layout.createSequentialGroup().addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addGap(0, 0, java.lang.Short.MAX_VALUE.toInt())))
        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1.setLayout(jPanel1Layout)
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, 249, java.lang.Short.MAX_VALUE.toInt()).addComponent(jSplitPane2, GroupLayout.DEFAULT_SIZE, 249, java.lang.Short.MAX_VALUE.toInt()))
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addComponent(jSplitPane2, GroupLayout.DEFAULT_SIZE, 301, java.lang.Short.MAX_VALUE.toInt()).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 148, GroupLayout.PREFERRED_SIZE)))
        jSplitPane1.setLeftComponent(jPanel1)
        val canvasLayout = GroupLayout(canvas)
        canvas!!.setLayout(canvasLayout)
        canvasLayout.setHorizontalGroup(canvasLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 373, java.lang.Short.MAX_VALUE.toInt()))
        canvasLayout.setVerticalGroup(canvasLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 449, java.lang.Short.MAX_VALUE.toInt()))
        jScrollPane1.setViewportView(canvas)
        jSplitPane1.setRightComponent(jScrollPane1)
        getContentPane().add(jSplitPane1, BorderLayout.CENTER)
        jMenu1.setText("File")
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
        jMenuItem1.setMnemonic('O')
        jMenuItem1.setText("Open")
        jMenuItem1.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                open(e)
            }
        })
        jMenu1.add(jMenuItem1)
        jMenuItem2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK))
        jMenuItem2.setMnemonic('S')
        jMenuItem2.setText("Save")
        jMenuItem2.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                save(e)
            }
        })
        jMenu1.add(jMenuItem2)
        jMenuBar1.add(jMenu1)
        jMenu2.setText("Edit")
        jMenuItem3.setText("Create glyph")
        jMenuItem3.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                createGlyph(e)
            }
        })
        jMenu2.add(jMenuItem3)
        jMenuBar1.add(jMenu2)
        setJMenuBar(jMenuBar1)
        pack()
    }

    private fun insertCharacters(model: DefaultTreeModel, child: MutableTreeNode, g: Int) {
        for (i in data!!.getTable().size.indices) {
            val glyphIndex = data!!.getTable()[i].toInt()
            if (glyphIndex != g) {
                continue
            }
            val sub = DefaultMutableTreeNode(DisplayableCharacter(i))
            model.insertNodeInto(sub, child, model.getChildCount(child))
        }
    }

    private fun insertGlyph(model: DefaultTreeModel, glyph: VBF.BitmapGlyph) {
        val child = DefaultMutableTreeNode(glyph)
        model.insertNodeInto(child, model.getRoot() as MutableTreeNode, model.getChildCount(model.getRoot()))
        insertCharacters(model, child, glyph.getIndex().toInt())
        model.reload()
    }

    private fun jMenuItem4ActionPerformed(evt: ActionEvent) {
        val selection = StringSelection(java.lang.String.valueOf(toCopy))
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection)
    }

    private fun jTree1MouseClicked(evt: MouseEvent) {
        mouseClicked(evt)
    }

    private fun jTree2MouseClicked(evt: MouseEvent) {
        mouseClicked(evt)
    }

    throws(javaClass<IOException>())
    private fun load(s: String) {
        LOG.log(Level.INFO, "Loading {0}", s)
        val p = canvas
        val vbf = File("$s.vbf")
        val vtf = File("$s.vtf")
        if (vbf.exists()) {
            data = VBF(FileInputStream(vbf))
            p!!.setVBF(data!!)
            val model = jTree1!!.getModel() as DefaultTreeModel
            val root = model.getRoot() as DefaultMutableTreeNode
            root.removeAllChildren()
            for (g in data!!.getGlyphs()) {
                insertGlyph(model, g)
            }
        }
        if (vtf.exists()) {
            image = VTF.load(FileInputStream(vtf))
            p!!.setVTF(image!!)
        }
        canvas!!.repaint()
    }

    private fun mouseClicked(evt: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(evt)) {
            val jTree = evt.getComponent() as JTree
            val clicked = jTree.getPathForLocation(evt.getX(), evt.getY())
            if (clicked == null) {
                return
            }
            if ((jTree.getSelectionPaths() == null) || !Arrays.asList<TreePath>(*jTree.getSelectionPaths()).contains(clicked)) {
                jTree.setSelectionPath(clicked)
            }
            for (p in jTree.getSelectionPaths()!!) {
                if (p.getLastPathComponent() !is DefaultMutableTreeNode) {
                    return
                }
                val userObject = (p.getLastPathComponent() as DefaultMutableTreeNode).getUserObject()
                if (userObject is DisplayableCharacter) {
                    toCopy = userObject.c
                    jPopupMenu1!!.show(jTree, evt.getX(), evt.getY())
                }
            }
        }
    }

    private fun open(evt: ActionEvent) {
        try {
            val fs = NativeFileChooser().setParent(this).setTitle("Select vbf").addFilter(BaseFileChooser.ExtensionFilter("Valve Bitmap Font", ".vbf")).addFilter(BaseFileChooser.ExtensionFilter("Valve Texture File", ".vtf")).choose()
            if (fs == null) {
                return
            }
            val file = fs[0]
            load(file.getPath().replace(".vbf", "").replace(".vtf", ""))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun save(evt: ActionEvent) {
        try {
            val fs = NativeFileChooser().setParent(this).setTitle("Select save location").addFilter(BaseFileChooser.ExtensionFilter("Valve Bitmap Font", ".vbf")).setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG).choose()
            if (fs == null) {
                return
            }
            val file = fs[0]
            val model = jTree1!!.getModel()
            val root = model.getRoot() as TreeNode
            for (i in root.getChildCount().indices) {
                val node = root.getChildAt(i) as DefaultMutableTreeNode
                val g = node.getUserObject() as VBF.BitmapGlyph
                for (x in node.getChildCount().indices) {
                    val character = node.getChildAt(x) as DefaultMutableTreeNode
                    val obj = character.getUserObject()
                    if (obj is DisplayableCharacter) {
                        data!!.getTable().set(obj.c.toInt(), g.getIndex())
                    } else if (obj is DefaultMutableTreeNode) {
                        // XXX: hack
                        data!!.getTable().set((obj.getUserObject() as DisplayableCharacter).c.toInt(), g.getIndex())
                    }
                }
            }
            data!!.save(FileOutputStream(file))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun treeInteraction(evt: TreeSelectionEvent) {
        val selection = evt.getNewLeadSelectionPath()
        if (selection == null) {
            return
        }
        when (evt.getSource()) {
            jTree1 -> jTree2
            jTree2 -> jTree1
            else -> null
        }?.setSelectionRow(-1)
        val node = selection.getLastPathComponent()
        if (node !is DefaultMutableTreeNode) {
            return
        }
        val obj = node.getUserObject()
        if (obj !is VBF.BitmapGlyph) {
            return
        }
        currentGlyph = obj
        canvas!!.select(currentGlyph)
        if (currentGlyph!!.getBounds() == null) {
            currentGlyph!!.setBounds(Rectangle())
        }
        val r = currentGlyph!!.getBounds()
        xSpinner!!.setValue(r.x)
        ySpinner!!.setValue(r.y)
        widthSpinner!!.setValue(r.width)
        heightSpinner!!.setValue(r.height)
    }

    private class DisplayableCharacter(i: Int) {

        public val c: Char

        {
            c = i.toChar()
        }

        override fun toString(): String {
            val block = Character.UnicodeBlock.of(c)
            val unprintable = Character.isISOControl(c) || (c == KeyEvent.CHAR_UNDEFINED) || (block == null) || block == Character.UnicodeBlock.SPECIALS
            if (unprintable) {
                return "0x${if ((c <= 15)) "0" else ""}${Integer.toHexString(c.toInt()).toUpperCase()}"
            }
            return Character.toString(c)
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VBFTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater {
                VBFTest().setVisible(true)
            }
        }
    }
}
