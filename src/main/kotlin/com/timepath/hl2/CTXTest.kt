package com.timepath.hl2

import com.timepath.hl2.io.CTX
import com.timepath.plaf.x.filechooser.NativeFileChooser
import org.jdesktop.swingx.JXTextArea
import org.jdesktop.swingx.JXTextField
import java.awt.EventQueue
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.*
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
SuppressWarnings("serial")
public class CTXTest protected constructor() : JFrame() {
    protected var input: JXTextField
    protected var output: JXTextArea

    init {
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        input = JXTextField(CTX.TF2)
        input.setToolTipText("")
        output = JXTextArea()
        output.setColumns(20)
        output.setRows(5)
        output.setTabSize(4)
        output.setEditable(false)
        val jMenuBar1 = JMenuBar()
        val jMenu1 = JMenu("File")
        val jMenuItem1 = JMenuItem("Open")
        jMenuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
        jMenuItem1.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                open()
            }
        })
        jMenu1.add(jMenuItem1)
        jMenuBar1.add(jMenu1)
        this.setJMenuBar(jMenuBar1)
        val layout = GroupLayout(getContentPane())
        getContentPane().setLayout(layout)
        val jScrollPane2 = JScrollPane(output)
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(input).addComponent(jScrollPane2, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 477, java.lang.Short.MAX_VALUE.toInt()))
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(input, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 325, java.lang.Short.MAX_VALUE.toInt())))
        this.pack()
        this.setLocationRelativeTo(null)
    }

    protected fun open() {
        try {
            val f = NativeFileChooser().setTitle("Select CTX").setMultiSelectionEnabled(false).choose()
            if (f == null) return
            val br = BufferedReader(InputStreamReader(CTX.decrypt(input.getText().toByteArray(), FileInputStream(f[0]))))
            output.setText("")
            br.forEachLine {
                output.append("$it\n")
            }
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, null, e)
        }

    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<CTXTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            EventQueue.invokeLater {
                CTXTest().setVisible(true)
            }
        }
    }
}
