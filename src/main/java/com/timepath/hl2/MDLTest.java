package com.timepath.hl2;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.*;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.bsp.BSP;
import com.timepath.hl2.io.image.VTF;
import com.timepath.hl2.io.studiomodel.StudioModel;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

class MDLTest extends SimpleApplication {

    private static final Logger          LOG         = Logger.getLogger(MDLTest.class.getName());
    private static final Logger          LOG_JME     = Logger.getLogger("com.jme3");
    private final        ExecutorService executor    = new ScheduledThreadPoolExecutor(4);
    private              String          FRAME_TITLE = "HLMV";
    private JFrame frame;
    private Node modelNode = new Node("Model node");

    MDLTest() {}

    public static void main(String... args) {
        LOG_JME.setLevel(Level.WARNING);
        SimpleApplication app = new MDLTest();
        app.setPauseOnLostFocus(false);
        app.setShowSettings(true);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL_ANY);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.createCanvas();
        app.startCanvas(true);
    }

    @Override
    public void simpleInitApp() {
        LOG_JME.setLevel(Level.INFO);
        registerLoaders();
        setDisplayStatView(false);
        setDisplayFps(false);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        initInput();
        attachGrid(Vector3f.ZERO, 100, ColorRGBA.LightGray);
        attachCoordinateAxes(Vector3f.ZERO, 10, 4);
        rootNode.attachChild(modelNode);
        //        loadModel("tf/models/player/heavy.mdl");
        loadMap("tf/maps/ctf_2fort.bsp");
    }

    void attachCoordinateAxes(Vector3f pos, float length, int width) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X.mult(length));
        arrow.setLineWidth(width);
        putShape(arrow, ColorRGBA.Red).setLocalTranslation(pos);
        arrow = new Arrow(Vector3f.UNIT_Y.mult(length));
        arrow.setLineWidth(width);
        putShape(arrow, ColorRGBA.Green).setLocalTranslation(pos);
        arrow = new Arrow(Vector3f.UNIT_Z.mult(length));
        arrow.setLineWidth(width);
        putShape(arrow, ColorRGBA.Blue).setLocalTranslation(pos);
    }

    Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        rootNode.attachChild(g);
        return g;
    }

    void attachGrid(Vector3f pos, int size, ColorRGBA color) {
        Geometry g = new Geometry("wireframe grid", new Grid(size, size, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        g.center().move(pos);
        rootNode.attachChild(g);
    }

    void initInput() {
        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);
        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setSmoothMotion(false);
        chaseCam.setRotationSpeed(3);
        chaseCam.setInvertHorizontalAxis(false);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI + FastMath.ZERO_TOLERANCE);
        chaseCam.setDefaultVerticalRotation(FastMath.HALF_PI / 2);
        chaseCam.setMaxVerticalRotation(FastMath.HALF_PI);
        chaseCam.setDefaultHorizontalRotation(FastMath.HALF_PI - ( FastMath.HALF_PI / 2 ));
        chaseCam.setDefaultDistance(100);
        chaseCam.setMaxDistance(300);
        chaseCam.setMaxDistance(30000);
        cam.setFrustumFar(30000);
        chaseCam.setZoomSensitivity(250);
    }

    void loadMap(final String name) {
        final Application application = this;
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    final Spatial mdl = assetManager.loadModel(name);
                    return application.enqueue(new Callable<Void>() {
                        @Override
                        public Void call() {
                            modelNode.attachChild(mdl);
                            return null;
                        }
                    }).get();
                } catch(Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
                return null;
            }
        });
    }

    void registerLoaders() {
        assetManager.registerLocator("/", ACFLocator.class);
        assetManager.registerLocator("/", FileLocator.class);
        assetManager.registerLoader(BSPLoader.class, "bsp");
        assetManager.registerLoader(MDLLoader.class, "mdl");
        assetManager.registerLoader(VTFLoader.class, "vtf");
    }

    @Override
    public void startCanvas(boolean waitFor) {
        super.startCanvas(waitFor);
        final MDLTest app = this;
        Canvas canvas = ( (JmeCanvasContext) context ).getCanvas();
        canvas.setSize(settings.getWidth(), settings.getHeight());
        frame = new JFrame(FRAME_TITLE);
        JMenuBar mb = new JMenuBar();
        frame.setJMenuBar(mb);
        JMenu fileMenu = new JMenu("File");
        mb.add(fileMenu);
        JMenuItem openName = new JMenuItem("Open from game");
        openName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                app.loadModel(JOptionPane.showInputDialog(frame, "Enter model name"));
            }
        });
        fileMenu.add(openName);
        JMenuItem openFile = new JMenuItem("Open from file");
        openFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    File[] f = new NativeFileChooser().setParent(frame).setTitle("Select model").choose();
                    if(f == null) {
                        return;
                    }
                    app.loadModel(f[0].getPath());
                } catch(IOException ex) {
                    Logger.getLogger(MDLTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        fileMenu.add(openFile);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.stop(true);
                frame.dispose();
            }
        });
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void destroy() {
        super.destroy();
        executor.shutdown();
    }

    void loadModel(final String name) {
        Geometry box = createBox(10);
        modelNode.detachAllChildren();
        modelNode.attachChild(box);
        final Application application = this;
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    final Spatial mdl = assetManager.loadModel(name);
                    return application.enqueue(new Callable<Void>() {
                        @Override
                        public Void call() {
                            modelNode.detachAllChildren();
                            modelNode.attachChild(mdl);
                            frame.setTitle(FRAME_TITLE + " - tf/models/" + mdl.getUserData("source"));
                            return null;
                        }
                    }).get();
                } catch(InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } catch(Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(frame, "Nope", "Nope", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
        });
    }

    Geometry createBox(float s) {
        Geometry box = new Geometry("Box", new Box(0.5f * s, 0.5f * s, 0.5f * s));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        Texture tex = assetManager.loadTexture("platform/materials/vgui/vtfnotloaded.vtf");
        mat.setTexture("ColorMap", tex);
        box.setMaterial(mat);
        return box;
    }

    public static class ACFLocator implements AssetLocator {

        private final ACF a;
        private final int appID = 440;
        private String rootPath;

        public ACFLocator() {
            ACF loading = null;
            try {
                loading = ACF.fromManifest(appID);
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            a = loading;
        }

        @Override
        public void setRootPath(String path) {
            rootPath = path;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public AssetInfo locate(AssetManager manager, AssetKey key) {
            if(a == null) {
                throw new AssetLoadException(MessageFormat.format("Steam game {0} not installed, run steam://install/{0}",
                                                                  appID));
            }
            String search = rootPath + VFile.SEPARATOR + key.getName();
            SimpleVFile found = a.query(search);
            if(found == null) {
                throw new AssetNotFoundException(MessageFormat.format("{0} not found", search));
            }
            return new SourceModelAssetInfo(manager, key, found);
        }

        private static class SourceModelAssetInfo extends AssetInfo {

            private final SimpleVFile source;

            @SuppressWarnings("rawtypes")
            SourceModelAssetInfo(AssetManager manager, AssetKey key, SimpleVFile source) {
                super(manager, key);
                this.source = source;
            }

            @Override
            public InputStream openStream() {
                return source.openStream();
            }
        }
    }

    public static class BSPLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(BSPLoader.class.getName());

        public BSPLoader() {}

        @Override
        public Object load(AssetInfo info) throws IOException {
            AssetManager am = info.getManager();
            String name = info.getKey().getName();
            LOG.log(Level.INFO, "Loading {0}...", name);
            BSP m = BSP.load(info.openStream());
            LOG.log(Level.INFO, "Creating mesh...");
            Mesh mesh = new Mesh();
            mesh.setMode(Mesh.Mode.Lines);
            mesh.setPointSize(2);
            FloatBuffer posBuf = m.getVertices();
            if(posBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
            }
            IntBuffer idxBuf = m.getIndices();
            if(idxBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Index, 2, idxBuf);
            }
            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();
            Geometry geom = new Geometry(name + "-geom", mesh);
            geom.rotateUpTo(Vector3f.UNIT_Z.negate());
            Material skin = new Material(info.getManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            skin.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
            skin.setTexture("ColorMap", am.loadTexture("hl2/materials/debug/debugempty.vtf"));
            geom.setMaterial(skin);
            LOG.log(Level.INFO, "{0} loaded", name);
            return geom;
        }
    }

    public static class MDLLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(MDLLoader.class.getName());

        public MDLLoader() {}

        @SuppressWarnings("rawtypes")
        @Override
        public Object load(AssetInfo info) throws IOException {
            AssetManager am = info.getManager();
            String name = info.getKey().getName();
            LOG.log(Level.INFO, "Loading {0}...\n", name);
            name = name.substring(0, name.lastIndexOf('.'));
            InputStream mdlStream = info.openStream();
            InputStream vvdStream = am.locateAsset(new AssetKey(name + ".vvd")).openStream();
            InputStream vtxStream = am.locateAsset(new AssetKey(name + ".dx90.vtx")).openStream();
            StudioModel m = new StudioModel(mdlStream, vvdStream, vtxStream);
            Mesh mesh = new Mesh();
            FloatBuffer posBuf = m.getVertices();
            if(posBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
            }
            FloatBuffer normBuf = m.getNormals();
            if(normBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
            }
            FloatBuffer texBuf = m.getTextureCoordinates();
            if(texBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
            }
            FloatBuffer tanBuf = m.getTangents();
            if(tanBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tanBuf);
            }
            IntBuffer idxBuf = m.getIndices();
            if(idxBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Index, 3, idxBuf);
            }
            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();
            Geometry geom = new Geometry(name + "-geom", mesh);
            Material skin = new Material(info.getManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            skin.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
            try {
                skin.setTexture("ColorMap", am.loadTexture(name + ".vtf"));
            } catch(AssetNotFoundException anfe) {
                skin.setTexture("ColorMap", am.loadTexture("hl2/materials/debug/debugempty.vtf"));
            }
            geom.setMaterial(skin);
            geom.setUserData("source", m.mdl.header.name);
            return geom;
        }
    }

    public static class VTFLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(VTFLoader.class.getName());

        public VTFLoader() {}

        @Override
        public Object load(AssetInfo info) throws IOException {
            String name = info.getKey().getName();
            LOG.log(Level.INFO, "Loading {0}...\n", name);
            VTF v = VTF.load(info.openStream());
            BufferedImage bimg = (BufferedImage) v.getImage(0);
            ByteBuffer buf = BufferUtils.createByteBuffer(bimg.getWidth() * bimg.getHeight() * 4);
            for(int y = bimg.getHeight() - 1; y >= 0; y--) {
                for(int x = 0; x < bimg.getWidth(); x++) {
                    int pixel = bimg.getRGB(x, y);
                    buf.put((byte) ( ( pixel >> 16 ) & 0xFF )); // Red
                    buf.put((byte) ( ( pixel >> 8 ) & 0xFF )); // Green
                    buf.put((byte) ( pixel & 0xFF )); // Blue
                    buf.put((byte) ( ( pixel >> 24 ) & 0xFF )); // Alpha
                }
            }
            buf.flip();
            Image img = new Image();
            img.setFormat(Image.Format.RGBA8);
            img.setWidth(bimg.getWidth());
            img.setHeight(bimg.getHeight());
            img.setData(buf);
            return img;
        }
    }
}
