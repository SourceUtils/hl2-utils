package com.timepath.hl2

import com.timepath.steam.io.VDF

import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
class VDFDiffTest protected() : JFrame() {
    protected var text1: JTextArea
    protected var text2: JTextArea

    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        setLayout(BorderLayout())
        add(object : JSplitPane() {
            {
                setResizeWeight(.5)
                text1 = object : JTextArea() {
                    {
                        setTabSize(4)
                        setText("\"A\" {\n" + "\t\"Modified\" {\n" + "\t\t\"Same\"\t\"yes\"\n" + "\t\t\"Similar\"\t\"one\"\n" + "\t\t\"Removed\"\t\"yes\"\n" + "\t}\n" + "\t\"Removed\" {}\n" + "\t\"Same\" {}\n" + "}\n")
                    }
                }
                setLeftComponent(JScrollPane(text1))
                text2 = object : JTextArea() {
                    {
                        setTabSize(4)
                        setText("\"B\" {\n" + "\t\"Modified\" {\n" + "\t\t\"Same\"\t\"yes\"\n" + "\t\t\"Similar\"\t\"two\"\n" + "\t\t\"Added\"\t\"yes\"\n" + "\t}\n" + "\t\"New\" {}\n" + "\t\"Same\" {}\n" + "}\n")
                    }
                }
                setRightComponent(JScrollPane(text2))
            }
        })
        add(object : JButton("Diff") {
            {
                addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {
                        try {
                            val n1 = VDF.load(ByteArrayInputStream(text1.getText().getBytes(StandardCharsets.UTF_8)))
                            val n2 = VDF.load(ByteArrayInputStream(text2.getText().getBytes(StandardCharsets.UTF_8)))
                            n1.getNodes().get(0).rdiff2(n2.getNodes().get(0))
                        } catch (ex: IOException) {
                            LOG.log(Level.SEVERE, null, ex)
                        }

                    }
                })
            }
        }, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(null)
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VDFDiffTest>().getName())

        public platformStatic fun main(args: Array<String>) {
            SwingUtilities.invokeLater(object : Runnable {
                override fun run() {
                    VDFDiffTest().setVisible(true)
                }
            })
        }
    }
}
