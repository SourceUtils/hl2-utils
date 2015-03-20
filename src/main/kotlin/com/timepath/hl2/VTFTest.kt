package com.timepath.hl2

import com.timepath.hl2.io.image.ImageFormat
import com.timepath.hl2.io.image.VTF

import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.filechooser.FileFilter
import java.awt.*
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic
import kotlin.concurrent.thread


SuppressWarnings("serial")
class ImagePreviewPanel(private val parentFrame: JFrame) : JPanel(), PropertyChangeListener {
    private val frame: JSpinner
    private val lod: JSpinner
    private val bgColor: Color
    private var image: Image? = null
    private var vtf: VTF? = null

    init {
        setPreferredSize(Dimension(ACCSIZE, -1))
        bgColor = Color.PINK
        lod = JSpinner()
        lod.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent) {
                try {
                    createImage(vtf)
                    repaint()
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, null, ex)
                }

            }
        })
        add(lod, BorderLayout.WEST)
        this.frame = JSpinner()
        this.frame.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent) {
                try {
                    createImage(vtf)
                    repaint()
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, null, ex)
                }

            }
        })
        add(this.frame, BorderLayout.EAST)
    }

    throws(javaClass<IOException>())
    private fun createImage(v: VTF?) {
        if (v != null) {
            val img = v.getImage(lod.getValue() as Int, frame.getValue() as Int)
            if (img != null) {
                parentFrame.setIconImage(v.getThumbImage())
                image = img
                return
            }
        }
        image = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
    }

    override fun paintComponent(g: Graphics) {
        g.setColor(bgColor)
        g.fillRect(0, 0, ACCSIZE, getHeight())
        if (image != null) {
            g.drawImage(image, (getWidth() / 2) - (image!!.getWidth(null) / 2), (getHeight() / 2) - (image!!.getHeight(null) / 2), this)
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        val propertyName = evt.getPropertyName()
        if (propertyName == JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) {
            try {
                load(evt.getNewValue() as File)
                repaint()
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }

        }
    }

    throws(javaClass<IOException>(), javaClass<FileNotFoundException>())
    private fun load(selection: File?) {
        if (selection == null) {
            return
        }
        vtf = VTF.load(FileInputStream(selection))
        if (vtf != null) {
            frame.setValue(vtf!!.frameFirst)
        }
        createImage(vtf)
    }

    companion object {

        private val ACCSIZE = 256
        private val LOG = Logger.getLogger(javaClass<ImagePreviewPanel>().getName())
    }
}

class VtfFileFilter(private val vtfFormat: ImageFormat) : FileFilter() {

    override fun accept(f: File): Boolean {
        if (f.isDirectory()) {
            return true
        }
        var v: VTF? = null
        try {
            v = VTF.load(FileInputStream(f))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        if (v == null) {
            return false
        }
        if (vtfFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
            return true
        }
        return v!!.format == vtfFormat
    }

    override fun getDescription(): String {
        return "VTF (${if ((vtfFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN)) "All" else vtfFormat.name()})"
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<VtfFileFilter>().getName())
    }
}

class AntiVtfFileFilter(private val name: String?, vararg val ignored: ImageFormat) : FileFilter() {

    override fun accept(f: File): Boolean {
        if (f.isDirectory()) {
            return true
        }
        var v: VTF? = null
        try {
            v = VTF.load(FileInputStream(f))
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        if (v == null) {
            return false
        }
        for (format in ignored) {
            if (v!!.format == format) {
                return false
            }
        }
        return true
    }

    override fun getDescription(): String = name ?: "VTF (Not ${Arrays.toString(ignored)})"

    companion object {

        private val LOG = Logger.getLogger(javaClass<AntiVtfFileFilter>().getName())
    }
}

object VTFTest {

    private val LOG = Logger.getLogger(javaClass<VTFTest>().getName())

    public platformStatic fun main(args: Array<String>) {
        thread {
            val frame = JFrame("VTF Loader")
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            val jsp = JScrollPane()
            jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
            jsp.getVerticalScrollBar().setUnitIncrement(64)
            frame.add(jsp)
            val pane = JPanel()
            pane.setLayout(BoxLayout(pane, BoxLayout.PAGE_AXIS))
            jsp.setViewportView(pane)
            val chooser = JFileChooser()
            val generic = VtfFileFilter(ImageFormat.IMAGE_FORMAT_UNKNOWN)
            chooser.addChoosableFileFilter(generic)
            chooser.addChoosableFileFilter(VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT1))
            chooser.addChoosableFileFilter(VtfFileFilter(ImageFormat.IMAGE_FORMAT_DXT5))
            chooser.addChoosableFileFilter(AntiVtfFileFilter(null, ImageFormat.IMAGE_FORMAT_DXT1, ImageFormat.IMAGE_FORMAT_DXT3, ImageFormat.IMAGE_FORMAT_DXT5))
            chooser.addChoosableFileFilter(AntiVtfFileFilter("RGB", ImageFormat.IMAGE_FORMAT_DXT1, ImageFormat.IMAGE_FORMAT_DXT3, ImageFormat.IMAGE_FORMAT_DXT5, ImageFormat.IMAGE_FORMAT_ABGR8888, ImageFormat.IMAGE_FORMAT_ARGB8888, ImageFormat.IMAGE_FORMAT_BGRA8888, ImageFormat.IMAGE_FORMAT_BGRA4444, ImageFormat.IMAGE_FORMAT_BGRA5551, ImageFormat.IMAGE_FORMAT_RGBA16161616, ImageFormat.IMAGE_FORMAT_RGBA16161616F, ImageFormat.IMAGE_FORMAT_RGBA32323232F, ImageFormat.IMAGE_FORMAT_RGBA8888))
            chooser.setFileFilter(generic)
            val preview = ImagePreviewPanel(frame)
            chooser.setAccessory(preview)
            chooser.addPropertyChangeListener(preview)
            chooser.setControlButtonsAreShown(false)
            pane.add(chooser)
            frame.setVisible(true)
            frame.pack()
            frame.setLocationRelativeTo(null)
        }
    }
}
