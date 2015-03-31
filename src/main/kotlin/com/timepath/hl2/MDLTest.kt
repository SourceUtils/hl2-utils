package com.timepath.hl2

import com.jme3.app.SimpleApplication
import com.jme3.asset.*
import com.jme3.asset.plugins.FileLocator
import com.jme3.input.ChaseCamera
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import com.jme3.scene.debug.Arrow
import com.jme3.scene.debug.Grid
import com.jme3.scene.shape.Box
import com.jme3.system.AppSettings
import com.jme3.system.JmeCanvasContext
import com.jme3.texture.Image
import com.jme3.util.BufferUtils
import com.timepath.hl2.io.bsp.BSP
import com.timepath.hl2.io.image.VTF
import com.timepath.hl2.io.studiomodel.StudioModel
import com.timepath.plaf.x.filechooser.NativeFileChooser
import com.timepath.steam.io.storage.ACF
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.VFile
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.text.MessageFormat
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.*
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class MDLTest protected() : SimpleApplication() {
    protected val executor: ExecutorService = ScheduledThreadPoolExecutor(4)
    protected var FRAME_TITLE: String = "HLMV"
    protected var frame: JFrame by Delegates.notNull()
    protected var modelNode: Node = Node("Model node")

    override fun simpleInitApp() {
        LOG_JME.setLevel(Level.INFO)
        registerLoaders()
        setDisplayStatView(false)
        setDisplayFps(false)
        viewPort.setBackgroundColor(ColorRGBA.DarkGray)
        initInput()
        attachGrid(Vector3f.ZERO, 100, ColorRGBA.LightGray)
        attachCoordinateAxes(Vector3f.ZERO, 10f, 4)
        rootNode.attachChild(modelNode)
        loadModel("tf/models/player/heavy.mdl")
        loadMap("tf/maps/ctf_2fort.bsp")
    }

    protected fun attachCoordinateAxes(pos: Vector3f, length: Float, width: Int) {
        for ((vec, color) in mapOf(
                Vector3f.UNIT_X to ColorRGBA.Red,
                Vector3f.UNIT_Y to ColorRGBA.Green,
                Vector3f.UNIT_Z to ColorRGBA.Blue)) {
            putShape(Arrow(vec.mult(length)).let {
                it.setLineWidth(width.toFloat())
                it
            }, color).setLocalTranslation(pos)
        }
    }

    protected fun putShape(shape: Mesh, color: ColorRGBA): Geometry {
        val g = Geometry("coordinate axis", shape)
        g.setMaterial(Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md").let {
            it.getAdditionalRenderState().setWireframe(true)
            it.setColor("Color", color)
            it
        })
        rootNode.attachChild(g)
        return g
    }

    protected fun attachGrid(pos: Vector3f, size: Int, color: ColorRGBA) {
        val g = Geometry("wireframe grid", Grid(size, size, 1f))
        g.setMaterial(Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md").let {
            it.getAdditionalRenderState().setWireframe(true)
            it.setColor("Color", color)
            it
        })
        g.center().move(pos)
        rootNode.attachChild(g)
    }

    protected fun initInput() {
        with(flyCam) {
            setDragToRotate(true)
            setEnabled(false)
        }
        with(ChaseCamera(cam, rootNode, inputManager)) {
            setSmoothMotion(false)
            setRotationSpeed(3f)
            setInvertHorizontalAxis(false)
            setInvertVerticalAxis(true)
            setMinVerticalRotation(-FastMath.HALF_PI + FastMath.ZERO_TOLERANCE)
            setDefaultVerticalRotation(FastMath.HALF_PI / 2)
            setMaxVerticalRotation(FastMath.HALF_PI)
            setDefaultHorizontalRotation(FastMath.HALF_PI - (FastMath.HALF_PI / 2))
            setDefaultDistance(100f)
            setMaxDistance(300f)
            setMaxDistance(30000f)
            setZoomSensitivity(250f)
        }
        with(cam) {
            setFrustumFar(30000f)
        }
    }

    protected fun loadMap(name: String) {
        val application = this
        executor.submit(Callable {
            try {
                val mdl = assetManager.loadModel(name)
                application.enqueue {
                    modelNode.attachChild(mdl)
                    frame.setTitle("$FRAME_TITLE - ${mdl.getUserData<Any>("source")}")
                }.get()
            } catch (ex: Exception) {
                LOG.log(Level.SEVERE, null, ex)
            }
        })
    }

    protected fun registerLoaders() {
        assetManager.registerLocator("/", javaClass<ACFLocator>())
        assetManager.registerLocator("/", javaClass<FileLocator>())
        assetManager.registerLoader(javaClass<BSPLoader>(), "bsp")
        assetManager.registerLoader(javaClass<MDLLoader>(), "mdl")
        assetManager.registerLoader(javaClass<VTFLoader>(), "vtf")
    }

    override fun startCanvas(waitFor: Boolean) {
        super.startCanvas(waitFor)
        val app = this
        val canvas = (context as JmeCanvasContext).getCanvas()
        canvas.setSize(settings.getWidth(), settings.getHeight())
        frame = JFrame(FRAME_TITLE)
        val mb = JMenuBar()
        frame.setJMenuBar(mb)
        val fileMenu = JMenu("File")
        mb.add(fileMenu)
        val clearItem = JMenuItem("Detach all")
        clearItem.addActionListener {
            app.modelNode.detachAllChildren()
        }
        fileMenu.add(clearItem)
        val openName = JMenuItem("Open from game")
        openName.addActionListener {
            app.loadModel(JOptionPane.showInputDialog(frame, "Enter model name"))
        }
        fileMenu.add(openName)
        val openFile = JMenuItem("Open from file")
        openFile.addActionListener {
            try {
                val f = NativeFileChooser().setParent(frame).setTitle("Select model").choose()
                if (f != null) {
                    app.loadModel(f[0].getPath())
                }
            } catch (ex: IOException) {
                Logger.getLogger(javaClass<MDLTest>().getName()).log(Level.SEVERE, null, ex)
            }
        }
        fileMenu.add(openFile)
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                app.stop(true)
                frame.dispose()
            }
        })
        frame.getContentPane().add(canvas)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.setVisible(true)
    }

    override fun destroy() {
        super.destroy()
        executor.shutdown()
    }

    protected fun loadModel(name: String) {
        val box = createBox(10f)
        modelNode.attachChild(box)
        val application = this
        executor.submit(Callable {
            try {
                val mdl = assetManager.loadModel(name)
                application.enqueue {
                    modelNode.detachChild(box)
                    modelNode.attachChild(mdl)
                    frame.setTitle("$FRAME_TITLE - ${mdl.getUserData<Any>("source")}")
                }.get()
            } catch (ex: InterruptedException) {
                LOG.log(Level.SEVERE, null, ex)
            } catch (ex: Exception) {
                LOG.log(Level.SEVERE, null, ex)
                JOptionPane.showMessageDialog(frame, "Nope", "Nope", JOptionPane.ERROR_MESSAGE)
            }
        })
    }

    protected fun createBox(s: Float): Geometry {
        val box = Geometry("Box", Box(0.5f * s, 0.5f * s, 0.5f * s))
        val mat = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        mat.setColor("Color", ColorRGBA.randomColor())
        val tex = assetManager.loadTexture("platform/materials/vgui/vtfnotloaded.vtf")
        mat.setTexture("ColorMap", tex)
        box.setMaterial(mat)
        return box
    }

    companion object {
        private val LOG_JME = Logger.getLogger("com.jme3")

        public platformStatic fun main(args: Array<String>) {
            LOG_JME.setLevel(Level.WARNING)
            val app = MDLTest()
            app.setPauseOnLostFocus(false)
            app.setShowSettings(true)
            val settings = AppSettings(true)
            settings.setRenderer(AppSettings.LWJGL_OPENGL_ANY)
            settings.setAudioRenderer(null)
            app.setSettings(settings)
            app.createCanvas()
            app.startCanvas(true)
        }
    }
}

private val LOG = Logger.getLogger(javaClass<MDLTest>().getName())

public class ACFLocator : AssetLocator {

    private val source: SimpleVFile?
    private val appID = 440 // TODO: Make configurable
    private var rootPath: String? = null

    init {
        var loading: ACF? = null
        try {
            loading = ACF.fromManifest(appID)
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        source = loading
    }

    override fun setRootPath(path: String) {
        rootPath = path
    }

    override fun locate(manager: AssetManager, key: AssetKey<*>): AssetInfo {
        if (source == null) {
            throw AssetLoadException(MessageFormat.format("Steam game {0} not installed, run steam://install/{0}", appID))
        }
        val search = "$rootPath${VFile.SEPARATOR}${key.getName()}"
        val found = source.query(search)
        if (found == null) {
            throw AssetNotFoundException(MessageFormat.format("{0} not found", search))
        }
        return SourceModelAssetInfo(manager, key, found)
    }

    private class SourceModelAssetInfo(manager: AssetManager,
                                       key: AssetKey<*>,
                                       private val source: SimpleVFile) : AssetInfo(manager, key) {
        override fun openStream() = source.openStream()!!
    }
}

public class BSPLoader : AssetLoader {

    throws(javaClass<IOException>())
    override fun load(info: AssetInfo): Any {
        val am = info.getManager()
        val name = info.getKey().getName()
        LOG.log(Level.INFO, "Loading {0}...", name)
        val m = BSP.load(info.openStream())!!
        LOG.log(Level.INFO, "Creating mesh...")
        val mesh = Mesh()
        mesh.setMode(Mesh.Mode.Lines)
        mesh.setPointSize(2f)
        val posBuf = m.vertices
        if (posBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf)
        }
        val idxBuf = m.indices
        if (idxBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Index, 2, idxBuf)
        }
        mesh.setStatic()
        mesh.updateBound()
        mesh.updateCounts()
        val geom = Geometry("$name-geom", mesh)
        geom.rotateUpTo(Vector3f.UNIT_Z.negate())
        val skin = Material(info.getManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        skin.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front)
        skin.setTexture("ColorMap", am.loadTexture("hl2/materials/debug/debugempty.vtf"))
        geom.setMaterial(skin)
        geom.setUserData("source", info.getKey().getName())
        LOG.log(Level.INFO, "{0} loaded", name)
        return geom
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<BSPLoader>().getName())
    }
}

public class MDLLoader : AssetLoader {

    SuppressWarnings("rawtypes")
    throws(javaClass<IOException>())
    override fun load(info: AssetInfo): Any {
        val am = info.getManager()
        val name = info.getKey().getName()
        LOG.log(Level.INFO, "Loading {0}...\n", name)
        val basename = name.substringBeforeLast('.')
        val mdlStream = info.openStream()
        val vvdStream = am.locateAsset(AssetKey<InputStream>("$basename.vvd")).openStream()
        val vtxStream = am.locateAsset(AssetKey<InputStream>("$basename.dx90.vtx")).openStream()
        val m = StudioModel(mdlStream, vvdStream, vtxStream)
        val mesh = Mesh()
        val posBuf = m.vertices
        if (posBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf)
        }
        val normBuf = m.normals
        if (normBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf)
        }
        val texBuf = m.textureCoordinates
        if (texBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf)
        }
        val tanBuf = m.tangents
        if (tanBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tanBuf)
        }
        val idxBuf = m.indices
        if (idxBuf != null) {
            mesh.setBuffer(VertexBuffer.Type.Index, 3, idxBuf)
        }
        mesh.setStatic()
        mesh.updateBound()
        mesh.updateCounts()
        val geom = Geometry("$basename-geom", mesh)
        val skin = Material(info.getManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        skin.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front)
        try {
            skin.setTexture("ColorMap", am.loadTexture("$basename.vtf"))
        } catch (anfe: AssetNotFoundException) {
            skin.setTexture("ColorMap", am.loadTexture("hl2/materials/debug/debugempty.vtf"))
        }

        geom.setMaterial(skin)
        geom.setUserData("source", basename)
        return geom
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<MDLLoader>().getName())
    }
}

public class VTFLoader : AssetLoader {

    throws(javaClass<IOException>())
    override fun load(info: AssetInfo): Any {
        val name = info.getKey().getName()
        LOG.log(Level.INFO, "Loading {0}...\n", name)
        val v = VTF.load(info.openStream())
        val bimg = v!!.getImage(0) as BufferedImage
        val buf = BufferUtils.createByteBuffer(bimg.getWidth() * bimg.getHeight() * 4)
        run {
            var y = bimg.getHeight() - 1
            while (y >= 0) {
                for (x in bimg.getWidth().indices) {
                    val pixel = bimg.getRGB(x, y)
                    buf.put(((pixel shr 16) and 0xFF).toByte()) // Red
                    buf.put(((pixel shr 8) and 0xFF).toByte()) // Green
                    buf.put(((pixel shr 0) and 0xFF).toByte()) // Blue
                    buf.put(((pixel shr 24) and 0xFF).toByte()) // Alpha
                }
                y--
            }
        }
        buf.flip()
        return Image().let {
            it.setFormat(Image.Format.RGBA8)
            it.setWidth(bimg.getWidth())
            it.setHeight(bimg.getHeight())
            it.setData(buf)
            it
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<VTFLoader>().getName())
    }
}

